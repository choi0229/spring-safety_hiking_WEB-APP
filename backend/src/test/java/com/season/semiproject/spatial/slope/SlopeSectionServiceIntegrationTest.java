package com.season.semiproject.spatial.slope;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-database verification of the Production Query path: {@link SlopeSectionService} now only
 * reads already-persisted `slope_section` rows (see docs/09-slope-section-analysis.md,
 * "compute-on-request -> precompute + persistence") -- no NetworkChainBuilder/ElevationProfile/
 * SlopeSectionCalculator call happens on this path anymore. {@link SlopeSectionBuildServiceIntegrationTest}
 * covers the Build (precompute) side; this class covers the Query side reading what a build left
 * behind.
 *
 * Each test ensures the table is populated by calling {@link SlopeSectionBuildService#buildAll()}
 * itself first (idempotent delete-all-then-insert), so this class never depends on build order
 * relative to other test classes.
 */
@SpringBootTest
class SlopeSectionServiceIntegrationTest {

    private static final long[] ALL_TRAIL_IDS = { 10, 11, 12, 13 };
    private static final int WINDOW = SlopeSectionBuildService.PRODUCTION_WINDOW_METERS;

    // 인왕산 bounding box (WGS84), generous margin -- sanity-checks that returned section
    // geometry coordinates are real lon/lat in the right place, not degenerate/garbled.
    private static final double MIN_LON = 126.90;
    private static final double MAX_LON = 127.00;
    private static final double MIN_LAT = 37.55;
    private static final double MAX_LAT = 37.62;

    @Autowired
    private SlopeSectionService service;

    @Autowired
    private SlopeSectionBuildService buildService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allFourTrailsProduceSlopeSectionsAfterBuild() {
        buildService.buildAll();
        for (long trailId : ALL_TRAIL_IDS) {
            SlopeSectionFeatureCollection result = service.getSlopeSections(trailId, WINDOW);
            assertFalse(result.getFeatures().isEmpty(), "trail " + trailId + " produced zero sections");
        }
    }

    @Test
    void sectionCountsMatchTheKnownBaseline() {
        buildService.buildAll();
        assertEquals(251, service.getSlopeSections(10, WINDOW).getFeatures().size());
        assertEquals(49, service.getSlopeSections(11, WINDOW).getFeatures().size());
        assertEquals(21, service.getSlopeSections(12, WINDOW).getFeatures().size());
        assertEquals(30, service.getSlopeSections(13, WINDOW).getFeatures().size());
    }

    @Test
    void everySectionHasValidNonDegenerateGeometryAndFiniteSlope() {
        buildService.buildAll();
        SlopeSectionFeatureCollection result = service.getSlopeSections(13, WINDOW);
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
        buildService.buildAll();
        SlopeSectionFeatureCollection result = service.getSlopeSections(10, WINDOW);
        for (SlopeSectionFeature feature : result.getFeatures()) {
            double delta = feature.getProperties().getEstimatedElevationDelta();
            double distance = feature.getProperties().getDistanceMeters();
            double expectedSlope = delta / distance * 100.0;
            assertEquals(expectedSlope, feature.getProperties().getEstimatedSlopePercent(), 1e-6);
        }
    }

    @Test
    void sectionsAreOrderedByChainThenSectionSequence() {
        buildService.buildAll();
        SlopeSectionFeatureCollection result = service.getSlopeSections(10, WINDOW);
        var features = result.getFeatures();
        for (int i = 1; i < features.size(); i++) {
            var prev = features.get(i - 1).getProperties();
            var cur = features.get(i).getProperties();
            boolean orderedOk = prev.getChainIndex() < cur.getChainIndex()
                    || (prev.getChainIndex() == cur.getChainIndex() && prev.getSectionIndex() < cur.getSectionIndex());
            assertTrue(orderedOk, "sections must be ordered by (chainIndex, sectionIndex) ascending");
        }
    }

    @Test
    void onlyWindowMeters20IsAccepted() {
        assertEquals(java.util.Set.of(20), SlopeSectionService.ALLOWED_WINDOW_METERS);
    }

    @Test
    void nonExistentTrailIsReportedAsNotExisting() {
        assertFalse(service.trailExists(999_999_999L));
    }

    @Test
    void absentWindowValueReturnsEmptyNotError() {
        buildService.buildAll();
        // window_m=10 is never persisted by the build (only 20 is, see
        // slope-section-schema.sql chk_slope_section_window_m) -- calling the Query service
        // directly (bypassing Controller-level ALLOWED_WINDOW_METERS validation) for that
        // combination must return an empty FeatureCollection, never throw and never silently
        // compute anything (no runtime fallback -- see docs/09).
        SlopeSectionFeatureCollection result = service.getSlopeSections(10, 10);
        assertNotNull(result);
        assertTrue(result.getFeatures().isEmpty());

        // sanity: the same trail at the actually-persisted window IS non-empty, proving "empty"
        // above genuinely means "absent", not a broken query.
        assertFalse(service.getSlopeSections(10, WINDOW).getFeatures().isEmpty());
    }

    @Test
    void queryingSlopeSectionsNeverChangesRawOrNetworkLayerRowCounts() {
        Integer trailCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail", Integer.class);
        Integer featureCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail_feature", Integer.class);
        Integer nodeCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail_node", Integer.class);
        Integer segmentCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trail_segment", Integer.class);
        Integer accidentCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accident_point", Integer.class);

        buildService.buildAll();
        for (long trailId : ALL_TRAIL_IDS) {
            service.getSlopeSections(trailId, WINDOW);
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
