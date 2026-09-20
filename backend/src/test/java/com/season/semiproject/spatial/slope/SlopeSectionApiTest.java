package com.season.semiproject.spatial.slope;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc-level contract test for GET /api/spatial/trails/{trailId}/slope-sections, run against
 * the real Docker PostgreSQL/PostGIS instance and the actual imported data (Phase 12B).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SlopeSectionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validWindowReturnsFeatureCollectionWithExpectedShape() throws Exception {
        mockMvc.perform(get("/api/spatial/trails/13/slope-sections").param("windowMeters", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.trailId", is(13)))
                .andExpect(jsonPath("$.windowMeters", is(20)))
                .andExpect(jsonPath("$.estimatedElevationSource").exists())
                .andExpect(jsonPath("$.features.length()", greaterThan(0)))
                .andExpect(jsonPath("$.features[*].type", everyItem(is("Feature"))))
                .andExpect(jsonPath("$.features[*].geometry.type", everyItem(is("LineString"))))
                .andExpect(jsonPath("$.features[0].properties.estimatedSlopePercent").exists())
                .andExpect(jsonPath("$.features[0].properties.estimatedElevationStart").exists())
                .andExpect(jsonPath("$.features[0].properties.estimatedElevationEnd").exists())
                .andExpect(jsonPath("$.features[0].properties.distanceMeters").exists());
    }

    @Test
    void allThreeAllowedWindowsAreAccepted() throws Exception {
        for (String window : new String[] { "10", "20", "30" }) {
            mockMvc.perform(get("/api/spatial/trails/13/slope-sections").param("windowMeters", window))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void disallowedWindowValueIsRejectedWithBadRequest() throws Exception {
        // Phase 12A found the short-baseline noise problem at ~1-5m; the API deliberately does
        // not accept arbitrary windowMeters to avoid reintroducing it (see docs/09).
        mockMvc.perform(get("/api/spatial/trails/13/slope-sections").param("windowMeters", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonExistentTrailReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/spatial/trails/999999999/slope-sections").param("windowMeters", "20"))
                .andExpect(status().isNotFound());
    }

    @Test
    void partialSectionsCarryTheDataQualityFlagWhenPresent() throws Exception {
        mockMvc.perform(get("/api/spatial/trails/13/slope-sections").param("windowMeters", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features[*].properties.dataQualityFlag",
                        everyItem(oneOf(null, "PARTIAL_SECTION"))));
    }
}
