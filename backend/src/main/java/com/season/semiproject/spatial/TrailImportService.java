package com.season.semiproject.spatial;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.season.semiproject.spatial.manifest.TrailImportManifest;
import com.season.semiproject.spatial.manifest.TrailManifestEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Raw Spatial Layer importer: GeoJSON Feature -> TrailFeature, 1:1, no network/topology work.
 * See docs/01-architecture-and-modeling.md (Phase 4) and docs/02-migration-and-data-quality.md
 * (Phase 5, "Spatial Data Import") for the design this implements.
 *
 * Deliberately NOT wired into any Controller/API -- only SpatialImportRunner (active under the
 * `spatial-import` profile) calls this.
 */
@Service
public class TrailImportService {

    private static final Logger log = LoggerFactory.getLogger(TrailImportService.class);

    private final TrailImportDAO dao;
    // FAIL_ON_UNKNOWN_PROPERTIES disabled so the manifest JSON may carry a "_comment" field for
    // human documentation without needing a matching Java property.
    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final FeatureClassifier classifier = new FeatureClassifier();

    @Autowired
    public TrailImportService(TrailImportDAO dao) {
        this.dao = dao;
    }

    public TrailImportManifest loadManifest(InputStream in) throws IOException {
        return objectMapper.readValue(in, TrailImportManifest.class);
    }

    public JsonNode loadGeoJson(InputStream in) throws IOException {
        return objectMapper.readTree(in);
    }

    /**
     * source_file replace strategy: delete any previous import for this source file, then
     * re-insert everything in one transaction. A failure partway through (including an
     * unexpected classification failure, which is checked BEFORE any DB write) rolls the whole
     * transaction back, leaving the previous import state intact -- never a partially-deleted,
     * partially-inserted, or empty state.
     */
    @Transactional
    public TrailImportResult importTrails(TrailImportManifest manifest, JsonNode geoJsonRoot) {
        FeatureClassifier.ClassificationResult classification = classifier.classify(manifest, geoJsonRoot);

        if (!classification.unexpected.isEmpty()) {
            throw new TrailImportException(
                    "Import aborted before any DB write -- " + classification.unexpected.size()
                            + " unexpected/undeclared feature(s) found in "
                            + manifest.getSourceFile() + ":\n"
                            + String.join("\n", classification.unexpected));
        }

        log.info("Classification for {}: total={}, included={}, excluded={}",
                manifest.getSourceFile(), classification.totalFeatures,
                classification.includedCount, classification.excludedCounts);

        dao.deleteTrailsBySourceFile(manifest.getSourceFile());

        TrailImportResult result = new TrailImportResult(manifest.getSourceFile(), classification.totalFeatures);
        result.includedFeatureCount = classification.includedCount;
        result.excludedCounts.putAll(classification.excludedCounts);

        for (TrailManifestEntry trailDef : manifest.getTrails()) {
            List<FeatureClassifier.ClassifiedFeature> features =
                    classification.byTrail.getOrDefault(trailDef.getPmntnNm(), List.of());

            TrailInsertParam trailParam = new TrailInsertParam();
            trailParam.setCourseId(trailDef.getCourseId());
            trailParam.setSourceFile(manifest.getSourceFile());
            trailParam.setSourceMountainName(trailDef.getSourceMountainName());
            trailParam.setSourceCourseName(trailDef.getSourceCourseName());
            Long trailId = dao.insertTrail(trailParam);

            for (FeatureClassifier.ClassifiedFeature cf : features) {
                JsonNode properties = cf.feature.get("properties");
                JsonNode geometry = cf.feature.get("geometry");
                TrailFeatureInsertParam featureParam = new TrailFeatureInsertParam();
                featureParam.setTrailId(trailId);
                featureParam.setSequence(cf.sequence);
                featureParam.setSourceFeatureIndex(cf.sourceFeatureIndex);
                featureParam.setDnValue(properties.get("DN").asDouble());
                try {
                    featureParam.setGeometryJson(objectMapper.writeValueAsString(geometry));
                } catch (IOException e) {
                    // Serializing a JsonNode we already parsed cannot fail in practice;
                    // wrapped only to satisfy the checked-exception signature.
                    throw new TrailImportException("Failed to serialize geometry for feature index "
                            + cf.sourceFeatureIndex + ": " + e.getMessage());
                }
                dao.insertTrailFeature(featureParam);
            }

            result.trails.add(new TrailImportResult.TrailSummary(
                    trailDef.getPmntnNm(), trailId, trailDef.getCourseId(), features.size()));
        }

        return result;
    }
}
