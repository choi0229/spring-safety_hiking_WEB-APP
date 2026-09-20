package com.season.semiproject.spatial.slope;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Production API read path for the SlopeSection Derived Analysis Layer: a plain
 * {@code SELECT ... FROM slope_section} plus GeoJSON assembly, nothing else. No
 * NetworkChainBuilder/ElevationProfile/SlopeSectionCalculator/ChainDistanceLocator call happens
 * here anymore -- SlopeSection is precomputed by {@link SlopeSectionBuildService} (the
 * `spatial-slope-build` profile) and this class only queries what that build already persisted.
 * See docs/09-slope-section-analysis.md for why this moved from compute-on-request to
 * precompute + persistence.
 *
 * Deliberately never falls back to computing on a cache miss (see docs/09, "No Runtime
 * Fallback") -- an empty result here means either a Trail with no SlopeSection coverage (the
 * pre-existing, expected meaning) or the Slope build has not been run yet; {@link #getSlopeSections}
 * logs a warning to make the latter case discoverable in ops without changing the API response.
 */
@Service
public class SlopeSectionService {

    private static final Logger log = LoggerFactory.getLogger(SlopeSectionService.class);

    /** Production only ever persists {@value SlopeSectionBuildService#PRODUCTION_WINDOW_METERS}m
     * -- see slope-section-schema.sql, chk_slope_section_window_m. */
    public static final Set<Integer> ALLOWED_WINDOW_METERS = Set.of(SlopeSectionBuildService.PRODUCTION_WINDOW_METERS);

    private final SlopeSectionDAO dao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SlopeSectionService(SlopeSectionDAO dao) {
        this.dao = dao;
    }

    public boolean trailExists(long trailId) {
        return dao.countTrailById(trailId) > 0;
    }

    public SlopeSectionFeatureCollection getSlopeSections(long trailId, int windowMeters) {
        List<SlopeSectionRow> rows = dao.findPersistedSections(trailId, windowMeters);
        if (rows.isEmpty()) {
            log.warn("No persisted SlopeSection rows for trailId={} windowMeters={} -- if this is "
                    + "unexpected, run the spatial-slope-build profile (see docs/09-slope-section-analysis.md)",
                    trailId, windowMeters);
        }

        List<SlopeSectionFeature> features = new ArrayList<>(rows.size());
        for (SlopeSectionRow row : rows) {
            JsonNode geometry = parseGeometry(row.getGeometryGeoJson());
            SlopeSectionProperties properties = new SlopeSectionProperties(
                    row.getChainSequence(), row.getSectionSequence(), row.getDistanceM(),
                    row.getEstimatedElevationStart(), row.getEstimatedElevationEnd(), row.getEstimatedElevationDelta(),
                    row.getEstimatedSlopePercent(), row.getDataQualityFlag());
            features.add(new SlopeSectionFeature(properties, geometry));
        }
        return new SlopeSectionFeatureCollection(trailId, windowMeters, features);
    }

    private JsonNode parseGeometry(String geoJson) {
        try {
            return objectMapper.readTree(geoJson);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid geometry GeoJSON stored in slope_section: " + geoJson, e);
        }
    }
}
