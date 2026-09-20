package com.season.semiproject.spatial.slope;

import java.util.List;

/**
 * A maximal branch-free walk through the TrailSegment/TrailNode graph of one Trail: a sequence of
 * Segments joined end-to-end with no other Segment branching off in between, from one boundary
 * (degree != 2, i.e. an endpoint or a junction) to another -- or, for a Segment set with no
 * boundary node at all (a pure cycle; not present in the current dataset, see
 * docs/09-slope-section-analysis.md), the whole cycle walked once starting from an arbitrary but
 * deterministic point.
 *
 * This is the analysis unit SlopeSection windows are cut from -- NOT the same thing as a
 * TrailSegment (Network topology/geometry unit) or a TrailFeature (Raw provenance unit).
 */
public final class NetworkChain {

    private final int chainIndex;
    private final List<ChainSegmentUsage> segments;
    private final double totalLengthMeters;
    private final boolean closed;

    public NetworkChain(int chainIndex, List<ChainSegmentUsage> segments, boolean closed) {
        this.chainIndex = chainIndex;
        this.segments = segments;
        this.closed = closed;
        this.totalLengthMeters = segments.isEmpty() ? 0
                : segments.get(segments.size() - 1).getCumulativeEndMeters();
    }

    public int getChainIndex() {
        return chainIndex;
    }

    public List<ChainSegmentUsage> getSegments() {
        return segments;
    }

    public double getTotalLengthMeters() {
        return totalLengthMeters;
    }

    public boolean isClosed() {
        return closed;
    }
}
