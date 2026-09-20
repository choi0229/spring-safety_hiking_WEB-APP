package com.season.semiproject.spatial.slope;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;
import static com.season.semiproject.spatial.slope.SlopeTestFixtures.segment;

/**
 * Pure unit tests for {@link ElevationProfile}: DN sample placement/dedup, linear interpolation,
 * and the "no extrapolation" policy (see docs/09-slope-section-analysis.md).
 */
class ElevationProfileTest {

    private static final double DELTA = 1e-6;

    @Test
    void twoFeaturesProduceTwoSamplesWithLinearInterpolationBetween() {
        // One chain, two single-segment Features: [0,10) dn=100, [10,25) dn=110.
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 1, 2, 10.0, 100, 100, 0, 0, 0, 0.0001),
                segment(2, 2, 3, 15.0, 101, 110, 0, 0.0001, 0, 0.00025));
        NetworkChain chain = NetworkChainBuilder.buildChains(rows).get(0);

        ElevationProfile profile = ElevationProfile.build(chain);

        assertTrue(profile.hasCoverage());
        assertEquals(2, profile.getSampleCount());
        assertEquals(5.0, profile.getCoverageStartMeters(), DELTA); // feature 100 midpoint: (0+10)/2
        assertEquals(17.5, profile.getCoverageEndMeters(), DELTA); // feature 101 midpoint: (10+25)/2

        assertEquals(100.0, profile.elevationAt(5.0).getAsDouble(), DELTA);
        assertEquals(110.0, profile.elevationAt(17.5).getAsDouble(), DELTA);
        double mid = profile.elevationAt(11.25).getAsDouble(); // exact midpoint between samples
        assertEquals(105.0, mid, DELTA);
    }

    @Test
    void noExtrapolationOutsideSampleCoverage() {
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 1, 2, 10.0, 100, 100, 0, 0, 0, 0.0001),
                segment(2, 2, 3, 10.0, 101, 110, 0, 0.0001, 0, 0.0002));
        NetworkChain chain = NetworkChainBuilder.buildChains(rows).get(0);
        ElevationProfile profile = ElevationProfile.build(chain);

        assertTrue(profile.elevationAt(profile.getCoverageStartMeters() - 1.0).isEmpty());
        assertTrue(profile.elevationAt(profile.getCoverageEndMeters() + 1.0).isEmpty());
    }

    @Test
    void oneSampleOnlyMeansNoCoverage() {
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 1, 2, 10.0, 100, 100, 0, 0, 0, 0.0001));
        NetworkChain chain = NetworkChainBuilder.buildChains(rows).get(0);
        ElevationProfile profile = ElevationProfile.build(chain);

        assertFalse(profile.hasCoverage());
        assertTrue(profile.elevationAt(5.0).isEmpty());
    }

    @Test
    void multiSegmentFeatureProducesExactlyOneDeduplicatedSample() {
        // Feature 100 is split into 2 Segments (like 256/1706 real Features) -- must yield ONE
        // sample at the length-weighted midpoint of the whole feature span, not two.
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 1, 2, 5.0, 100, 90, 0, 0, 0, 0.00005),
                segment(2, 2, 3, 5.0, 100, 90, 0, 0.00005, 0, 0.00010), // same feature id 100
                segment(3, 3, 4, 5.0, 101, 120, 0, 0.00010, 0, 0.00015));
        NetworkChain chain = NetworkChainBuilder.buildChains(rows).get(0);

        ElevationProfile profile = ElevationProfile.build(chain);

        assertEquals(2, profile.getSampleCount()); // feature 100 (deduped) + feature 101
        assertEquals(5.0, profile.getCoverageStartMeters(), DELTA); // feature100 span [0,10] -> mid 5
        assertEquals(12.5, profile.getCoverageEndMeters(), DELTA); // feature101 span [10,15] -> mid 12.5
    }

    @Test
    void samplesAtIdenticalDistanceAreMergedByAveragingDn() {
        ChainSegmentUsage a = new ChainSegmentUsage(1, 100, 100.0, 0.0, 4.0, false, List.of(
                new double[] { 0, 0 }, new double[] { 0, 1 }));
        ChainSegmentUsage b = new ChainSegmentUsage(2, 200, 110.0, 0.0, 4.0, false, List.of(
                new double[] { 0, 0 }, new double[] { 0, 1 }));
        ChainSegmentUsage c = new ChainSegmentUsage(3, 300, 200.0, 8.0, 12.0, false, List.of(
                new double[] { 0, 0 }, new double[] { 0, 1 }));
        NetworkChain chain = new NetworkChain(0, List.of(a, b, c), false);

        ElevationProfile profile = ElevationProfile.build(chain);

        assertEquals(2, profile.getSampleCount());
        OptionalDouble atTwo = profile.elevationAt(2.0);
        assertTrue(atTwo.isPresent());
        assertEquals(105.0, atTwo.getAsDouble(), DELTA); // average of 100 and 110
        assertEquals(200.0, profile.elevationAt(10.0).getAsDouble(), DELTA);
    }
}
