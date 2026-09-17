package com.season.semiproject.spatial.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derived Network Layer builder: TrailFeature.geom (Raw Spatial Layer) -> TrailNode/TrailSegment.
 * See docs/01-architecture-and-modeling.md (Phase 6) for the full design rationale.
 *
 * This Phase builds topology using EXACT coordinate equality only. No snapping tolerance is
 * applied (a near-endpoint tolerance experiment was run for reference but intentionally not
 * used -- see docs), and no mid-line splitting is performed (none of the candidate intersections
 * found in the data turned out to be genuine non-endpoint crossings -- see docs). Every
 * TrailSegment's geometry is copied unchanged from its source LineString Part.
 *
 * Rebuild strategy: delete the entire existing trail_segment/trail_node content and regenerate
 * it from trail_feature, all in one transaction -- trail/trail_feature (Raw layer) are never
 * touched, and a build failure rolls back to leave the previous network (if any) intact.
 *
 * Deliberately NOT wired into any Controller/API -- only NetworkBuildRunner (active under the
 * `spatial-network-build` profile) calls this.
 */
@Service
public class NetworkBuildService {

    private static final Logger log = LoggerFactory.getLogger(NetworkBuildService.class);

    private final NetworkBuildDAO dao;

    @Autowired
    public NetworkBuildService(NetworkBuildDAO dao) {
        this.dao = dao;
    }

    @Transactional
    public NetworkBuildResult buildNetwork() {
        dao.deleteAllTrailSegments();
        dao.deleteAllTrailNodes();

        dao.createLinePartTempTable();
        int lineParts = dao.countLineParts();
        log.info("Decomposed TrailFeature.geom into {} LineString Parts (ST_Dump)", lineParts);

        dao.insertNodesFromParts();
        int nodes = dao.countTrailNodes();
        log.info("Created {} TrailNode rows from exact-match endpoint clustering", nodes);

        dao.insertSegmentsFromParts();
        int segments = dao.countTrailSegments();
        log.info("Created {} TrailSegment rows", segments);

        // Sanity check: in this exact-match, no-split design, every Part must become exactly
        // one Segment. If not, some Part's endpoint failed to resolve to a TrailNode (should be
        // impossible given nodes are derived from these same Parts' endpoints) -- treat this as
        // a hard failure rather than silently persisting an incomplete network.
        if (segments != lineParts) {
            throw new NetworkBuildException(
                    "Network build produced " + segments + " segments but there were "
                            + lineParts + " LineString Parts -- some Part's endpoint did not "
                            + "resolve to a TrailNode. Aborting (transaction will roll back).");
        }

        return new NetworkBuildResult(lineParts, nodes, segments);
    }
}
