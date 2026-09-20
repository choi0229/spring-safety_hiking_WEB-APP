package com.season.semiproject.spatial.slope;

import org.junit.jupiter.api.BeforeEach;
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
 * the real Docker PostgreSQL/PostGIS instance and the actual imported data. The API now serves
 * precomputed `slope_section` rows (see docs/09-slope-section-analysis.md) -- this class ensures
 * a build has run before asserting on response content, and confirms the Production contract is
 * unchanged from the consumer's point of view (same response shape as compute-on-request) while
 * only windowMeters=20 is accepted now.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SlopeSectionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SlopeSectionBuildService buildService;

    @BeforeEach
    void ensureBuilt() {
        buildService.buildAll();
    }

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
    void windowMeters20IsAccepted() throws Exception {
        mockMvc.perform(get("/api/spatial/trails/13/slope-sections").param("windowMeters", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void windowMeters10And30AreNoLongerAcceptedByTheProductionApi() throws Exception {
        // Persistence deliberately only ever stores window_m=20 (see slope-section-schema.sql,
        // chk_slope_section_window_m). Accepting 10/30 here again would silently reintroduce a
        // mixed-semantics API ("20 -> DB read, 10/30 -> some other computation") that Phase 12E
        // explicitly avoided -- so both must now be rejected the same way any other invalid
        // windowMeters value is.
        for (String window : new String[] { "10", "30" }) {
            mockMvc.perform(get("/api/spatial/trails/13/slope-sections").param("windowMeters", window))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void disallowedWindowValueIsRejectedWithBadRequest() throws Exception {
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
