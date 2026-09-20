package com.season.semiproject.spatial.slope;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static com.season.semiproject.spatial.slope.SlopeTestFixtures.segment;

/**
 * Pure unit tests for {@link NetworkChainBuilder} on hand-built graphs -- no DB, no Spring. Covers
 * the linear-chain, junction and cycle cases described in docs/09-slope-section-analysis.md; only
 * the linear-chain case is exercised by the real dataset today (every TrailNode has degree 1 or 2
 * currently -- see Phase 12A), but junction/cycle handling must still be correct and deterministic.
 */
class NetworkChainBuilderTest {

    private static final double DELTA = 1e-9;

    @Test
    void simpleLinearChainOfThreeSegmentsBecomesOneChain() {
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 1, 2, 5.0, 100, 90, 0, 0, 0, 0.00005),
                segment(2, 2, 3, 5.0, 101, 95, 0, 0.00005, 0, 0.00010),
                segment(3, 3, 4, 5.0, 102, 100, 0, 0.00010, 0, 0.00015));

        List<NetworkChain> chains = NetworkChainBuilder.buildChains(rows);

        assertEquals(1, chains.size());
        NetworkChain chain = chains.get(0);
        assertEquals(3, chain.getSegments().size());
        assertEquals(15.0, chain.getTotalLengthMeters(), DELTA);
        assertFalse(chain.isClosed());
        // Canonical direction: node1 (smaller id) is the start, so segment 1 comes first.
        assertEquals(1, chain.getSegments().get(0).getSegmentId());
        assertEquals(0.0, chain.getSegments().get(0).getCumulativeStartMeters(), DELTA);
        assertEquals(15.0, chain.getSegments().get(2).getCumulativeEndMeters(), DELTA);
    }

    @Test
    void reversedSegmentOrientationIsHandledAndCoordinatesAreFlipped() {
        // Segment 2 is stored 3->2 (opposite of the chain's node1->node4 traversal direction).
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 1, 2, 5.0, 100, 90, 0, 0, 0, 0.00005),
                segment(2, 3, 2, 5.0, 101, 95, 0, 0.00010, 0, 0.00005),
                segment(3, 3, 4, 5.0, 102, 100, 0, 0.00010, 0, 0.00015));

        List<NetworkChain> chains = NetworkChainBuilder.buildChains(rows);

        assertEquals(1, chains.size());
        NetworkChain chain = chains.get(0);
        ChainSegmentUsage middle = chain.getSegments().get(1);
        assertEquals(2, middle.getSegmentId());
        assertTrue(middle.isReversed());
        // Oriented coordinates must run in traversal direction: from (0,0.00005) to (0,0.00010).
        assertEquals(0.00005, middle.getOrientedCoordinates().get(0)[1], DELTA);
        assertEquals(0.00010, middle.getOrientedCoordinates().get(1)[1], DELTA);
    }

    @Test
    void canonicalDirectionStartsAtTheSmallerNodeIdEvenIfBuiltFromTheOtherEnd() {
        // Same 3-segment chain as above but node ids run the opposite way (start node id is 4,
        // larger than the far end's id 1) -- canonicalize() must still normalize start<end.
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 4, 3, 5.0, 100, 90, 0, 0.00015, 0, 0.00010),
                segment(2, 3, 2, 5.0, 101, 95, 0, 0.00010, 0, 0.00005),
                segment(3, 2, 1, 5.0, 102, 100, 0, 0.00005, 0, 0));

        List<NetworkChain> chains = NetworkChainBuilder.buildChains(rows);

        assertEquals(1, chains.size());
        NetworkChain chain = chains.get(0);
        // After canonicalization, traversal starts at node 1 (segment 3's original "to" end).
        assertEquals(3, chain.getSegments().get(0).getSegmentId());
        assertEquals(1, chain.getSegments().get(2).getSegmentId());
        assertEquals(0.0, chain.getSegments().get(0).getCumulativeStartMeters(), DELTA);
    }

    @Test
    void junctionNodeSplitsIntoThreeSeparateChains() {
        // A "Y": 1-2, 2-3, 2-4, node 2 has degree 3.
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 1, 2, 5.0, 100, 90, 0, 0, 0, 0.00005),
                segment(2, 2, 3, 5.0, 101, 95, 0, 0.00005, 0, 0.00010),
                segment(3, 2, 4, 5.0, 102, 95, 0, 0.00005, 0.00005, 0.00005));

        List<NetworkChain> chains = NetworkChainBuilder.buildChains(rows);

        assertEquals(3, chains.size());
        for (NetworkChain chain : chains) {
            assertEquals(1, chain.getSegments().size());
            assertFalse(chain.isClosed());
        }
    }

    @Test
    void pureCycleWithNoBoundaryNodeBecomesOneClosedChain() {
        // A square loop: 1-2-3-4-1, every node has degree 2, no boundary node exists.
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 1, 2, 5.0, 100, 90, 0, 0, 0, 0.00005),
                segment(2, 2, 3, 5.0, 101, 91, 0, 0.00005, 0.00005, 0.00005),
                segment(3, 3, 4, 5.0, 102, 92, 0.00005, 0.00005, 0.00005, 0),
                segment(4, 4, 1, 5.0, 103, 93, 0.00005, 0, 0, 0));

        List<NetworkChain> chains = NetworkChainBuilder.buildChains(rows);

        assertEquals(1, chains.size());
        NetworkChain chain = chains.get(0);
        assertTrue(chain.isClosed());
        assertEquals(4, chain.getSegments().size());
        assertEquals(20.0, chain.getTotalLengthMeters(), DELTA);
    }

    @Test
    void singleSegmentComponentIsItsOwnChain() {
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 10, 20, 3.0, 100, 90, 0, 0, 0, 0.00003));

        List<NetworkChain> chains = NetworkChainBuilder.buildChains(rows);

        assertEquals(1, chains.size());
        assertEquals(3.0, chains.get(0).getTotalLengthMeters(), DELTA);
    }

    @Test
    void doesNotMixTwoDisconnectedComponents() {
        // Two separate 1-segment chains sharing no nodes.
        List<TrailSegmentElevationRow> rows = List.of(
                segment(1, 1, 2, 5.0, 100, 90, 0, 0, 0, 0.00005),
                segment(2, 100, 200, 7.0, 200, 150, 1, 0, 1, 0.00007));

        List<NetworkChain> chains = NetworkChainBuilder.buildChains(rows);

        assertEquals(2, chains.size());
        assertNotEquals(chains.get(0).getSegments().get(0).getSegmentId(),
                chains.get(1).getSegments().get(0).getSegmentId());
    }
}
