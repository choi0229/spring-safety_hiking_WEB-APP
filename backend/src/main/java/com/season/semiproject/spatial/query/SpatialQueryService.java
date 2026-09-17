package com.season.semiproject.spatial.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 8 spatial relation queries between AccidentPoint (Raw) and TrailSegment (Derived
 * Network). Deliberately stateless and read-only -- no FK, no cached distance, computed fresh on
 * every call via PostGIS (see mapper-spatial-query.xml). `distanceMeters` is a caller-supplied
 * search radius, not a claimed risk/safety radius -- see docs/05-accident-spatial-query.md.
 */
@Service
public class SpatialQueryService {

    private final SpatialQueryDAO dao;

    @Autowired
    public SpatialQueryService(SpatialQueryDAO dao) {
        this.dao = dao;
    }

    public boolean accidentExists(Long accidentId) {
        return dao.countAccidentById(accidentId) > 0;
    }

    public boolean segmentExists(Long segmentId) {
        return dao.countSegmentById(segmentId) > 0;
    }

    public List<NearbySegmentRow> findSegmentsNearAccident(Long accidentId, double distanceMeters) {
        return dao.findSegmentsNearAccident(accidentId, distanceMeters);
    }

    public List<NearbyAccidentRow> findAccidentsNearSegment(Long segmentId, double distanceMeters) {
        return dao.findAccidentsNearSegment(segmentId, distanceMeters);
    }
}
