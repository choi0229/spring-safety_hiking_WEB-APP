package com.season.semiproject.spatial.accident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccidentFeatureValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AccidentFeatureValidator validator = new AccidentFeatureValidator();

    private JsonNode parse(String json) throws Exception {
        return mapper.readTree(json);
    }

    private static final String VALID_PROPERTIES = """
            "msfrtn_resc_reprt_no":"20231103201R00037","dsp_ymd":20230115,
             "acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동"
            """;

    @Test
    void validPointFeaturePasses() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{%s},
             "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
            """.formatted(VALID_PROPERTIES));
        assertTrue(validator.validate(f).isValid());
    }

    @Test
    void rejectsWrongGeometryType() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{%s},
             "geometry":{"type":"MultiPoint","coordinates":[[126.96,37.58]]}}
            """.formatted(VALID_PROPERTIES));
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsNullGeometry() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{%s},
             "geometry":null}
            """.formatted(VALID_PROPERTIES));
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsCoordinateDimensionError() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{%s},
             "geometry":{"type":"Point","coordinates":[126.96]}}
            """.formatted(VALID_PROPERTIES));
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsNonNumericLongitude() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{%s},
             "geometry":{"type":"Point","coordinates":["not-a-number",37.58]}}
            """.formatted(VALID_PROPERTIES));
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsNonNumericLatitude() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{%s},
             "geometry":{"type":"Point","coordinates":[126.96,"not-a-number"]}}
            """.formatted(VALID_PROPERTIES));
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsCoordinateOutsideKoreaBounds() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{%s},
             "geometry":{"type":"Point","coordinates":[200,300]}}
            """.formatted(VALID_PROPERTIES));
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsMissingReportNo() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"dsp_ymd":20230115,"acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동"},
             "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsMissingDspYmd() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"msfrtn_resc_reprt_no":"20231103201R00037","acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동"},
             "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsInvalidCalendarDate() throws Exception {
        // 2023-02-30 does not exist
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"msfrtn_resc_reprt_no":"20231103201R00037","dsp_ymd":20230230,
                            "acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동"},
             "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsMissingAccidentType() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"msfrtn_resc_reprt_no":"20231103201R00037","dsp_ymd":20230115,"emd_nm":"부암동"},
             "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsMissingLocationName() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"msfrtn_resc_reprt_no":"20231103201R00037","dsp_ymd":20230115,"acdnt_cause_asort_nm":"사고부상"},
             "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void duplicateCoordinatesAcrossDifferentFeaturesAreBothValid() throws Exception {
        // Real accidents legitimately share exact coordinates (see accident-schema.sql) --
        // the validator must not reject a Feature just because its coordinate matches another.
        JsonNode f1 = parse("""
            {"type":"Feature",
             "properties":{"msfrtn_resc_reprt_no":"REPORT-A","dsp_ymd":20230115,
                            "acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동"},
             "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
            """);
        JsonNode f2 = parse("""
            {"type":"Feature",
             "properties":{"msfrtn_resc_reprt_no":"REPORT-B","dsp_ymd":20230116,
                            "acdnt_cause_asort_nm":"질환","emd_nm":"홍제동"},
             "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
            """);
        assertTrue(validator.validate(f1).isValid());
        assertTrue(validator.validate(f2).isValid());
    }
}
