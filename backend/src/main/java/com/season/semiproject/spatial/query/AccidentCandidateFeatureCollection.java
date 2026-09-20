package com.season.semiproject.spatial.query;

import java.util.List;

/**
 * GeoJSON FeatureCollection wrapper for the {@code trails/{trailId}/nearby-accidents} API
 * response ("API C": Trail -> distinct nearby AccidentPoint candidates, see
 * mapper-spatial-query.xml). One Feature per distinct accidentId -- never duplicated across the
 * Trail's TrailSegments (see docs/05-accident-spatial-query.md).
 */
public final class AccidentCandidateFeatureCollection {

    private final String type = "FeatureCollection";
    private final long trailId;
    private final double distanceMeters;
    private final List<AccidentCandidateFeature> features;

    public AccidentCandidateFeatureCollection(long trailId, double distanceMeters, List<AccidentCandidateFeature> features) {
        this.trailId = trailId;
        this.distanceMeters = distanceMeters;
        this.features = features;
    }

    public String getType() {
        return type;
    }

    public long getTrailId() {
        return trailId;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public List<AccidentCandidateFeature> getFeatures() {
        return features;
    }
}
