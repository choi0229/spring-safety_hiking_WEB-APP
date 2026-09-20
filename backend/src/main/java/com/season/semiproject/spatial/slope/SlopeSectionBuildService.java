package com.season.semiproject.spatial.slope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derived Analysis Layer builder: TrailSegment/TrailNode Network + TrailFeature.dn_value -&gt;
 * slope_section (precomputed 20m SlopeSection rows). See docs/09-slope-section-analysis.md for
 * the full rationale for moving SlopeSection from compute-on-request to precompute + persistence
 * -- it is a deterministic function of the Network state, never a per-request value.
 *
 * This class is the relocated computation orchestration that used to live directly in
 * {@link SlopeSectionService} (Phase 12B/12C, compute-on-request) -- {@link NetworkChainBuilder},
 * {@link ElevationProfile}, {@link SlopeSectionCalculator} and {@link ChainDistanceLocator} are
 * reused completely unchanged; only the destination of their output changed (DB rows instead of
 * an HTTP response). {@link SlopeSectionService} is now a thin read-only Query service with no
 * computation at all.
 *
 * Deliberately NOT wired into any Controller/API -- only SlopeSectionBuildRunner (active under
 * the `spatial-slope-build` profile) calls this, the same pattern as NetworkBuildService.
 *
 * Rebuild strategy: delete the entire existing slope_section content and regenerate it for every
 * Trail in one transaction -- trail/trail_feature/trail_node/trail_segment (Raw + Network layers)
 * are never touched, and a build failure rolls back to leave the previous slope_section content
 * (if any) intact.
 */
@Service
public class SlopeSectionBuildService {

    private static final Logger log = LoggerFactory.getLogger(SlopeSectionBuildService.class);

    /** The only window size Production persists -- see slope-section-schema.sql,
     * chk_slope_section_window_m. 10m/30m remain available only as pure in-memory calculations
     * (SlopeSectionCalculator), exercised directly by its own unit tests. */
    public static final int PRODUCTION_WINDOW_METERS = 20;

    private static final double FRACTION_EPS = 1e-6;

    private final SlopeSectionDAO readDao;
    private final SlopeSectionBuildDAO buildDao;

    @Autowired
    public SlopeSectionBuildService(SlopeSectionDAO readDao, SlopeSectionBuildDAO buildDao) {
        this.readDao = readDao;
        this.buildDao = buildDao;
    }

    @Transactional
    public SlopeSectionBuildResult buildAll() {
        buildDao.deleteAllSlopeSections();

        List<Long> trailIds = buildDao.findAllTrailIds();
        List<SlopeSectionInsertParam> allParams = new ArrayList<>();
        Map<Long, Integer> countByTrail = new LinkedHashMap<>();

        for (Long trailId : trailIds) {
            List<SlopeSectionInsertParam> params = computeParamsForTrail(trailId);
            countByTrail.put(trailId, params.size());
            allParams.addAll(params);
            log.info("Computed {} SlopeSection(s) for trailId={}", params.size(), trailId);
        }

        buildDao.insertSlopeSections(allParams);

        int actualTotal = buildDao.countAllSlopeSections();
        if (actualTotal != allParams.size()) {
            throw new SlopeSectionBuildException(
                    "Slope build produced " + allParams.size() + " computed row(s) but "
                            + actualTotal + " row(s) exist in slope_section after insert -- "
                            + "aborting (transaction will roll back).");
        }

        return new SlopeSectionBuildResult(actualTotal, countByTrail);
    }

    /** Computes every {@value #PRODUCTION_WINDOW_METERS}m SlopeSection for one Trail, ready to
     * insert. Same computation pipeline the old compute-on-request SlopeSectionService used. */
    private List<SlopeSectionInsertParam> computeParamsForTrail(long trailId) {
        List<TrailSegmentElevationRow> rows = readDao.findSegmentsForTrail(trailId);
        List<NetworkChain> chains = NetworkChainBuilder.buildChains(rows);
        Map<Integer, NetworkChain> chainByIndex = new HashMap<>();
        for (NetworkChain chain : chains) {
            chainByIndex.put(chain.getChainIndex(), chain);
        }

        List<SlopeSectionResult> allResults = new ArrayList<>();
        for (NetworkChain chain : chains) {
            allResults.addAll(SlopeSectionCalculator.computeSections(chain, PRODUCTION_WINDOW_METERS));
        }
        if (allResults.isEmpty()) {
            return List.of();
        }

        List<SectionCutRequest> cutRequests = new ArrayList<>();
        Map<Integer, List<Piece>> piecesBySection = new HashMap<>();
        for (int sectionIdx = 0; sectionIdx < allResults.size(); sectionIdx++) {
            SlopeSectionResult r = allResults.get(sectionIdx);
            NetworkChain chain = chainByIndex.get(r.getChainIndex());
            piecesBySection.put(sectionIdx, buildPieces(chain, r.getChainStartMeters(), r.getChainEndMeters(), cutRequests));
        }

        Map<Integer, String> cutGeometryByRequestIdx = new HashMap<>();
        for (SectionCutResultRow row : readDao.cutSections(cutRequests)) {
            cutGeometryByRequestIdx.put(row.getIdx(), row.getGeometryGeoJson());
        }

        List<SlopeSectionInsertParam> params = new ArrayList<>(allResults.size());
        for (int sectionIdx = 0; sectionIdx < allResults.size(); sectionIdx++) {
            SlopeSectionResult r = allResults.get(sectionIdx);
            List<double[]> coordinates = assembleCoordinates(piecesBySection.get(sectionIdx), cutGeometryByRequestIdx);
            if (coordinates.size() < 2) {
                // Defensive only: every requested piece resolves to a matching row.
                continue;
            }
            String wkt = GeoJsonLineStringParser.toWkt(coordinates);
            params.add(new SlopeSectionInsertParam(
                    trailId, r.getChainIndex(), r.getSectionIndex(), PRODUCTION_WINDOW_METERS,
                    r.getDistanceMeters(), r.getEstimatedElevationStart(), r.getEstimatedElevationEnd(),
                    r.getEstimatedElevationDelta(), r.getEstimatedSlopePercent(),
                    r.isPartialSection() ? "PARTIAL_SECTION" : null, wkt));
        }
        return params;
    }

    /** One piece of a SlopeSection's geometry: either a whole Segment's coordinates (no DB round
     * trip needed) or a pending per-Segment {@code ST_LineSubstring} cut, resolved later via
     * {@code cutRequestIdx} into {@code cutGeometryByRequestIdx}. Identical to the piece model
     * the old compute-on-request SlopeSectionService used. */
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
}
