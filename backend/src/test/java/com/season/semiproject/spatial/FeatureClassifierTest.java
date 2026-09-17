package com.season.semiproject.spatial;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.season.semiproject.spatial.manifest.ExcludedFeatureEntry;
import com.season.semiproject.spatial.manifest.TrailImportManifest;
import com.season.semiproject.spatial.manifest.TrailManifestEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureClassifierTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final FeatureClassifier classifier = new FeatureClassifier();

    private TrailManifestEntry trail(String pmntnNm, String mountain) {
        TrailManifestEntry t = new TrailManifestEntry();
        t.setPmntnNm(pmntnNm);
        t.setSourceMountainName(mountain);
        t.setSourceCourseName(pmntnNm);
        t.setCourseId(null);
        t.setMappingStatus("UNVERIFIED");
        t.setMappingReason("test fixture");
        return t;
    }

    private ExcludedFeatureEntry excluded(int index, String category) {
        ExcludedFeatureEntry e = new ExcludedFeatureEntry();
        e.setSourceFeatureIndex(index);
        e.setCategory(category);
        e.setReason("test fixture");
        return e;
    }

    private JsonNode feature(String pmntnNm, double dn, String coordsJson) throws Exception {
        return mapper.readTree(String.format("""
            {"type":"Feature",
             "properties":{"PMNTN_NM":"%s","DN":%s},
             "geometry":{"type":"MultiLineString","coordinates":%s}}
            """, pmntnNm, dn, coordsJson));
    }

    /** Manifest loading round-trips real JSON text through Jackson correctly. */
    @Test
    void manifestLoadsFromJson() throws Exception {
        String json = """
            {"sourceFile":"test.geojson",
             "trails":[{"pmntnNm":"마루","sourceMountainName":"북한산_백운대","sourceCourseName":"마루",
                        "courseId":1,"mappingStatus":"VERIFIED","mappingReason":"r","note":"n"}],
             "excludedFeatures":[{"sourceFeatureIndex":5,"pmntnNm":" ","category":"EXCLUDED_SOURCE_FEATURE","reason":"r"}]}
            """;
        TrailImportManifest manifest = mapper.readValue(json, TrailImportManifest.class);
        assertEquals("test.geojson", manifest.getSourceFile());
        assertEquals(1, manifest.getTrails().size());
        assertEquals("마루", manifest.getTrails().get(0).getPmntnNm());
        assertEquals(Integer.valueOf(1), manifest.getTrails().get(0).getCourseId());
        assertEquals(1, manifest.getExcludedFeatures().size());
        assertEquals(5, manifest.getExcludedFeatures().get(0).getSourceFeatureIndex());
    }

    @Test
    void groupsFeaturesByTrailAndPreservesSourceIndexAndAssignsSourceOrderSequence() throws Exception {
        TrailImportManifest manifest = new TrailImportManifest();
        manifest.setTrails(List.of(trail("마루", "북한산_백운대")));
        manifest.setExcludedFeatures(List.of(excluded(1, "EXCLUDED_SOURCE_FEATURE")));

        // index 0,2 = 마루 (valid); index 1 = declared noise exclusion (blank PMNTN_NM)
        JsonNode fc = mapper.readTree("{\"type\":\"FeatureCollection\",\"features\":[]}");
        var arr = mapper.createArrayNode();
        arr.add(feature("마루", 100, "[[[126.0,37.0],[126.1,37.1]]]"));
        arr.add(feature(" ", 101, "[[[126.1,37.1],[126.2,37.2]]]"));
        arr.add(feature("마루", 102, "[[[126.2,37.2],[126.3,37.3]]]"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) fc).set("features", arr);

        FeatureClassifier.ClassificationResult result = classifier.classify(manifest, fc);

        assertTrue(result.unexpected.isEmpty(), "no unexpected features expected: " + result.unexpected);
        assertEquals(3, result.totalFeatures);
        assertEquals(2, result.includedCount);
        assertEquals(Integer.valueOf(1), result.excludedCounts.get("EXCLUDED_SOURCE_FEATURE"));

        List<FeatureClassifier.ClassifiedFeature> maru = result.byTrail.get("마루");
        assertEquals(2, maru.size());
        // source_feature_index must be the ORIGINAL absolute index (0 and 2), not renumbered
        assertEquals(0, maru.get(0).sourceFeatureIndex);
        assertEquals(2, maru.get(1).sourceFeatureIndex);
        // sequence must be the continuous 0..n-1 position among INCLUDED features only
        assertEquals(0, maru.get(0).sequence);
        assertEquals(1, maru.get(1).sequence);
    }

    @Test
    void invalidGeometryDeclaredInManifestIsExcludedNotUnexpected() throws Exception {
        TrailImportManifest manifest = new TrailImportManifest();
        manifest.setTrails(List.of(trail("마루", "북한산_백운대")));
        manifest.setExcludedFeatures(List.of(excluded(0, "INVALID_GEOMETRY_SOURCE_FEATURE")));

        var fc = (com.fasterxml.jackson.databind.node.ObjectNode)
                mapper.readTree("{\"type\":\"FeatureCollection\",\"features\":[]}");
        var arr = mapper.createArrayNode();
        arr.add(feature("마루", 100, "[[[126.0,37.0]]]")); // degenerate 1-point sub-line
        fc.set("features", arr);

        FeatureClassifier.ClassificationResult result = classifier.classify(manifest, fc);

        assertTrue(result.unexpected.isEmpty());
        assertEquals(0, result.includedCount);
        assertEquals(Integer.valueOf(1), result.excludedCounts.get("INVALID_GEOMETRY_SOURCE_FEATURE"));
    }

    @Test
    void undeclaredAnomalyIsReportedAsUnexpectedNotSilentlyDropped() throws Exception {
        TrailImportManifest manifest = new TrailImportManifest();
        manifest.setTrails(List.of(trail("마루", "북한산_백운대")));
        manifest.setExcludedFeatures(List.of()); // nothing declared

        var fc = (com.fasterxml.jackson.databind.node.ObjectNode)
                mapper.readTree("{\"type\":\"FeatureCollection\",\"features\":[]}");
        var arr = mapper.createArrayNode();
        arr.add(feature("마루", 100, "[[[126.0,37.0]]]")); // degenerate, NOT declared -> unexpected
        fc.set("features", arr);

        FeatureClassifier.ClassificationResult result = classifier.classify(manifest, fc);

        assertEquals(1, result.unexpected.size());
        assertEquals(0, result.includedCount);
        assertTrue(result.excludedCounts.isEmpty());
    }

    @Test
    void unmatchedPmntnNmNotDeclaredIsUnexpected() throws Exception {
        TrailImportManifest manifest = new TrailImportManifest();
        manifest.setTrails(List.of(trail("마루", "북한산_백운대")));
        manifest.setExcludedFeatures(List.of()); // "다른코스" not declared anywhere

        var fc = (com.fasterxml.jackson.databind.node.ObjectNode)
                mapper.readTree("{\"type\":\"FeatureCollection\",\"features\":[]}");
        var arr = mapper.createArrayNode();
        arr.add(feature("다른코스", 100, "[[[126.0,37.0],[126.1,37.1]]]"));
        fc.set("features", arr);

        FeatureClassifier.ClassificationResult result = classifier.classify(manifest, fc);

        assertEquals(1, result.unexpected.size());
        assertTrue(result.unexpected.get(0).contains("다른코스"));
    }

    @Test
    void rejectsNonFeatureCollectionRoot() throws Exception {
        TrailImportManifest manifest = new TrailImportManifest();
        manifest.setTrails(List.of());
        manifest.setExcludedFeatures(List.of());
        JsonNode notAFeatureCollection = mapper.readTree("{\"type\":\"Feature\"}");

        assertThrows(TrailImportException.class, () -> classifier.classify(manifest, notAFeatureCollection));
    }
}
