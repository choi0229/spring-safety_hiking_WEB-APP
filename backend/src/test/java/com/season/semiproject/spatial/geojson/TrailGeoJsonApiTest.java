package com.season.semiproject.spatial.geojson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MockMvc-level contract test for GET /api/spatial/trails/geojson, run against the real
 * Docker PostgreSQL/PostGIS instance and the actual imported TrailFeature data.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TrailGeoJsonApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsFeatureCollectionWithCorrectContentTypeAndCount() throws Exception {
        mockMvc.perform(get("/api/spatial/trails/geojson"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.features.length()", is(1706)))
                .andExpect(jsonPath("$.features[*].type", everyItem(is("Feature"))))
                .andExpect(jsonPath("$.features[*].geometry.type", everyItem(is("MultiLineString"))))
                .andExpect(jsonPath("$.features[0].properties.PMNTN_NM").exists())
                .andExpect(jsonPath("$.features[0].properties.DN").exists())
                .andExpect(jsonPath("$.features[0].properties.trailId").exists());
    }

    @Test
    void responseBodyIsNotDoubleSerialized() throws Exception {
        // Guards against `"{\"type\":...}"` (a JSON string containing JSON text) instead of an
        // actual JSON object -- would happen if the Mapper's JSON string were passed through
        // Jackson's default object serialization instead of being returned as raw response body.
        MvcResult result = mockMvc.perform(get("/api/spatial/trails/geojson"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertDoesNotThrow(() -> {
            JsonNode root = new ObjectMapper().readTree(body);
            if (!root.isObject() || !root.has("features")) {
                throw new AssertionError("response did not parse as a plain JSON object: " + body.substring(0, 100));
            }
        });
    }
}
