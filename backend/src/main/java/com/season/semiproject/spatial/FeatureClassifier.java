package com.season.semiproject.spatial;

import com.fasterxml.jackson.databind.JsonNode;
import com.season.semiproject.spatial.manifest.ExcludedFeatureEntry;
import com.season.semiproject.spatial.manifest.TrailImportManifest;
import com.season.semiproject.spatial.manifest.TrailManifestEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure classification logic (no DB, no file I/O) so it can be unit-tested directly with small
 * synthetic GeoJSON fixtures. For every Feature in file order, decides one of:
 *   - included in a manifest-declared Trail (source-order sequence assigned here)
 *   - a manifest-declared exclusion (EXCLUDED_SOURCE_FEATURE / INVALID_GEOMETRY_SOURCE_FEATURE)
 *   - unexpected (not declared anywhere) -- collected and reported, never silently dropped
 *
 * `sequence` assigned here is strictly the "source-order sequence" defined in
 * docs/02-migration-and-data-quality.md: the position of this Feature among the *other included
 * Features of the same Trail*, in file order. It is NOT a network/traversal/routing order.
 */
public class FeatureClassifier {

    private final GeoJsonFeatureValidator validator = new GeoJsonFeatureValidator();

    public static class ClassifiedFeature {
        public final int sourceFeatureIndex;
        public final int sequence;
        public final JsonNode feature;

        ClassifiedFeature(int sourceFeatureIndex, int sequence, JsonNode feature) {
            this.sourceFeatureIndex = sourceFeatureIndex;
            this.sequence = sequence;
            this.feature = feature;
        }
    }

    public static class ClassificationResult {
        /** pmntnNm -> ordered list of included features (already in file/source order). */
        public final Map<String, List<ClassifiedFeature>> byTrail = new LinkedHashMap<>();
        public final Map<String, Integer> excludedCounts = new LinkedHashMap<>();
        public final List<String> unexpected = new ArrayList<>();
        public int totalFeatures;
        public int includedCount;
    }

    public ClassificationResult classify(TrailImportManifest manifest, JsonNode featureCollection) {
        JsonNode typeNode = featureCollection.get("type");
        if (typeNode == null || !"FeatureCollection".equals(typeNode.asText())) {
            throw new TrailImportException("root GeoJSON type is not 'FeatureCollection' (was: " + typeNode + ")");
        }
        JsonNode features = featureCollection.get("features");
        if (features == null || !features.isArray()) {
            throw new TrailImportException("'features' is missing or not an array");
        }

        Map<String, TrailManifestEntry> trailsByName = manifest.trailsByPmntnNm();
        Map<Integer, ExcludedFeatureEntry> excludedByIndex = manifest.excludedByIndex();

        ClassificationResult result = new ClassificationResult();
        result.totalFeatures = features.size();

        for (int i = 0; i < features.size(); i++) {
            JsonNode feature = features.get(i);
            FeatureValidationResult vr = validator.validate(feature);
            String pmntnNm = feature.path("properties").path("PMNTN_NM").isMissingNode()
                    ? null : feature.path("properties").path("PMNTN_NM").asText();

            if (!vr.isValid()) {
                ExcludedFeatureEntry ex = excludedByIndex.get(i);
                if (ex != null && "INVALID_GEOMETRY_SOURCE_FEATURE".equals(ex.getCategory())) {
                    result.excludedCounts.merge(ex.getCategory(), 1, Integer::sum);
                } else {
                    result.unexpected.add("index=" + i + " failed structural validation ("
                            + vr.getReason() + ") and is NOT declared in manifest.excludedFeatures");
                }
                continue;
            }

            TrailManifestEntry trail = trailsByName.get(pmntnNm);
            if (trail != null) {
                List<ClassifiedFeature> list = result.byTrail.computeIfAbsent(pmntnNm, k -> new ArrayList<>());
                list.add(new ClassifiedFeature(i, list.size(), feature));
                result.includedCount++;
                continue;
            }

            ExcludedFeatureEntry ex = excludedByIndex.get(i);
            if (ex != null && "EXCLUDED_SOURCE_FEATURE".equals(ex.getCategory())) {
                result.excludedCounts.merge(ex.getCategory(), 1, Integer::sum);
            } else {
                result.unexpected.add("index=" + i + " PMNTN_NM='" + pmntnNm
                        + "' does not match any declared Trail and is NOT declared in manifest.excludedFeatures");
            }
        }

        return result;
    }
}
