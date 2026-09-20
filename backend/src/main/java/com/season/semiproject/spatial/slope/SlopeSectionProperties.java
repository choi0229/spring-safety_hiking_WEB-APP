package com.season.semiproject.spatial.slope;

/**
 * GeoJSON Feature properties for one SlopeSection. Naming intentionally avoids
 * "actualElevation"/"measuredElevation"/"exactSlope" -- every value here is derived from the
 * source GeoJSON's unverified {@code DN} property (see docs/09-slope-section-analysis.md, "DN
 * provenance"), never a verified physical measurement.
 */
public final class SlopeSectionProperties {

    private final int chainIndex;
    private final int sectionIndex;
    private final double distanceMeters;
    private final double estimatedElevationStart;
    private final double estimatedElevationEnd;
    private final double estimatedElevationDelta;
    private final double estimatedSlopePercent;
    /** Null when the section is a full-length window; {@code "PARTIAL_SECTION"} otherwise. */
    private final String dataQualityFlag;

    public SlopeSectionProperties(int chainIndex, int sectionIndex, double distanceMeters,
            double estimatedElevationStart, double estimatedElevationEnd, double estimatedElevationDelta,
            double estimatedSlopePercent, String dataQualityFlag) {
        this.chainIndex = chainIndex;
        this.sectionIndex = sectionIndex;
        this.distanceMeters = distanceMeters;
        this.estimatedElevationStart = estimatedElevationStart;
        this.estimatedElevationEnd = estimatedElevationEnd;
        this.estimatedElevationDelta = estimatedElevationDelta;
        this.estimatedSlopePercent = estimatedSlopePercent;
        this.dataQualityFlag = dataQualityFlag;
    }

    public int getChainIndex() {
        return chainIndex;
    }

    public int getSectionIndex() {
        return sectionIndex;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public double getEstimatedElevationStart() {
        return estimatedElevationStart;
    }

    public double getEstimatedElevationEnd() {
        return estimatedElevationEnd;
    }

    public double getEstimatedElevationDelta() {
        return estimatedElevationDelta;
    }

    public double getEstimatedSlopePercent() {
        return estimatedSlopePercent;
    }

    public String getDataQualityFlag() {
        return dataQualityFlag;
    }
}
