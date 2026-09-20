package com.season.semiproject.spatial.slope;

import java.util.List;

/** GeoJSON FeatureCollection wrapper for the {@code slope-sections} API response. */
public final class SlopeSectionFeatureCollection {

    private final String type = "FeatureCollection";
    private final long trailId;
    private final int windowMeters;
    /** Explicit provenance note carried on every response -- see docs/09-slope-section-analysis.md. */
    private final String estimatedElevationSource =
            "GeoJSON properties.DN (unverified DEM-derived representative elevation, not a measured elevation)";
    private final List<SlopeSectionFeature> features;

    public SlopeSectionFeatureCollection(long trailId, int windowMeters, List<SlopeSectionFeature> features) {
        this.trailId = trailId;
        this.windowMeters = windowMeters;
        this.features = features;
    }

    public String getType() {
        return type;
    }

    public long getTrailId() {
        return trailId;
    }

    public int getWindowMeters() {
        return windowMeters;
    }

    public String getEstimatedElevationSource() {
        return estimatedElevationSource;
    }

    public List<SlopeSectionFeature> getFeatures() {
        return features;
    }
}
