package com.season.semiproject.spatial.slope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Phase 12B/12C: Network-based fixed-distance SlopeSection model, compute-on-request (no
 * persistence -- see docs/09-slope-section-analysis.md, "storage model" comparison). Never
 * touches trail_segment/trail_node; only reads them.
 *
 * Section geometry is cut per-Segment (Phase 12C), not by one global fraction over the whole
 * merged chain (Phase 12B) -- see {@link ChainDistanceLocator} for why: cutting one short,
 * nearly-straight Segment at a time keeps the geometry-fraction/geography-distance mismatch
 * negligible, where cutting the whole chain at once let it grow to double digits of percent on
 * long chains (measured on the real dataset, see docs/09).
 */
@Service
public class SlopeSectionService {

    public static final Set<Integer> ALLOWED_WINDOW_METERS = Set.of(10, 20, 30);

    private final SlopeSectionDAO dao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SlopeSectionService(SlopeSectionDAO dao) {
        this.dao = dao;
    }

    public boolean trailExists(long trailId) {
        return dao.countTrailById(trailId) > 0;
    }

    public SlopeSectionFeatureCollection computeSlopeSections(long trailId, int windowMeters) {
        List<TrailSegmentElevationRow> rows = dao.findSegmentsForTrail(trailId);
        List<NetworkChain> chains = NetworkChainBuilder.buildChains(rows);
        Map<Integer, NetworkChain> chainByIndex = new HashMap<>();
        for (NetworkChain chain : chains) {
            chainByIndex.put(chain.getChainIndex(), chain);
        }

        List<SlopeSectionResult> allResults = new ArrayList<>();
        for (NetworkChain chain : chains) {
            allResults.addAll(SlopeSectionCalculator.computeSections(chain, windowMeters));
        }

        List<SectionCutRequest> cutRequests = new ArrayList<>();
        Map<Integer, List<Piece>> piecesBySection = new HashMap<>();
        for (int sectionIdx = 0; sectionIdx < allResults.size(); sectionIdx++) {
            SlopeSectionResult r = allResults.get(sectionIdx);
            NetworkChain chain = chainByIndex.get(r.getChainIndex());
            piecesBySection.put(sectionIdx, buildPieces(chain, r.getChainStartMeters(), r.getChainEndMeters(), cutRequests));
        }

        Map<Integer, String> cutGeometryByRequestIdx = new HashMap<>();
        for (SectionCutResultRow row : dao.cutSections(cutRequests)) {
            cutGeometryByRequestIdx.put(row.getIdx(), row.getGeometryGeoJson());
        }

        List<SlopeSectionFeature> features = new ArrayList<>(allResults.size());
        for (int sectionIdx = 0; sectionIdx < allResults.size(); sectionIdx++) {
            SlopeSectionResult r = allResults.get(sectionIdx);
            List<double[]> coordinates = assembleCoordinates(piecesBySection.get(sectionIdx), cutGeometryByRequestIdx);
            if (coordinates.size() < 2) {
                // Defensive only: every requested piece resolves to a matching row.
                continue;
            }
            JsonNode geometry = buildLineStringNode(coordinates);
            SlopeSectionProperties properties = new SlopeSectionProperties(
                    r.getChainIndex(), r.getSectionIndex(), r.getDistanceMeters(),
                    r.getEstimatedElevationStart(), r.getEstimatedElevationEnd(), r.getEstimatedElevationDelta(),
                    r.getEstimatedSlopePercent(), r.isPartialSection() ? "PARTIAL_SECTION" : null);
            features.add(new SlopeSectionFeature(properties, geometry));
        }

        return new SlopeSectionFeatureCollection(trailId, windowMeters, features);
    }

    /** One piece of a SlopeSection's geometry: either a whole Segment's coordinates (no DB round
     * trip needed) or a pending per-Segment {@code ST_LineSubstring} cut, resolved later via
     * {@code cutRequestIdx} into {@code cutGeometryByRequestIdx}. */
    private static final class Piece {
        final Integer cutRequestIdx;
        final List<double[]> fullCoordinates;

        Piece(Integer cutRequestIdx, List<double[]> fullCoordinates) {
            this.cutRequestIdx = cutRequestIdx;
            this.fullCoordinates = fullCoordinates;
        }
    }

    private List<Piece> buildPieces(NetworkChain chain, double startMeters, double endMeters,
            List<SectionCutRequest> cutRequests) {
        List<ChainSegmentUsage> segments = chain.getSegments();
        ChainDistanceLocator.LocatedPoint start = ChainDistanceLocator.locateStart(chain, startMeters);
        ChainDistanceLocator.LocatedPoint end = ChainDistanceLocator.locateEnd(chain, endMeters);

        List<Piece> pieces = new ArrayList<>();
        if (start.usageIndex() == end.usageIndex()) {
            pieces.add(cutPiece(segments.get(start.usageIndex()), start.localFraction(), end.localFraction(), cutRequests));
            return pieces;
        }

        pieces.add(cutPiece(segments.get(start.usageIndex()), start.localFraction(), 1.0, cutRequests));
        for (int i = start.usageIndex() + 1; i < end.usageIndex(); i++) {
            pieces.add(new Piece(null, segments.get(i).getOrientedCoordinates()));
        }
        pieces.add(cutPiece(segments.get(end.usageIndex()), 0.0, end.localFraction(), cutRequests));
        return pieces;
    }

    private static final double FRACTION_EPS = 1e-6;

    private Piece cutPiece(ChainSegmentUsage usage, double fromFraction, double toFraction,
            List<SectionCutRequest> cutRequests) {
        if (fromFraction <= FRACTION_EPS && toFraction >= 1.0 - FRACTION_EPS) {
            return new Piece(null, usage.getOrientedCoordinates());
        }
        String wkt = GeoJsonLineStringParser.toWkt(usage.getOrientedCoordinates());
        int idx = cutRequests.size();
        cutRequests.add(new SectionCutRequest(idx, wkt, fromFraction, toFraction));
        return new Piece(idx, null);
    }

    private List<double[]> assembleCoordinates(List<Piece> pieces, Map<Integer, String> cutGeometryByRequestIdx) {
        List<double[]> merged = new ArrayList<>();
        for (Piece piece : pieces) {
            List<double[]> coords = piece.fullCoordinates != null
                    ? piece.fullCoordinates
                    : GeoJsonLineStringParser.parseLineString(cutGeometryByRequestIdx.get(piece.cutRequestIdx));
            int startIdx = merged.isEmpty() ? 0 : 1;
            for (int i = startIdx; i < coords.size(); i++) {
                merged.add(coords.get(i));
            }
        }
        return merged;
    }

    private JsonNode buildLineStringNode(List<double[]> coordinates) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", "LineString");
        ArrayNode coordsArray = objectMapper.createArrayNode();
        for (double[] pt : coordinates) {
            ArrayNode pair = objectMapper.createArrayNode();
            pair.add(pt[0]);
            pair.add(pt[1]);
            coordsArray.add(pair);
        }
        node.set("coordinates", coordsArray);
        return node;
    }
}
