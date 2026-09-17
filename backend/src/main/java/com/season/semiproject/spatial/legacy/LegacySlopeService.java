package com.season.semiproject.spatial.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy Slope Compatibility (Phase 7A). Reproduces the legacy frontend's
 * {@code processGeoJSON()} flatten/filter step, then delegates grouping and
 * slope/color math to {@link LegacySlopeCalculator}.
 *
 * Deliberately takes GeoJSON content directly (a parsed {@link JsonNode}),
 * not a file path -- this class has no knowledge of where the GeoJSON comes
 * from, so it does not depend on the frontend directory layout. Callers
 * (regression tests today; a future Phase 9 controller) own reading the file.
 *
 * Input source is the ORIGINAL GeoJSON semantics, not the validated
 * TrailFeature DB layer: {@code processGeoJSON()} has no geometry validation,
 * so features excluded from TrailFeature for invalid geometry are still
 * included here, exactly as the legacy frontend includes them. See
 * docs/01-architecture-and-modeling.md for the "Legacy Compatibility Source
 * vs Validated Spatial Source" distinction -- this class implements the
 * former only, and must not be pointed at TrailFeature.
 */
@Service
public class LegacySlopeService {

    /**
     * Verbatim port of the legacy {@code processGeoJSON()} flatten/filter
     * step. Feature order is preserved (source order, not re-sorted). The
     * course filter is a substring match ({@code PMNTN_NM.includes(courseName)}),
     * not exact equality -- kept as-is since the legacy frontend relies on
     * this to match e.g. courseName "무악동" against PMNTN_NM "무악동구간".
     * No geometry-shape validation is performed (mirrors the legacy code),
     * so degenerate sub-lines are flattened in unchanged, and Feature/Part
     * boundaries are not preserved in the output.
     */
    public List<LegacyCoordinate> flattenCourseCoordinates(JsonNode geoJsonRoot, String courseName) {
        List<LegacyCoordinate> allCoordinates = new ArrayList<>();

        for (JsonNode feature : geoJsonRoot.path("features")) {
            JsonNode pmntnNmNode = feature.path("properties").path("PMNTN_NM");
            if (!pmntnNmNode.isTextual() || !pmntnNmNode.asText().contains(courseName)) {
                continue;
            }

            double dnValue = feature.path("properties").path("DN").asDouble(0);
            JsonNode geometry = feature.path("geometry");
            String geometryType = geometry.path("type").asText();

            if ("MultiLineString".equals(geometryType)) {
                for (JsonNode line : geometry.path("coordinates")) {
                    for (JsonNode coord : line) {
                        allCoordinates.add(toLegacyCoordinate(coord, dnValue));
                    }
                }
            } else if ("LineString".equals(geometryType)) {
                for (JsonNode coord : geometry.path("coordinates")) {
                    allCoordinates.add(toLegacyCoordinate(coord, dnValue));
                }
            }
            // Any other geometry type: legacy processGeoJSON() leaves `coordinates` as [],
            // i.e. contributes nothing -- reproduced here by simply not handling it.
        }

        return allCoordinates;
    }

    private LegacyCoordinate toLegacyCoordinate(JsonNode coord, double dnValue) {
        return new LegacyCoordinate(coord.get(0).asDouble(), coord.get(1).asDouble(), dnValue);
    }

    /**
     * Full legacy pipeline for one course at one groupSize: flatten -> group
     * -> render-guard + slope/color. groupSize is a caller-supplied
     * parameter -- the four legacy views use 5, 7 and 12 respectively, and
     * Phase 7A does not unify them (see docs/01-architecture-and-modeling.md).
     */
    public LegacySlopeResult buildLegacySlopeGroups(JsonNode geoJsonRoot, String courseName, int groupSize) {
        List<LegacyCoordinate> flattened = flattenCourseCoordinates(geoJsonRoot, courseName);
        List<List<LegacyCoordinate>> groups = LegacySlopeCalculator.groupCoordinates(flattened, groupSize);
        List<LegacySlopeGroup> rendered = LegacySlopeCalculator.toRenderedGroups(groups);
        return new LegacySlopeResult(groups.size(), rendered);
    }
}
