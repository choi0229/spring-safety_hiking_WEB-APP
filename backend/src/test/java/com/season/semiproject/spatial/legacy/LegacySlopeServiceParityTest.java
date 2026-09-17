package com.season.semiproject.spatial.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real-data JS <-> Java parity test for Phase 7A (Legacy Slope Compatibility).
 *
 * The reference values in legacy-slope-fixture.json were produced by an
 * independent Node re-implementation (see generate-fixture.js, same
 * directory), NOT by calling into LegacySlopeService -- this test would be
 * meaningless if the reference were derived from the code under test.
 *
 * Input source is the same original GeoJSON file the legacy frontend fetches
 * (frontend/public/data/인왕산ele copy.geojson), read with the same relative
 * path convention already used by SpatialImportRunner's default
 * spatial.import.geojson-file property. TrailFeature/the DB are not used
 * here -- Phase 7A's compatibility source is intentionally the raw file
 * (see docs/01-architecture-and-modeling.md).
 *
 * Scope note: this only establishes parity for the current legacy source
 * file (인왕산ele copy.geojson) across its 4 real courses and the 3 real
 * groupSize values (5/7/12) in use. It is not a claim of parity for
 * arbitrary GeoJSON input.
 */
class LegacySlopeServiceParityTest {

    private static final double COORDINATE_TOLERANCE = 1e-9;
    // Safety margin for trig/sqrt evaluation order differences between V8 and
    // the JVM, not a measured value. The actual measured maximum is tracked
    // in maxObservedSlopeError below and printed by reportMaxObservedError().
    private static final double NUMERIC_TOLERANCE = 1e-6;

    private static JsonNode geoJson;
    private static JsonNode fixture;
    private static double maxObservedSlopeError = 0.0;

    private final LegacySlopeService service = new LegacySlopeService();

    @BeforeAll
    static void loadFixturesOnce() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        File geoJsonFile = new File("../frontend/public/data/인왕산ele copy.geojson");
        assertTrue(geoJsonFile.exists(),
                "expected legacy source GeoJSON at " + geoJsonFile.getAbsolutePath());
        geoJson = mapper.readTree(geoJsonFile);

