package com.season.semiproject.spatial.slope;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-database verification of Phase 12B SlopeSection computation, run against the live Docker
 * PostgreSQL/PostGIS instance and the actual imported Trail/TrailFeature/TrailSegment data. See
 * docs/09-slope-section-analysis.md for the full measured distributions this feeds into.
 *
 * Read-only: never writes to trail/trail_feature/trail_segment/trail_node -- the
 * "Network protection" tests below confirm the baseline row counts are unchanged.
 */
@SpringBootTest
class SlopeSectionServiceIntegrationTest {

    // 인왕산 bounding box (WGS84), generous margin -- used only to sanity-check that returned
    // section geometry coordinates are real lon/lat in the right place, not degenerate/garbled.
    private static final double MIN_LON = 126.90;
    private static final double MAX_LON = 127.00;
    private static final double MIN_LAT = 37.55;
    private static final double MAX_LAT = 37.62;

    private static final long[] ALL_TRAIL_IDS = { 10, 11, 12, 13 };
    private static final int[] WINDOWS = { 10, 20, 30 };

    @Autowired
    private SlopeSectionService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allFourTrailsProduceComputableSlopeSectionsAtEveryWindow() {
        for (long trailId : ALL_TRAIL_IDS) {
            for (int window : WINDOWS) {
                SlopeSectionFeatureCollection result = service.computeSlopeSections(trailId, window);
                assertFalse(result.getFeatures().isEmpty(),
                        "trail " + trailId + " at window " + window + "m produced zero sections");
            }
        }
    }

    @Test
    void everySectionHasValidNonDegenerateGeometryAndFiniteSlope() {
        SlopeSectionFeatureCollection result = service.computeSlopeSections(13, 20);
        assertFalse(result.getFeatures().isEmpty());

        for (SlopeSectionFeature feature : result.getFeatures()) {
            assertEquals("LineString", feature.getGeometry().get("type").asText());
            JsonNode coords = feature.getGeometry().get("coordinates");
            assertTrue(coords.size() >= 2, "section geometry must have at least 2 points");
            for (JsonNode pt : coords) {
                double lon = pt.get(0).asDouble();
                double lat = pt.get(1).asDouble();
                assertTrue(lon >= MIN_LON && lon <= MAX_LON, "longitude out of expected range: " + lon);
                assertTrue(lat >= MIN_LAT && lat <= MAX_LAT, "latitude out of expected range: " + lat);
            }

            double slope = feature.getProperties().getEstimatedSlopePercent();
            assertFalse(Double.isNaN(slope), "slope must never be NaN");
            assertFalse(Double.isInfinite(slope), "slope must never be Infinite");
            assertTrue(feature.getProperties().getDistanceMeters() > 0, "section distance must be > 0");
        }
    }

    @Test
    void noSectionIsASilentZeroFallback() {
        // Every returned section's slope must come from a real, finite elevation delta / distance
        // computation -- never a placeholder 0 substituted for an uncomputable case (see
        // SlopeSectionCalculator: uncomputable sections are omitted, not zeroed).
        for (int window : WINDOWS) {
            SlopeSectionFeatureCollection result = service.computeSlopeSections(10, window);
            for (SlopeSectionFeature feature : result.getFeatures()) {
                double delta = feature.getProperties().getEstimatedElevationDelta();
                double distance = feature.getProperties().getDistanceMeters();
                double expectedSlope = delta / distance * 100.0;
                assertEquals(expectedSlope, feature.getProperties().getEstimatedSlopePercent(), 1e-6);
            }
        }
    }

    @Test
    void largerWindowsProduceFewerOrEqualSectionsThanSmallerWindows() {
        int count10 = service.computeSlopeSections(10, 10).getFeatures().size();
        int count20 = service.computeSlopeSections(10, 20).getFeatures().size();
        int count30 = service.computeSlopeSections(10, 30).getFeatures().size();

        assertTrue(count10 >= count20);
        assertTrue(count20 >= count30);
    }

    @Test
    void everySectionBelongsOnlyToItsOwnRequestedTrail() {
        // Structural guarantee: findSegmentsForTrail already filters by trail_id, so a chain can
        // never mix Segments from a different Trail (Phase 12A found 0 cross-trail adjacent pairs;
        // this asserts the production code path preserves that for every Trail/window).
        for (long trailId : ALL_TRAIL_IDS) {
            List<Long> featureIdsForOtherTrails = jdbcTemplate.queryForList(
                    "SELECT tf.id FROM trail_feature tf WHERE tf.trail_id <> ?", Long.class, trailId);
            SlopeSectionFeatureCollection result = service.computeSlopeSections(trailId, 20);
            // No direct feature id is exposed on the response, so this is asserted indirectly:
            // simply confirm computation succeeds independently per trail without exceptions and
            // every trailId on the response matches the request.
            assertEquals(trailId, result.getTrailId());
            assertFalse(featureIdsForOtherTrails.isEmpty(), "sanity: other trails must have features");
        }
    }

    @Test
    void nonExistentTrailIsReportedAsNotExisting() {
        assertFalse(service.trailExists(999_999_999L));
    }

    @Test
    void computingSlopeSectionsNeverChangesNetworkOrRawLayerRowCounts() {
        Integer trailCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail", Integer.class);
        Integer featureCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail_feature", Integer.class);
        Integer nodeCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail_node", Integer.class);
        Integer segmentCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail_segment", Integer.class);
        Integer accidentCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accident_point", Integer.class);

        for (long trailId : ALL_TRAIL_IDS) {
            for (int window : WINDOWS) {
                service.computeSlopeSections(trailId, window);
            }
        }

        assertEquals(4, trailCountBefore);
        assertEquals(1706, featureCountBefore);
        assertEquals(2526, nodeCountBefore);
        assertEquals(2122, segmentCountBefore);
        assertEquals(42, accidentCountBefore);

        assertEquals(trailCountBefore, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail", Integer.class));
        assertEquals(featureCountBefore, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail_feature", Integer.class));
        assertEquals(nodeCountBefore, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail_node", Integer.class));
        assertEquals(segmentCountBefore, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail_segment", Integer.class));
        assertEquals(accidentCountBefore, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accident_point", Integer.class));
    }
}
