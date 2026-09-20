package com.season.semiproject.spatial.slope;

import java.util.List;

/**
 * Locates which {@link ChainSegmentUsage} a chain-relative distance falls into, plus the local
 * fraction inside that Segment's own span -- used by {@link SlopeSectionService} to cut a
 * SlopeSection's geometry per-Segment (Phase 12C) instead of via a single global fraction over
 * the whole merged chain (Phase 12B).
 *
 * Phase 12B cut the whole chain's WKT with one {@code ST_LineSubstring(chainGeom, globalFraction)}
 * call. {@code ST_LineSubstring} on a {@code geometry} (not {@code geography}) input measures
 * fraction using the PLANAR (degree-based) length of the input line, while
 * {@code distanceMeters}/window boundaries are computed from GEOGRAPHY length accumulated per
 * Segment -- these two length measures are not exactly proportional along a long chain that
 * spans a changing latitude, so the returned geometry's actual length could differ from the
 * declared {@code distanceMeters} by several percent (measured up to ~14% on the real dataset,
 * see docs/09-slope-section-analysis.md). Cutting one short (median ~3m) Segment at a time keeps
 * the planar/geography mismatch negligible within each individual cut, since a short, nearly
 * straight Segment's local degree-per-meter ratio barely changes across its own length.
 *
 * At an exact boundary between two Segments, a "start" point resolves to the NEXT Segment
 * (fraction 0) and an "end" point resolves to the PREVIOUS Segment (fraction 1) -- this guarantees
 * a section's start/end never produces a degenerate zero-length cut piece.
 */
public final class ChainDistanceLocator {

    private static final double EPS = 1e-6;

    private ChainDistanceLocator() {
    }

    public record LocatedPoint(int usageIndex, double localFraction) {
    }

    public static LocatedPoint locateStart(NetworkChain chain, double distanceMeters) {
        List<ChainSegmentUsage> segments = chain.getSegments();
        for (int i = 0; i < segments.size(); i++) {
            ChainSegmentUsage usage = segments.get(i);
            if (distanceMeters < usage.getCumulativeEndMeters() - EPS) {
                return new LocatedPoint(i, clamp01(localFraction(usage, distanceMeters)));
            }
        }
        return new LocatedPoint(segments.size() - 1, 1.0);
    }

    public static LocatedPoint locateEnd(NetworkChain chain, double distanceMeters) {
        List<ChainSegmentUsage> segments = chain.getSegments();
        int endIndex = -1;
        for (int i = 0; i < segments.size(); i++) {
            ChainSegmentUsage usage = segments.get(i);
            if (distanceMeters > usage.getCumulativeStartMeters() + EPS) {
                endIndex = i;
            } else {
                break;
            }
        }
        if (endIndex == -1) {
            endIndex = 0;
        }
        return new LocatedPoint(endIndex, clamp01(localFraction(segments.get(endIndex), distanceMeters)));
    }

    private static double localFraction(ChainSegmentUsage usage, double distanceMeters) {
        double length = usage.getCumulativeEndMeters() - usage.getCumulativeStartMeters();
        if (length <= EPS) {
            return 0.0;
        }
        return (distanceMeters - usage.getCumulativeStartMeters()) / length;
    }

    private static double clamp01(double fraction) {
        return Math.max(0.0, Math.min(1.0, fraction));
    }
}
