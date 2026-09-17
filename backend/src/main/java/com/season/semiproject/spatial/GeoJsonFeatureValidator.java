package com.season.semiproject.spatial;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Structural validation for a single GeoJSON Feature, applied before any Trail/TrailFeature
 * grouping decision is made. This only checks shape/type correctness -- it does not decide
 * whether a Feature belongs to a target Trail (that is a separate, manifest-driven decision
 * in TrailImportService). Coordinates are always treated as GeoJSON-standard
 * [longitude, latitude] order -- never assumed from expected numeric ranges.
 *
 * No Feature that fails here is silently coerced (empty geometry, 0, default DN, etc.) --
 * callers must either abort the whole import or match the failure against a manifest-declared
 * INVALID_GEOMETRY_SOURCE_FEATURE exclusion (see TrailImportManifest).
 */
public class GeoJsonFeatureValidator {

    // Rough bounding box for South Korea, used only as a sanity check against gross errors
    // (e.g. swapped lon/lat, unit mistakes) -- not a precise territorial boundary.
    private static final double MIN_LON = 124.0;
    private static final double MAX_LON = 132.0;
    private static final double MIN_LAT = 33.0;
    private static final double MAX_LAT = 43.0;

    public FeatureValidationResult validate(JsonNode feature) {
        JsonNode typeNode = feature.get("type");
        if (typeNode == null || !"Feature".equals(typeNode.asText())) {
            return FeatureValidationResult.invalid("root type is not 'Feature'");
        }

        JsonNode geometry = feature.get("geometry");
        if (geometry == null || geometry.isNull()) {
            return FeatureValidationResult.invalid("geometry is missing");
        }

        JsonNode geomType = geometry.get("type");
        if (geomType == null || !"MultiLineString".equals(geomType.asText())) {
            return FeatureValidationResult.invalid(
                    "geometry.type is not MultiLineString (was: " + geomType + ")");
        }

        JsonNode coordinates = geometry.get("coordinates");
        if (coordinates == null || !coordinates.isArray() || coordinates.isEmpty()) {
            return FeatureValidationResult.invalid("coordinates missing or empty");
        }

        for (JsonNode line : coordinates) {
            if (!line.isArray() || line.size() < 2) {
                return FeatureValidationResult.invalid(
                        "a sub-line has fewer than 2 coordinates (size=" + line.size() + ")");
            }
            for (JsonNode point : line) {
                if (!point.isArray() || point.size() < 2) {
                    return FeatureValidationResult.invalid(
                            "a coordinate has fewer than 2 elements");
                }
                JsonNode lonNode = point.get(0);
                JsonNode latNode = point.get(1);
                if (!lonNode.isNumber() || !latNode.isNumber()) {
                    return FeatureValidationResult.invalid(
                            "a coordinate's longitude/latitude is not numeric");
                }
                double lon = lonNode.asDouble();
                double lat = latNode.asDouble();
                if (lon < MIN_LON || lon > MAX_LON || lat < MIN_LAT || lat > MAX_LAT) {
                    return FeatureValidationResult.invalid(
                            "coordinate out of expected South Korea bounds: [" + lon + ", " + lat + "]");
                }
            }
        }

        JsonNode properties = feature.get("properties");
        if (properties == null || properties.isNull()) {
            return FeatureValidationResult.invalid("properties missing");
        }

        JsonNode dn = properties.get("DN");
        if (dn == null || dn.isNull()) {
            return FeatureValidationResult.invalid("properties.DN is missing");
        }
        if (!dn.isNumber()) {
            return FeatureValidationResult.invalid("properties.DN is not numeric (was: " + dn + ")");
        }

        JsonNode pmntnNm = properties.get("PMNTN_NM");
        if (pmntnNm == null || pmntnNm.isNull()) {
            return FeatureValidationResult.invalid("properties.PMNTN_NM is missing");
        }

        return FeatureValidationResult.valid();
    }
}
