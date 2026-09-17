package com.season.semiproject.spatial.accident;

import com.fasterxml.jackson.databind.JsonNode;
import com.season.semiproject.spatial.FeatureValidationResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Structural validation for a single Accident GeoJSON Feature, applied before any DB write.
 * Mirrors GeoJsonFeatureValidator's philosophy (see that class) but for Point geometry and the
 * accident-specific property set. No Feature that fails here is silently skipped -- the whole
 * import aborts (see AccidentImportService) since the 42-Feature source file was already
 * confirmed to be entirely valid during Phase 8 analysis; a validation failure here means
 * something about the source file changed and must be looked at, not coerced.
 *
 * Duplicate coordinates across different Features are NOT treated as invalid here -- see
 * accident-schema.sql for why (real distinct accidents legitimately share a coordinate).
 */
public class AccidentFeatureValidator {

    // Rough bounding box for South Korea, used only as a sanity check against gross errors
    // (e.g. swapped lon/lat, unit mistakes) -- not a precise territorial boundary. Same bounds as
    // GeoJsonFeatureValidator; kept as a separate constant here since these two validators check
    // different geometry types and shapes and are not meant to share implementation.
    private static final double MIN_LON = 124.0;
    private static final double MAX_LON = 132.0;
    private static final double MIN_LAT = 33.0;
    private static final double MAX_LAT = 43.0;

    // STRICT so an out-of-range calendar date (e.g. 2023-02-30) is rejected outright instead of
    // being silently clamped to a nearby valid date (the default SMART resolver would resolve
    // "20230230" to 2023-02-28). Pattern uses "uuuu" (year), not "yyyy" (year-of-era) -- under
    // STRICT, "yyyy" requires an explicit Era to resolve and otherwise fails to parse even a
    // genuinely valid date.
    private static final DateTimeFormatter YMD =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

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
        if (geomType == null || !"Point".equals(geomType.asText())) {
            return FeatureValidationResult.invalid("geometry.type is not Point (was: " + geomType + ")");
        }

        JsonNode coordinates = geometry.get("coordinates");
        if (coordinates == null || !coordinates.isArray() || coordinates.size() != 2) {
            return FeatureValidationResult.invalid(
                    "coordinates missing or not exactly 2 elements (was: " + coordinates + ")");
        }

        JsonNode lonNode = coordinates.get(0);
        JsonNode latNode = coordinates.get(1);
        if (!lonNode.isNumber() || !latNode.isNumber()) {
            return FeatureValidationResult.invalid("longitude/latitude is not numeric");
        }
        double lon = lonNode.asDouble();
        double lat = latNode.asDouble();
        if (lon < MIN_LON || lon > MAX_LON || lat < MIN_LAT || lat > MAX_LAT) {
            return FeatureValidationResult.invalid(
                    "coordinate out of expected South Korea bounds: [" + lon + ", " + lat + "]");
        }

        JsonNode properties = feature.get("properties");
        if (properties == null || properties.isNull()) {
            return FeatureValidationResult.invalid("properties missing");
        }

        JsonNode reportNo = properties.get("msfrtn_resc_reprt_no");
        if (reportNo == null || reportNo.isNull() || reportNo.asText().isBlank()) {
            return FeatureValidationResult.invalid("properties.msfrtn_resc_reprt_no is missing/blank");
        }

        JsonNode dspYmd = properties.get("dsp_ymd");
        if (dspYmd == null || dspYmd.isNull()) {
            return FeatureValidationResult.invalid("properties.dsp_ymd is missing");
        }
        try {
            LocalDate.parse(dspYmd.asText(), YMD);
        } catch (DateTimeParseException e) {
            return FeatureValidationResult.invalid(
                    "properties.dsp_ymd is not a valid YYYYMMDD date (was: " + dspYmd.asText() + ")");
        }

        JsonNode accidentType = properties.get("acdnt_cause_asort_nm");
        if (accidentType == null || accidentType.isNull() || accidentType.asText().isBlank()) {
            return FeatureValidationResult.invalid("properties.acdnt_cause_asort_nm is missing/blank");
        }

        JsonNode emdNm = properties.get("emd_nm");
        if (emdNm == null || emdNm.isNull() || emdNm.asText().isBlank()) {
            return FeatureValidationResult.invalid("properties.emd_nm is missing/blank");
        }

        return FeatureValidationResult.valid();
    }
}
