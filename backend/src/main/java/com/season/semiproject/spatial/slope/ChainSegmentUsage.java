package com.season.semiproject.spatial.slope;

import java.util.List;

/**
 * One TrailSegment as it appears inside a single {@link NetworkChain}'s traversal order.
 *
 * {@code cumulativeStartMeters}/{@code cumulativeEndMeters} are positions on the chain's own
 * 0..chainLength distance axis (always {@code cumulativeStartMeters < cumulativeEndMeters},
 * regardless of the Segment's own stored from_node/to_node direction). {@code reversed} records
 * whether this chain traverses the Segment opposite to its stored from_node -> to_node direction --
 * {@code orientedCoordinates} is already flipped to match traversal direction, so callers never
 * need to re-check {@code reversed} to use the coordinates correctly.
 */
public final class ChainSegmentUsage {

    private final long segmentId;
    private final long sourceTrailFeatureId;
    private final double sourceDn;
    private final double cumulativeStartMeters;
    private final double cumulativeEndMeters;
    private final boolean reversed;
    private final List<double[]> orientedCoordinates;

    public ChainSegmentUsage(long segmentId, long sourceTrailFeatureId, double sourceDn,
            double cumulativeStartMeters, double cumulativeEndMeters, boolean reversed,
            List<double[]> orientedCoordinates) {
        this.segmentId = segmentId;
        this.sourceTrailFeatureId = sourceTrailFeatureId;
        this.sourceDn = sourceDn;
        this.cumulativeStartMeters = cumulativeStartMeters;
        this.cumulativeEndMeters = cumulativeEndMeters;
        this.reversed = reversed;
        this.orientedCoordinates = orientedCoordinates;
    }

    public long getSegmentId() {
        return segmentId;
    }

    public long getSourceTrailFeatureId() {
        return sourceTrailFeatureId;
    }

    public double getSourceDn() {
        return sourceDn;
    }

    public double getCumulativeStartMeters() {
        return cumulativeStartMeters;
    }

    public double getCumulativeEndMeters() {
        return cumulativeEndMeters;
    }

    public boolean isReversed() {
        return reversed;
    }

    public List<double[]> getOrientedCoordinates() {
        return orientedCoordinates;
    }
}
