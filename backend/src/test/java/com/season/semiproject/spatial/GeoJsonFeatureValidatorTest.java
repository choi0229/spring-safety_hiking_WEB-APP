package com.season.semiproject.spatial;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoJsonFeatureValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GeoJsonFeatureValidator validator = new GeoJsonFeatureValidator();

    private JsonNode parse(String json) throws Exception {
        return mapper.readTree(json);
    }

    @Test
    void validFeaturePasses() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"PMNTN_NM":"마루","DN":90},
             "geometry":{"type":"MultiLineString","coordinates":[[[126.96,37.58],[126.97,37.59]]]}}
            """);
        assertTrue(validator.validate(f).isValid());
    }

    @Test
    void rejectsWrongGeometryType() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"PMNTN_NM":"마루","DN":90},
             "geometry":{"type":"LineString","coordinates":[[126.96,37.58],[126.97,37.59]]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsEmptyCoordinates() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"PMNTN_NM":"마루","DN":90},
             "geometry":{"type":"MultiLineString","coordinates":[]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsSubLineWithFewerThanTwoPoints() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"PMNTN_NM":"마루","DN":90},
             "geometry":{"type":"MultiLineString","coordinates":[[[126.96,37.58]]]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsCoordinateWithFewerThanTwoElements() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"PMNTN_NM":"마루","DN":90},
             "geometry":{"type":"MultiLineString","coordinates":[[[126.96],[126.97,37.59]]]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsMissingDn() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"PMNTN_NM":"마루"},
             "geometry":{"type":"MultiLineString","coordinates":[[[126.96,37.58],[126.97,37.59]]]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsNonNumericDn() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"PMNTN_NM":"마루","DN":"not-a-number"},
             "geometry":{"type":"MultiLineString","coordinates":[[[126.96,37.58],[126.97,37.59]]]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsMissingPmntnNm() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"DN":90},
             "geometry":{"type":"MultiLineString","coordinates":[[[126.96,37.58],[126.97,37.59]]]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }

    @Test
    void rejectsCoordinateOutsideKoreaBounds() throws Exception {
        JsonNode f = parse("""
            {"type":"Feature",
             "properties":{"PMNTN_NM":"마루","DN":90},
             "geometry":{"type":"MultiLineString","coordinates":[[[37.58,126.96],[200,300]]]}}
            """);
        assertFalse(validator.validate(f).isValid());
    }
}
