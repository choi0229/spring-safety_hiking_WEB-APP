package com.season.semiproject.spatial.geojson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-database verification of the Trail GeoJSON compatibility query (see
 * mapper-trail-geojson.xml), run against the live Docker PostgreSQL/PostGIS instance and the
 * actual imported TrailFeature data (1706 rows). Read-only except for the empty-dataset test,
 * which is wrapped in a transaction Spring's test framework rolls back afterward.
 */
@SpringBootTest
class TrailGeoJsonServiceTest {

    @Autowired
    private TrailGeoJsonService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void returnsAllTrailFeaturesAsOneFeatureCollection() throws Exception {
        JsonNode root = mapper.readTree(service.getTrailFeatureCollectionJson());

        assertEquals("FeatureCollection", root.get("type").asText());
        assertEquals(1706, root.get("features").size());
    }

    @Test
    void everyFeatureHasExpectedShapeAndProperties() throws Exception {
        JsonNode root = mapper.readTree(service.getTrailFeatureCollectionJson());

        for (JsonNode feature : root.get("features")) {
            assertEquals("Feature", feature.get("type").asText());
            assertEquals("MultiLineString", feature.get("geometry").get("type").asText());
            assertTrue(feature.get("properties").has("PMNTN_NM"));
            assertTrue(feature.get("properties").has("DN"));
            // trailId (t.id) was added so the Frontend can resolve the current database's Trail
            // id for a course instead of hardcoding one (see docs/09-slope-section-analysis.md,
            // "trail.id is a surrogate PK"). source_feature_index/sequence remain unexposed --
            // only fields an actual consumer reads belong here (see
            // docs/06-frontend-api-compatibility.md, "response property design").
            assertTrue(feature.get("properties").has("trailId"));
            assertFalse(feature.get("properties").has("source_feature_index"));
            assertFalse(feature.get("properties").has("sequence"));
        }
    }

    @Test
    void allFourCoursesArePresentWithExpectedFeatureCounts() throws Exception {
        JsonNode root = mapper.readTree(service.getTrailFeatureCollectionJson());

        Map<String, Integer> counts = new java.util.HashMap<>();
        for (JsonNode feature : root.get("features")) {
            String course = feature.get("properties").get("PMNTN_NM").asText();
            counts.merge(course, 1, Integer::sum);
        }

        assertEquals(Map.of("마루", 1267, "무악동구간", 216, "홍제동구간", 108, "부암동구간", 115), counts);
    }

    @Test
    void eachCourseNameMapsToExactlyOneTrailId() throws Exception {
        // The Frontend resolver (frontend/src/api/slopeSection.js, resolveTrailId) trusts that a
        // course name identifies exactly one trailId and throws otherwise -- this pins down that
        // assumption against the real data so a future import that breaks it fails loudly here
        // instead of as a silent Frontend error.
        JsonNode root = mapper.readTree(service.getTrailFeatureCollectionJson());

        Map<String, java.util.Set<Integer>> trailIdsByCourse = new java.util.HashMap<>();
        for (JsonNode feature : root.get("features")) {
            String course = feature.get("properties").get("PMNTN_NM").asText();
            int trailId = feature.get("properties").get("trailId").asInt();
            trailIdsByCourse.computeIfAbsent(course, k -> new java.util.HashSet<>()).add(trailId);
        }

        assertEquals(4, trailIdsByCourse.size());
        for (Map.Entry<String, java.util.Set<Integer>> entry : trailIdsByCourse.entrySet()) {
            assertEquals(1, entry.getValue().size(),
                    "course " + entry.getKey() + " maps to multiple trailId values: " + entry.getValue());
        }
    }

    @Test
    void featureOrderMatchesSourceFeatureIndexAscending() throws Exception {
        JsonNode root = mapper.readTree(service.getTrailFeatureCollectionJson());

        // Independent reference ordering, queried directly by source_feature_index -- the
        // production response itself does not expose this field (see above), so the test
        // reaches around it via JdbcTemplate rather than expanding the API contract just to
        // make ordering directly assertable.
        List<Map<String, Object>> reference = jdbcTemplate.queryForList(
                "SELECT tf.dn_value, t.source_course_name "
                        + "FROM trail_feature tf JOIN trail t ON t.id = tf.trail_id "
                        + "ORDER BY tf.source_feature_index ASC");

        assertEquals(reference.size(), root.get("features").size());
        for (int i = 0; i < reference.size(); i++) {
            JsonNode feature = root.get("features").get(i);
            Map<String, Object> expected = reference.get(i);
            assertEquals(((Number) expected.get("dn_value")).doubleValue(),
                    feature.get("properties").get("DN").asDouble(), 1e-9,
                    "DN mismatch at position " + i);
            assertEquals(expected.get("source_course_name"),
                    feature.get("properties").get("PMNTN_NM").asText(),
                    "PMNTN_NM mismatch at position " + i);
        }
    }

    @Test
    void geometryPrecisionMatchesFullStoredPrecisionNotDefaultNineDigits() throws Exception {
        // Known coordinate (마루, source_feature_index=0) whose raw stored value has more than
        // 9 decimal digits -- ST_AsGeoJSON(geom) with no precision argument would truncate this.
        JsonNode root = mapper.readTree(service.getTrailFeatureCollectionJson());
        JsonNode firstCoord = root.get("features").get(0).get("geometry").get("coordinates").get(0).get(0);

        assertEquals(126.95848586400908, firstCoord.get(0).asDouble(), 1e-11);
        assertEquals(37.61217639729065, firstCoord.get(1).asDouble(), 1e-11);
    }

    @Test
    @Transactional
    void emptyDatasetReturnsEmptyFeatureArrayNotNull() throws Exception {
        jdbcTemplate.update("DELETE FROM trail_feature");

        JsonNode root = mapper.readTree(service.getTrailFeatureCollectionJson());

        assertEquals("FeatureCollection", root.get("type").asText());
        assertTrue(root.get("features").isArray());
        assertEquals(0, root.get("features").size());
        assertFalse(root.get("features").isNull());
    }
}
