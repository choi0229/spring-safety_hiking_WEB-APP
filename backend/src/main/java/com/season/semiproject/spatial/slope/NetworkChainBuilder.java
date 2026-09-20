package com.season.semiproject.spatial.slope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Splits one Trail's TrailSegment/TrailNode graph into {@link NetworkChain}s: maximal walks
 * between "boundary" nodes (degree != 2 -- an endpoint or a junction), so that no chain crosses a
 * junction and no chain crosses a connected-component boundary (a chain can only ever be built
 * from Segments that are actually reachable from each other).
 *
 * Pure in-memory graph algorithm, no DB/Spring dependency, so it is unit-testable with hand-built
 * graphs -- including junction (degree >= 3) and cycle (no boundary node) cases that do NOT occur
 * in the current dataset (see docs/09-slope-section-analysis.md, Phase 12A: every TrailNode today
 * has degree 1 or 2 only) but must still be handled correctly and deterministically.
 *
 * Only ever receives the Segments of a single Trail (the caller's Mapper query already filters by
 * trail_id), so a chain can never mix Segments from two different Trails.
 */
public final class NetworkChainBuilder {

    private NetworkChainBuilder() {
    }

    public static List<NetworkChain> buildChains(List<TrailSegmentElevationRow> rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, TrailSegmentElevationRow> rowById = new HashMap<>();
        Map<Long, List<Long>> nodeToSegmentIds = new HashMap<>();
        for (TrailSegmentElevationRow row : rows) {
            rowById.put(row.getSegmentId(), row);
            nodeToSegmentIds.computeIfAbsent(row.getFromNodeId(), k -> new ArrayList<>()).add(row.getSegmentId());
            nodeToSegmentIds.computeIfAbsent(row.getToNodeId(), k -> new ArrayList<>()).add(row.getSegmentId());
        }
        Map<Long, Integer> degree = new HashMap<>();
        nodeToSegmentIds.forEach((node, segIds) -> degree.put(node, segIds.size()));

        Set<Long> visitedSegments = new HashSet<>();
        List<NetworkChain> chains = new ArrayList<>();
        int chainIndex = 0;

        // Pass 1: start a chain from every unvisited Segment incident to a boundary node
        // (degree != 2), in deterministic (sorted) node/segment order.
        for (Long nodeId : new TreeSet<>(degree.keySet())) {
            if (degree.get(nodeId) == 2) {
                continue;
            }
            List<Long> incident = new ArrayList<>(nodeToSegmentIds.get(nodeId));
            Collections.sort(incident);
            for (Long segId : incident) {
                if (visitedSegments.contains(segId)) {
                    continue;
                }
                chains.add(walk(nodeId, segId, rowById, nodeToSegmentIds, degree, visitedSegments, chainIndex++));
            }
        }

        // Pass 2: any Segment still unvisited belongs to a pure cycle (every node on it has
        // degree 2) -- not present in the current dataset, but handled deterministically: start
        // from the lowest unvisited segment id's from_node each time.
        List<Long> remainingSegmentIds = new ArrayList<>(rowById.keySet());
        remainingSegmentIds.removeAll(visitedSegments);
        Collections.sort(remainingSegmentIds);
        for (Long segId : remainingSegmentIds) {
            if (visitedSegments.contains(segId)) {
                continue;
            }
            long startNode = rowById.get(segId).getFromNodeId();
            chains.add(walk(startNode, segId, rowById, nodeToSegmentIds, degree, visitedSegments, chainIndex++));
        }

        return chains;
    }

    private static NetworkChain walk(long startNode, long firstSegmentId,
            Map<Long, TrailSegmentElevationRow> rowById, Map<Long, List<Long>> nodeToSegmentIds,
            Map<Long, Integer> degree, Set<Long> visitedSegments, int chainIndex) {

        List<ChainSegmentUsage> usages = new ArrayList<>();
        double cumulative = 0.0;
        long currentNode = startNode;
        long currentSegId = firstSegmentId;
        boolean closed = false;
        long endNode = startNode;

        while (true) {
            TrailSegmentElevationRow row = rowById.get(currentSegId);
            visitedSegments.add(currentSegId);

            boolean reversed = row.getFromNodeId() != currentNode;
            List<double[]> raw = GeoJsonLineStringParser.parseLineString(row.getGeometryGeoJson());
            List<double[]> oriented = reversed ? GeoJsonLineStringParser.reversed(raw) : raw;

            double start = cumulative;
            double end = cumulative + row.getLengthMeters();
            usages.add(new ChainSegmentUsage(row.getSegmentId(), row.getSourceTrailFeatureId(),
                    row.getSourceDn(), start, end, reversed, oriented));
            cumulative = end;

            long nextNode = reversed ? row.getFromNodeId() : row.getToNodeId();
            endNode = nextNode;
            if (nextNode == startNode) {
                closed = true;
                break;
            }
            if (degree.get(nextNode) != 2) {
                break;
            }
            List<Long> incident = nodeToSegmentIds.get(nextNode);
            Long nextSegId = null;
            for (Long candidate : incident) {
                if (!candidate.equals(currentSegId)) {
                    nextSegId = candidate;
                    break;
                }
            }
            if (nextSegId == null || visitedSegments.contains(nextSegId)) {
                // Defensive only: a degree-2 node must have exactly one still-unvisited other
                // Segment when we first reach it via a fresh walk. Stop rather than loop forever.
                break;
            }
            currentNode = nextNode;
            currentSegId = nextSegId;
        }

        NetworkChain chain = new NetworkChain(chainIndex, usages, closed);
        return closed ? chain : canonicalize(chain, startNode, endNode);
    }

    private static NetworkChain canonicalize(NetworkChain chain, long startNode, long endNode) {
        if (endNode >= startNode) {
            return chain;
        }
        List<ChainSegmentUsage> original = chain.getSegments();
        List<ChainSegmentUsage> reversed = new ArrayList<>(original.size());
        double total = chain.getTotalLengthMeters();
        for (int i = original.size() - 1; i >= 0; i--) {
            ChainSegmentUsage u = original.get(i);
            double newStart = total - u.getCumulativeEndMeters();
            double newEnd = total - u.getCumulativeStartMeters();
            reversed.add(new ChainSegmentUsage(u.getSegmentId(), u.getSourceTrailFeatureId(),
                    u.getSourceDn(), newStart, newEnd, !u.isReversed(),
                    GeoJsonLineStringParser.reversed(u.getOrientedCoordinates())));
        }
        return new NetworkChain(chain.getChainIndex(), reversed, chain.isClosed());
    }
}
