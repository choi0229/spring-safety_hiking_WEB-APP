package com.season.semiproject.spatial.geojson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.season.semiproject.spatial.manifest.TrailImportManifest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for the Original(1719) vs API(1706) difference introduced by Phase 5's
 * exclusion of 13 Features (see trail-import-manifest.json / docs/02-migration-and-data-quality.md).
 * This difference is INTENTIONAL -- these tests fix it as a known, expected shape rather than
 * treating it as a defect, and separately verify that every Feature the API DOES return still
 * matches its original source Feature exactly (property + geometry parity, precision-aware).
 */
@SpringBootTest
class TrailGeoJsonKnownDifferenceTest {

    private static final String GEOJSON_PATH = "../frontend/public/data/인왕산ele copy.geojson";
    private static final List<String> TRACKED_COURSES =
            List.of("마루", "무악동구간", "홍제동구간", "부암동구간");
    private static final double COORDINATE_TOLERANCE = 1e-9;

    @Autowired
    private TrailGeoJsonService service;

    private static JsonNode originalGeoJson;
    private static Set<Integer> excludedIndices;

    @BeforeAll
    static void loadOriginalSourceAndManifest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        originalGeoJson = mapper.readTree(new File(GEOJSON_PATH));

        ObjectMapper manifestMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        try (InputStream in = TrailGeoJsonKnownDifferenceTest.class.getClassLoader()
                .getResourceAsStream("spatial-import/trail-import-manifest.json")) {
            assertNotNull(in, "expected trail-import-manifest.json on the classpath");
            TrailImportManifest manifest = manifestMapper.readValue(in, TrailImportManifest.class);
            excludedIndices = manifest.excludedByIndex().keySet();
        }
    }

    @Test
    void originalHas1719FeaturesApiHas1706ExcludedIs13() throws Exception {
        assertEquals(1719, originalGeoJson.get("features").size());
        assertEquals(13, excludedIndices.size());

        JsonNode apiRoot = new ObjectMapper().readTree(service.getTrailFeatureCollectionJson());
        assertEquals(1706, apiRoot.get("features").size());
    }

    @Test
    void noExcludedFeatureGeometryContributesToTheApiResponse() throws Exception {
        // (PMNTN_NM, DN) is NOT a reliable identity key -- DN repeats naturally across many
        // Features along the same course, so an excluded Feature's DN can coincidentally match a
        // legitimately-included one. Geometry coordinates are the one attribute unique enough per
        // Feature to detect a real leak, so this test uses each excluded Feature's exact
        // coordinate array (still present, just not the join key normally used) as the signature.
        Set<String> excludedGeometrySignatures = new HashSet<>();
        JsonNode features = originalGeoJson.get("features");
        for (Integer idx : excludedIndices) {
            JsonNode feature = features.get(idx);
            String pmntn = feature.get("properties").get("PMNTN_NM").asText();
            if (TRACKED_COURSES.contains(pmntn)) {
                excludedGeometrySignatures.add(feature.get("geometry").get("coordinates").toString());
            }
        }
        // The 9 INVALID_GEOMETRY features (8 마루 + 1 홍제동구간) are the only excluded ones that
        // carry a tracked course label; the 4 blank-PMNTN_NM ones can never appear under any
        // tracked course name in the first place. Some of the 9 happen to share identical
        // degenerate single-point coordinates with each other, so the distinct-signature count
        // can be less than 9 -- only a non-empty, bounded-by-9 set is asserted here.
        assertTrue(excludedGeometrySignatures.size() > 0 && excludedGeometrySignatures.size() <= 9);

        JsonNode apiRoot = new ObjectMapper().readTree(service.getTrailFeatureCollectionJson());
        for (JsonNode feature : apiRoot.get("features")) {
            String signature = feature.get("geometry").get("coordinates").toString();
            assertFalse(excludedGeometrySignatures.contains(signature),
                    "an excluded Feature's exact geometry leaked into the API response: " + signature);
        }
    }

    @Test
    void everyApiFeatureExactlyMatchesItsOriginalSourceFeature() throws Exception {
        // Reconstruct the expected sequence exactly like TrailImportService's classification
        // (original order, tracked-course filter, excluded indices removed) and compare it
        // feature-by-feature against the full 1706-Feature API response.
        List<JsonNode> expected = new ArrayList<>();
        JsonNode features = originalGeoJson.get("features");
        for (int i = 0; i < features.size(); i++) {
            if (excludedIndices.contains(i)) {
                continue;
            }
            JsonNode feature = features.get(i);
            if (TRACKED_COURSES.contains(feature.get("properties").get("PMNTN_NM").asText())) {
                expected.add(feature);
            }
        }
        assertEquals(1706, expected.size());

        JsonNode apiRoot = new ObjectMapper().readTree(service.getTrailFeatureCollectionJson());
        JsonNode apiFeatures = apiRoot.get("features");
        assertEquals(expected.size(), apiFeatures.size());

        for (int i = 0; i < expected.size(); i++) {
            JsonNode expectedFeature = expected.get(i);
            JsonNode apiFeature = apiFeatures.get(i);
            String context = "feature at position " + i;

            assertEquals(expectedFeature.get("properties").get("PMNTN_NM").asText(),
                    apiFeature.get("properties").get("PMNTN_NM").asText(), context + ": PMNTN_NM");
            assertEquals(expectedFeature.get("properties").get("DN").asDouble(),
                    apiFeature.get("properties").get("DN").asDouble(), 1e-9, context + ": DN");

            JsonNode expectedGeom = expectedFeature.get("geometry");
            JsonNode apiGeom = apiFeature.get("geometry");
            assertEquals(expectedGeom.get("type").asText(), apiGeom.get("type").asText(), context + ": geometry.type");
            assertCoordinatesMatch(expectedGeom.get("coordinates"), apiGeom.get("coordinates"), context);
        }
    }

    private void assertCoordinatesMatch(JsonNode expected, JsonNode actual, String context) {
        assertEquals(expected.size(), actual.size(), context + ": sub-line count");
        for (int i = 0; i < expected.size(); i++) {
            JsonNode expectedLine = expected.get(i);
            JsonNode actualLine = actual.get(i);
            assertEquals(expectedLine.size(), actualLine.size(), context + ": sub-line[" + i + "] point count");
            for (int j = 0; j < expectedLine.size(); j++) {
                assertEquals(expectedLine.get(j).get(0).asDouble(), actualLine.get(j).get(0).asDouble(),
                        COORDINATE_TOLERANCE, context + ": sub-line[" + i + "] point[" + j + "] lng");
                assertEquals(expectedLine.get(j).get(1).asDouble(), actualLine.get(j).get(1).asDouble(),
                        COORDINATE_TOLERANCE, context + ": sub-line[" + i + "] point[" + j + "] lat");
            }
        }
    }
}