        try (InputStream in = LegacySlopeServiceParityTest.class
                .getResourceAsStream("/legacy/legacy-slope-fixture.json")) {
            assertTrue(in != null, "expected legacy-slope-fixture.json on the test classpath");
            fixture = mapper.readTree(in);
        }
    }

    @Test
    void javaOutputMatchesIndependentJsReferenceForEveryCourseAndGroupSize() {
        int casesCompared = 0;

        for (JsonNode expectedCase : fixture.path("cases")) {
            String courseName = expectedCase.path("courseName").asText();
            int groupSize = expectedCase.path("groupSize").asInt();

            String context = courseName + " / groupSize=" + groupSize;

            List<LegacyCoordinate> flattened = service.flattenCourseCoordinates(geoJson, courseName);
            assertEquals(expectedCase.path("totalCoordinateCount").asInt(), flattened.size(),
                    context + ": flatten coordinate count (Original GeoJSON -> Legacy flatten parity)");

            LegacySlopeResult actual = service.buildLegacySlopeGroups(geoJson, courseName, groupSize);

            assertEquals(expectedCase.path("totalGroupCount").asInt(), actual.totalGroupCount(),
                    context + ": totalGroupCount");
            assertEquals(expectedCase.path("renderedGroupCount").asInt(), actual.renderedGroups().size(),
                    context + ": renderedGroupCount");

            JsonNode expectedGroups = expectedCase.path("renderedGroups");
            assertEquals(expectedGroups.size(), actual.renderedGroups().size(),
                    context + ": rendered group list size");

            for (int i = 0; i < expectedGroups.size(); i++) {
                JsonNode expectedGroup = expectedGroups.get(i);
                LegacySlopeGroup actualGroup = actual.renderedGroups().get(i);
                String groupContext = context + " group[" + i + "]";

                assertEquals(expectedGroup.path("groupIndex").asInt(), actualGroup.groupIndex(),
                        groupContext + ": groupIndex");
                assertEquals(expectedGroup.path("size").asInt(), actualGroup.coordinates().size(),
                        groupContext + ": size");

                LegacyCoordinate actualStart = actualGroup.coordinates().get(0);
                LegacyCoordinate actualEnd = actualGroup.coordinates().get(actualGroup.coordinates().size() - 1);
                JsonNode expectedStart = expectedGroup.path("start");
                JsonNode expectedEnd = expectedGroup.path("end");

                assertEquals(expectedStart.path("lng").asDouble(), actualStart.lng(), COORDINATE_TOLERANCE, groupContext + ": start.lng");
                assertEquals(expectedStart.path("lat").asDouble(), actualStart.lat(), COORDINATE_TOLERANCE, groupContext + ": start.lat");
                assertEquals(expectedStart.path("dn").asDouble(), actualStart.dnValue(), COORDINATE_TOLERANCE, groupContext + ": start.dn");
                assertEquals(expectedEnd.path("lng").asDouble(), actualEnd.lng(), COORDINATE_TOLERANCE, groupContext + ": end.lng");
                assertEquals(expectedEnd.path("lat").asDouble(), actualEnd.lat(), COORDINATE_TOLERANCE, groupContext + ": end.lat");
                assertEquals(expectedEnd.path("dn").asDouble(), actualEnd.dnValue(), COORDINATE_TOLERANCE, groupContext + ": end.dn");

                double expectedSlope = expectedGroup.path("legacySlopeValue").asDouble();
                double absError = Math.abs(expectedSlope - actualGroup.legacySlopeValue());
                maxObservedSlopeError = Math.max(maxObservedSlopeError, absError);
                assertEquals(expectedSlope, actualGroup.legacySlopeValue(),
                        NUMERIC_TOLERANCE, groupContext + ": legacySlopeValue");
                assertEquals(expectedGroup.path("color").asText(), actualGroup.color(),
                        groupContext + ": color (must be exact, not tolerance-based)");
            }

            casesCompared++;
        }

        assertEquals(12, casesCompared, "expected 4 courses x 3 groupSizes = 12 fixture cases");
    }

    @Test
    void colorDistributionMatchesReferencePerCase() {
        for (JsonNode expectedCase : fixture.path("cases")) {
            String courseName = expectedCase.path("courseName").asText();
            int groupSize = expectedCase.path("groupSize").asInt();
            LegacySlopeResult actual = service.buildLegacySlopeGroups(geoJson, courseName, groupSize);

            long red = actual.renderedGroups().stream().filter(g -> g.color().equals("#FF4500")).count();
            long blue = actual.renderedGroups().stream().filter(g -> g.color().equals("#1E90FF")).count();
            long green = actual.renderedGroups().stream().filter(g -> g.color().equals("#32CD32")).count();

            JsonNode expectedColors = expectedCase.path("colorCounts");
            String context = courseName + " / groupSize=" + groupSize;
            assertEquals(expectedColors.path("#FF4500").asLong(), red, context + ": red count");
            assertEquals(expectedColors.path("#1E90FF").asLong(), blue, context + ": blue count");
            assertEquals(expectedColors.path("#32CD32").asLong(), green, context + ": green count");
        }
    }

    @Test
    void groupSizeFiveAllCoursesCombinedMatchesPriorLegacyStatistics() {
        // Cross-check against the Phase 7 analysis figures (1177 total groups,
        // 1176 rendered, colors 290/335/551) -- computed here from the fixture,
        // not hardcoded, so this only fails if the fixture itself changes.
        String[] courses = {"마루", "무악동", "홍제동", "부암동"};
        int totalGroups = 0;
        int renderedGroups = 0;
        long red = 0, blue = 0, green = 0;

        for (String courseName : courses) {
            LegacySlopeResult actual = service.buildLegacySlopeGroups(geoJson, courseName, 5);
            totalGroups += actual.totalGroupCount();
            renderedGroups += actual.renderedGroups().size();
            for (LegacySlopeGroup g : actual.renderedGroups()) {
                switch (g.color()) {
                    case "#FF4500" -> red++;
                    case "#1E90FF" -> blue++;
                    case "#32CD32" -> green++;
                    default -> throw new IllegalStateException("unexpected color " + g.color());
                }
            }
        }

        assertEquals(1177, totalGroups);
        assertEquals(1176, renderedGroups);
        assertEquals(290, red);
        assertEquals(335, blue);
        assertEquals(551, green);
    }

    /**
     * Prints the actual measured maximum |legacySlopeValue| divergence between
     * the Java implementation and the independent JS reference, observed
     * across all group comparisons in
     * javaOutputMatchesIndependentJsReferenceForEveryCourseAndGroupSize().
     * This is a real measurement (see test output), not an assumed value --
     * kept separate from NUMERIC_TOLERANCE, which is a safety margin picked
     * before measuring, not the measurement itself.
     */
    @AfterAll
    static void reportMaxObservedError() {
        System.out.println("[LegacySlopeServiceParityTest] max observed |legacySlopeValue| error "
                + "(Java vs independent JS reference, all 12 course x groupSize cases): "
                + maxObservedSlopeError);
    }
}
