package com.season.semiproject.spatial.network;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Pure orchestration tests using a mocked DAO -- exercises the build sequence and the
 * part-count-vs-segment-count sanity check without needing a database. Real database behavior
 * (actual node/segment counts, exact-match clustering correctness, connectivity) is verified
 * against the live Docker PostgreSQL/PostGIS instance -- see
 * docs/01-architecture-and-modeling.md, Phase 6, for those results.
 */
class NetworkBuildServiceTest {

    @Test
    void buildsNetworkAndReturnsCountsWhenPartsMatchSegments() {
        NetworkBuildDAO dao = mock(NetworkBuildDAO.class);
        when(dao.countLineParts()).thenReturn(2122);
        when(dao.countTrailNodes()).thenReturn(2526);
        when(dao.countTrailSegments()).thenReturn(2122);

        NetworkBuildService service = new NetworkBuildService(dao);
        NetworkBuildResult result = service.buildNetwork();

        assertEquals(2122, result.lineParts);
        assertEquals(2526, result.nodes);
        assertEquals(2122, result.segments);

        InOrder order = inOrder(dao);
        order.verify(dao).deleteAllTrailSegments();
        order.verify(dao).deleteAllTrailNodes();
        order.verify(dao).createLinePartTempTable();
        order.verify(dao).insertNodesFromParts();
        order.verify(dao).insertSegmentsFromParts();
    }

    @Test
    void deletesExistingNetworkBeforeInsertingNewOne() {
        NetworkBuildDAO dao = mock(NetworkBuildDAO.class);
        when(dao.countLineParts()).thenReturn(5);
        when(dao.countTrailNodes()).thenReturn(6);
        when(dao.countTrailSegments()).thenReturn(5);

        new NetworkBuildService(dao).buildNetwork();

        verify(dao, times(1)).deleteAllTrailSegments();
        verify(dao, times(1)).deleteAllTrailNodes();
    }

    @Test
    void abortsWhenSegmentCountDoesNotMatchPartCount() {
        NetworkBuildDAO dao = mock(NetworkBuildDAO.class);
        when(dao.countLineParts()).thenReturn(10);
        when(dao.countTrailNodes()).thenReturn(8);
        when(dao.countTrailSegments()).thenReturn(9); // one Part's endpoint failed to resolve

        NetworkBuildService service = new NetworkBuildService(dao);

        assertThrows(NetworkBuildException.class, service::buildNetwork);
    }
}
