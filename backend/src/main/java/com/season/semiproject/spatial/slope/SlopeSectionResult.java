package com.season.semiproject.spatial.slope;

/**
 * One computed fixed-distance SlopeSection, before its cut geometry has been fetched from
 * PostGIS (see SlopeSectionService). Never carries a synthetic 0% fallback -- if a section
 * cannot be computed it is simply not produced (see SlopeSectionCalculator).
 */
public final class SlopeSectionResult {

    private final int chainIndex;
    private final int sectionIndex;
    private final double chainStartMeters;
    private final double chainEndMeters;
    private final double distanceMeters;
    private final double estimatedElevationStart;
    private final double estimatedElevationEnd;
    private final double estimatedElevationDelta;
    private final double estimatedSlopePercent;
    private final boolean partialSection;

    public SlopeSectionResult(int chainIndex, int sectionIndex, double chainStartMeters, double chainEndMeters,
            double estimatedElevationStart, double estimatedElevationEnd, boolean partialSection) {
        this.chainIndex = chainIndex;
        this.sectionIndex = sectionIndex;
        this.chainStartMeters = chainStartMeters;
        this.chainEndMeters = chainEndMeters;
        this.distanceMeters = chainEndMeters - chainStartMeters;
        this.estimatedElevationStart = estimatedElevationStart;
        this.estimatedElevationEnd = estimatedElevationEnd;
        this.estimatedElevationDelta = estimatedElevationEnd - estimatedElevationStart;
        this.estimatedSlopePercent = (this.estimatedElevationDelta / this.distanceMeters) * 100.0;
        this.partialSection = partialSection;
    }

    public int getChainIndex() {
        return chainIndex;
    }

    public int getSectionIndex() {
        return sectionIndex;
    }

    public double getChainStartMeters() {
        return chainStartMeters;
    }

    public double getChainEndMeters() {
        return chainEndMeters;
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

    public boolean isPartialSection() {
        return partialSection;
    }
}
