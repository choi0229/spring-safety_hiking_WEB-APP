package com.season.semiproject.spatial.slope;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Cuts one {@link NetworkChain}'s {@link ElevationProfile} coverage range into non-overlapping
 * fixed-distance SlopeSections (10/20/30m -- see docs/09-slope-section-analysis.md).
 *
 * Windowing starts at the profile's coverage start (the first DN sample), not at chain distance 0
 * -- no SlopeSection is ever generated outside sample coverage (no extrapolation, Phase 12A
 * section 13). The final remainder is kept as one PARTIAL_SECTION only if it retains at least
 * {@link #PARTIAL_MIN_FRACTION} of a full window; otherwise it is dropped -- this ties the
 * cutoff to the window size itself rather than an unrelated fixed meter value (see docs, Phase
 * 12B section "partial policy" for the empirical comparison this threshold was chosen from).
 */
public final class SlopeSectionCalculator {

    public static final double PARTIAL_MIN_FRACTION = 0.5;

    private SlopeSectionCalculator() {
    }

    public static List<SlopeSectionResult> computeSections(NetworkChain chain, double windowMeters) {
        List<SlopeSectionResult> results = new ArrayList<>();
        ElevationProfile profile = ElevationProfile.build(chain);
        if (!profile.hasCoverage()) {
            return results;
        }

        double coverageStart = profile.getCoverageStartMeters();
        double coverageEnd = profile.getCoverageEndMeters();
        double coverageLength = coverageEnd - coverageStart;
        if (coverageLength <= 0) {
            return results;
        }

        int fullSections = (int) Math.floor(coverageLength / windowMeters);
        int sectionIndex = 0;
        for (int i = 0; i < fullSections; i++) {
            double start = coverageStart + i * windowMeters;
            double end = start + windowMeters;
            addSectionIfComputable(results, chain, profile, sectionIndex++, start, end, false);
        }

        double remainder = coverageLength - fullSections * windowMeters;
        if (remainder >= windowMeters * PARTIAL_MIN_FRACTION) {
            double start = coverageStart + fullSections * windowMeters;
            addSectionIfComputable(results, chain, profile, sectionIndex, start, coverageEnd, true);
        }

        return results;
    }

    private static void addSectionIfComputable(List<SlopeSectionResult> results, NetworkChain chain,
            ElevationProfile profile, int sectionIndex, double start, double end, boolean partial) {
        OptionalDouble elevStart = profile.elevationAt(start);
        OptionalDouble elevEnd = profile.elevationAt(end);
        if (elevStart.isEmpty() || elevEnd.isEmpty() || end - start <= 0) {
            // Never a silent slope=0 fallback -- an uncomputable section is simply omitted.
            return;
        }
        results.add(new SlopeSectionResult(chain.getChainIndex(), sectionIndex, start, end,
                elevStart.getAsDouble(), elevEnd.getAsDouble(), partial));
    }
}
