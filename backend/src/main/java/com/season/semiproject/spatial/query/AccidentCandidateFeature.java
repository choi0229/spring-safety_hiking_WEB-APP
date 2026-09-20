package com.season.semiproject.spatial.query;

import com.fasterxml.jackson.databind.JsonNode;

/** One GeoJSON Feature (Point) in the {@code trails/{trailId}/nearby-accidents} API response. */
public final class AccidentCandidateFeature {

    private final String type = "Feature";
    private final AccidentCandidateProperties properties;
    private final JsonNode geometry;

    public AccidentCandidateFeature(AccidentCandidateProperties properties, JsonNode geometry) {
        this.properties = properties;
        this.geometry = geometry;
    }

    public String getType() {
        return type;
    }

    public AccidentCandidateProperties getProperties() {
        return properties;
    }

    public JsonNode getGeometry() {
        return geometry;
    }
}
