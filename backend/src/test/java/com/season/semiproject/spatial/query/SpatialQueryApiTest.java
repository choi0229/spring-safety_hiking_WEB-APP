package com.season.semiproject.spatial.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.season.semiproject.spatial.accident.AccidentImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc-level API tests for /api/spatial/... (SpatialQueryController), run against the real
 * Docker PostgreSQL/PostGIS instance and the actual imported accident/trail data. Not wired into
 * any Frontend yet (Phase 9) -- this only verifies the Backend contract.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpatialQueryApiTest {

    private static final String SOURCE_FILE = "2023산악사고_인왕산.geojson";
    private static final String GEOJSON_PATH = "../frontend/public/data/2023산악사고_인왕산.geojson";
    private static final String NEAREST_REPORT_NO = "20231103201R00346";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccidentImportService importService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long accidentId;
    private Long segmentId;
    private Long muakdongTrailId;
    private Long maruTrailId;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(GEOJSON_PATH));
        importService.importAccidents(SOURCE_FILE, root);

        accidentId = jdbcTemplate.queryForObject(
                "SELECT id FROM accident_point WHERE source_file = ? AND report_no = ?",
                Long.class, SOURCE_FILE, NEAREST_REPORT_NO);

        segmentId = jdbcTemplate.queryForObject(
                "SELECT ts.id FROM accident_point a JOIN trail_segment ts "
                        + "ON ST_DWithin(a.geom::geography, ts.geom::geography, 2.0) "
                        + "WHERE a.id = ? ORDER BY ST_Distance(a.geom::geography, ts.geom::geography) LIMIT 1",
                Long.class, accidentId);

        muakdongTrailId = jdbcTemplate.queryForObject(
                "SELECT id FROM trail WHERE source_course_name = ?", Long.class, "무악동구간");
        maruTrailId = jdbcTemplate.queryForObject(
                "SELECT id FROM trail WHERE source_course_name = ?", Long.class, "마루");
    }

    @Test
    void nearbySegments_validRequest_returnsAscendingResultsWithinThreshold() throws Exception {
        mockMvc.perform(get("/api/spatial/accidents/{id}/nearby-segments", accidentId)
                        .param("distanceMeters", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(24)))
                .andExpect(jsonPath("$[0].distanceMeters", lessThanOrEqualTo(30.0)))
                .andExpect(jsonPath("$[0].segmentId", notNullValue()))
                .andExpect(jsonPath("$[0].sourceCourseName", is("무악동구간")));
    }

    @Test
    void nearbySegments_noMatches_returnsEmptyListNotError() throws Exception {
        mockMvc.perform(get("/api/spatial/accidents/{id}/nearby-segments", accidentId)
                        .param("distanceMeters", "0.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void nearbySegments_nonExistentAccident_returns404() throws Exception {
        mockMvc.perform(get("/api/spatial/accidents/{id}/nearby-segments", 999_999_999L)
                        .param("distanceMeters", "30"))
                .andExpect(status().isNotFound());
    }

    @Test
    void nearbySegments_zeroDistance_returns400() throws Exception {
        mockMvc.perform(get("/api/spatial/accidents/{id}/nearby-segments", accidentId)
                        .param("distanceMeters", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbySegments_negativeDistance_returns400() throws Exception {
        mockMvc.perform(get("/api/spatial/accidents/{id}/nearby-segments", accidentId)
                        .param("distanceMeters", "-10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbySegments_missingDistanceParam_returns400() throws Exception {
        mockMvc.perform(get("/api/spatial/accidents/{id}/nearby-segments", accidentId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyAccidents_validRequest_returnsAscendingResultsWithinThreshold() throws Exception {
        mockMvc.perform(get("/api/spatial/trail-segments/{id}/nearby-accidents", segmentId)
                        .param("distanceMeters", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accidentId", is(accidentId.intValue())))
                .andExpect(jsonPath("$[0].reportNo", is(NEAREST_REPORT_NO)))
                .andExpect(jsonPath("$[0].distanceMeters", lessThanOrEqualTo(30.0)));
    }

    @Test
    void nearbyAccidents_nonExistentSegment_returns404() throws Exception {
        mockMvc.perform(get("/api/spatial/trail-segments/{id}/nearby-accidents", 999_999_999L)
                        .param("distanceMeters", "30"))
                .andExpect(status().isNotFound());
    }

    @Test
    void nearbyAccidents_zeroDistance_returns400() throws Exception {
        mockMvc.perform(get("/api/spatial/trail-segments/{id}/nearby-accidents", segmentId)
                        .param("distanceMeters", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyAccidentsForTrail_validRequest_returnsDedupedFeatureCollection() throws Exception {
        // 24 raw TrailSegment matches for this accident (see SpatialQueryIntegrationTest,
        // exhaustiveResultsIncludeMatchesOutsideAnySmallKnnWindow) must collapse to exactly one
        // GeoJSON Feature here (Phase 13 dedup requirement).
        mockMvc.perform(get("/api/spatial/trails/{trailId}/nearby-accidents", muakdongTrailId)
                        .param("distanceMeters", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.trailId", is(muakdongTrailId.intValue())))
                .andExpect(jsonPath("$.features", hasSize(2)))
                .andExpect(jsonPath("$.features[0].type", is("Feature")))
                .andExpect(jsonPath("$.features[0].geometry.type", is("Point")))
                .andExpect(jsonPath("$.features[0].properties.reportNo", is(NEAREST_REPORT_NO)))
                .andExpect(jsonPath("$.features[0].properties.distanceToTrailMeters", closeTo(1.46, 0.01)))
                .andExpect(jsonPath("$.features[0].properties.distanceToTrailMeters",
                        lessThanOrEqualTo(30.0)));
    }

    @Test
    void nearbyAccidentsForTrail_noCandidatesWithinRadius_returnsEmptyFeatureCollectionNotError() throws Exception {
        // 마루 has zero accident candidates at 30m (see
        // SpatialQueryIntegrationTest.trailLevelCandidateCountsMatchTheKnownThirtyMeterBaselineForAllFourTrails).
        mockMvc.perform(get("/api/spatial/trails/{trailId}/nearby-accidents", maruTrailId)
                        .param("distanceMeters", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.features", hasSize(0)));
    }

    @Test
    void nearbyAccidentsForTrail_nonExistentTrail_returns404() throws Exception {
        mockMvc.perform(get("/api/spatial/trails/{trailId}/nearby-accidents", 999_999_999L)
                        .param("distanceMeters", "30"))
                .andExpect(status().isNotFound());
    }

    @Test
    void nearbyAccidentsForTrail_zeroDistance_returns400() throws Exception {
        mockMvc.perform(get("/api/spatial/trails/{trailId}/nearby-accidents", muakdongTrailId)
                        .param("distanceMeters", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyAccidentsForTrail_negativeDistance_returns400() throws Exception {
        mockMvc.perform(get("/api/spatial/trails/{trailId}/nearby-accidents", muakdongTrailId)
                        .param("distanceMeters", "-1"))
                .andExpect(status().isBadRequest());
    }
}
