package com.season.semiproject.spatial.legacy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for the ported legacy algorithm core -- no GeoJSON, no
 * Spring context, no database. Real-data regression/parity is covered
 * separately by LegacySlopeServiceParityTest.
 */
class LegacySlopeCalculatorTest {

    private static final double DELTA = 1e-9;

    // -- haversineMeters --------------------------------------------------

    @Test
    void haversineIsZeroForIdenticalPoints() {
        LegacyCoordinate a = new LegacyCoordinate(126.9, 37.5, 100);
        assertEquals(0.0, LegacySlopeCalculator.haversineMeters(a, a), DELTA);
    }

    @Test
    void haversineIsSymmetric() {
        LegacyCoordinate a = new LegacyCoordinate(126.9500, 37.5800, 100);
        LegacyCoordinate b = new LegacyCoordinate(126.9600, 37.5900, 150);
        double ab = LegacySlopeCalculator.haversineMeters(a, b);
        double ba = LegacySlopeCalculator.haversineMeters(b, a);
        assertEquals(ab, ba, DELTA);
        assertTrue(ab > 0);
    }

    // -- calculateLegacySlope ----------------------------------------------

    @Test
    void positiveDnDeltaProducesPositiveSlope() {
        LegacyCoordinate start = new LegacyCoordinate(126.9700, 37.5800, 100);
        LegacyCoordinate end = new LegacyCoordinate(126.9705, 37.5805, 150);
        double slope = LegacySlopeCalculator.calculateLegacySlope(start, end);
        assertTrue(slope > 0, "uphill DN delta must yield positive legacySlopeValue");
    }

    @Test
    void negativeDnDeltaProducesNegativeSlope() {
        LegacyCoordinate start = new LegacyCoordinate(126.9700, 37.5800, 150);
        LegacyCoordinate end = new LegacyCoordinate(126.9705, 37.5805, 100);
        double slope = LegacySlopeCalculator.calculateLegacySlope(start, end);
        assertTrue(slope < 0, "downhill DN delta must yield negative legacySlopeValue");
    }

    @Test
    void zeroDnDeltaProducesZeroSlope() {
        LegacyCoordinate start = new LegacyCoordinate(126.9700, 37.5800, 100);
        LegacyCoordinate end = new LegacyCoordinate(126.9705, 37.5805, 100);
        double slope = LegacySlopeCalculator.calculateLegacySlope(start, end);
        assertEquals(0.0, slope, DELTA);
    }

    @Test
    void horizontalDistanceUnderOneMeterForcesZeroRegardlessOfDnDelta() {
        // Same lng/lat -> horizontalDistance == 0 < 1m guard, even with a large DN delta.
        LegacyCoordinate start = new LegacyCoordinate(126.9700, 37.5800, 0);
        LegacyCoordinate end = new LegacyCoordinate(126.9700, 37.5800, 500);
        double slope = LegacySlopeCalculator.calculateLegacySlope(start, end);
        assertEquals(0.0, slope, DELTA);
    }

    // -- colorForLegacySlope threshold boundaries ---------------------------

    @Test
    void slopeExactlyThirtyIsNotSteepUp() {
        assertEquals("#32CD32", LegacySlopeCalculator.colorForLegacySlope(30.0));
    }

    @Test
    void slopeJustAboveThirtyIsSteepUp() {
        assertEquals("#FF4500", LegacySlopeCalculator.colorForLegacySlope(30.0000001));
    }

    @Test
    void slopeExactlyMinusFifteenIsNotSteepDown() {
        assertEquals("#32CD32", LegacySlopeCalculator.colorForLegacySlope(-15.0));
    }

    @Test
    void slopeJustBelowMinusFifteenIsSteepDown() {
        assertEquals("#1E90FF", LegacySlopeCalculator.colorForLegacySlope(-15.0000001));
    }

    @Test
    void zeroSlopeIsFlat() {
        assertEquals("#32CD32", LegacySlopeCalculator.colorForLegacySlope(0.0));
    }

    // -- groupCoordinates ----------------------------------------------------

    private static List<LegacyCoordinate> coords(int n) {
        List<LegacyCoordinate> list = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new LegacyCoordinate(i, i, i));
        }
        return list;
    }

    @Test
    void groupCoordinatesSplitsEvenlyWhenDivisible() {
        List<List<LegacyCoordinate>> groups = LegacySlopeCalculator.groupCoordinates(coords(10), 5);
        assertEquals(2, groups.size());
        assertEquals(5, groups.get(0).size());
        assertEquals(5, groups.get(1).size());
    }

    @Test
    void groupCoordinatesKeepsSmallerRemainderGroup() {
        List<List<LegacyCoordinate>> groups = LegacySlopeCalculator.groupCoordinates(coords(12), 5);
        assertEquals(3, groups.size());
        assertEquals(5, groups.get(0).size());
        assertEquals(5, groups.get(1).size());
        assertEquals(2, groups.get(2).size());
    }

    @Test
    void groupCoordinatesProducesOnePointRemainderGroupRatherThanDroppingIt() {
        List<List<LegacyCoordinate>> groups = LegacySlopeCalculator.groupCoordinates(coords(11), 5);
        assertEquals(3, groups.size());
        assertEquals(1, groups.get(2).size());
    }

    @Test
    void groupCoordinatesRejectsNonPositiveGroupSize() {
        assertThrows(IllegalArgumentException.class,
                () -> LegacySlopeCalculator.groupCoordinates(coords(5), 0));
    }

    // -- toRenderedGroups: the group.length > 1 render guard -----------------

    @Test
    void onePointRemainderGroupIsExcludedFromRenderedGroupsNotZeroed() {
        List<List<LegacyCoordinate>> groups = LegacySlopeCalculator.groupCoordinates(coords(11), 5);
        List<LegacySlopeGroup> rendered = LegacySlopeCalculator.toRenderedGroups(groups);

        assertEquals(3, groups.size(), "raw groupCoordinates output keeps the 1-point remainder");
        assertEquals(2, rendered.size(), "render guard drops the 1-point remainder entirely");
        // The two rendered groups must be groupIndex 0 and 1, not 0 and 2.
        assertEquals(0, rendered.get(0).groupIndex());
        assertEquals(1, rendered.get(1).groupIndex());
    }

    @Test
    void allGroupsRenderedWhenNoRemainder() {
        List<List<LegacyCoordinate>> groups = LegacySlopeCalculator.groupCoordinates(coords(10), 5);
        List<LegacySlopeGroup> rendered = LegacySlopeCalculator.toRenderedGroups(groups);
        assertEquals(2, rendered.size());
    }
}
