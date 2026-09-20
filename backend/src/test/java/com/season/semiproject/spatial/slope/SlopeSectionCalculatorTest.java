package com.season.semiproject.spatial.slope;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for {@link SlopeSectionCalculator}: fixed-window cutting, the partial-section
 * threshold policy, positive/negative slope sign, and the "never silently fall back to 0%" rule
 * (see docs/09-slope-section-analysis.md).
 */
class SlopeSectionCalculatorTest {

    private static final double DELTA = 1e-6;

    /**
     * Two single-segment Features, each its own sample at its own span's midpoint: Feature 100
     * spans [0, coverageLength] (mid = coverageLength/2), Feature 200 spans
     * [coverageLength, 2*coverageLength] (mid = 1.5*coverageLength) -- so the resulting profile
     * coverage is exactly [0.5*coverageLength, 1.5*coverageLength], length = coverageLength.
     */
    private static NetworkChain twoSampleRamp(double coverageLength, double dnStart, double dnEnd) {
        ChainSegmentUsage a = new ChainSegmentUsage(1, 100, dnStart, 0.0, coverageLength, false, List.of(
                new double[] { 0, 0 }, new double[] { 0, 1 }));
        ChainSegmentUsage b = new ChainSegmentUsage(2, 200, dnEnd, coverageLength, coverageLength * 2, false, List.of(
                new double[] { 0, 0 }, new double[] { 0, 1 }));
        return new NetworkChain(0, List.of(a, b), false);
    }

    @Test
    void fullWindowsPlusPartialAboveThresholdAreKept() {
        // coverageLength=40 -> profile coverage = [20, 60], length 40.
        NetworkChain chain = twoSampleRamp(40.0, 100.0, 140.0);

        List<SlopeSectionResult> sections = SlopeSectionCalculator.computeSections(chain, 10.0);

        // fullSections = floor(40/10) = 4, remainder = 0 -> exactly 4 full sections, no partial.
        assertEquals(4, sections.size());
        assertTrue(sections.stream().noneMatch(SlopeSectionResult::isPartialSection));
        assertEquals(20.0, sections.get(0).getChainStartMeters(), DELTA);
        assertEquals(60.0, sections.get(3).getChainEndMeters(), DELTA);
    }

    @Test
    void remainderAtOrAboveHalfWindowIsKeptAsPartialSection() {
        // Coverage length 25 with window 10: fullSections=2 (20m), remainder=5 = 0.5*window -> kept.
        NetworkChain chain = twoSampleRamp(25.0, 100.0, 150.0);

        List<SlopeSectionResult> sections = SlopeSectionCalculator.computeSections(chain, 10.0);

        assertEquals(3, sections.size());
        assertFalse(sections.get(0).isPartialSection());
        assertFalse(sections.get(1).isPartialSection());
        assertTrue(sections.get(2).isPartialSection());
        assertEquals(5.0, sections.get(2).getDistanceMeters(), DELTA);
    }

    @Test
    void remainderBelowHalfWindowIsDropped() {
        // Coverage length 23 with window 10: fullSections=2 (20m), remainder=3 < 5 -> dropped.
        NetworkChain chain = twoSampleRamp(23.0, 100.0, 150.0);

        List<SlopeSectionResult> sections = SlopeSectionCalculator.computeSections(chain, 10.0);

        assertEquals(2, sections.size());
        assertTrue(sections.stream().noneMatch(SlopeSectionResult::isPartialSection));
    }

    @Test
    void positiveElevationDeltaProducesPositiveSlope() {
        NetworkChain chain = twoSampleRamp(20.0, 100.0, 120.0);
        List<SlopeSectionResult> sections = SlopeSectionCalculator.computeSections(chain, 10.0);
        assertTrue(sections.get(0).getEstimatedSlopePercent() > 0);
    }

    @Test
    void negativeElevationDeltaProducesNegativeSlope() {
        NetworkChain chain = twoSampleRamp(20.0, 120.0, 100.0);
        List<SlopeSectionResult> sections = SlopeSectionCalculator.computeSections(chain, 10.0);
        assertTrue(sections.get(0).getEstimatedSlopePercent() < 0);
    }

    @Test
    void slopeFormulaMatchesPlainPercentGradeNotLegacyDiagonal() {
        // ramp: dn 100 -> 200 over coverage [5,45] (length 40); window 40 -> single section.
        NetworkChain chain = twoSampleRamp(40.0, 100.0, 200.0);
        List<SlopeSectionResult> sections = SlopeSectionCalculator.computeSections(chain, 40.0);
        assertEquals(1, sections.size());
        // elevationDelta=100, distance=40 -> 100/40*100 = 250%, NOT delta/hypot(...)*100.
        assertEquals(250.0, sections.get(0).getEstimatedSlopePercent(), DELTA);
    }

    @Test
    void chainWithFewerThanTwoSamplesProducesNoSections() {
        ChainSegmentUsage single = new ChainSegmentUsage(1, 100, 100.0, 0.0, 10.0, false, List.of(
                new double[] { 0, 0 }, new double[] { 0, 1 }));
        NetworkChain chain = new NetworkChain(0, List.of(single), false);

        List<SlopeSectionResult> sections = SlopeSectionCalculator.computeSections(chain, 10.0);

        assertTrue(sections.isEmpty());
    }
}
