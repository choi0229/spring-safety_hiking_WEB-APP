package com.season.semiproject.spatial.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    public boolean trailExists(long trailId) {
        return dao.countTrailById(trailId) > 0;
    }

    public List<NearbySegmentRow> findSegmentsNearAccident(Long accidentId, double distanceMeters) {
        return dao.findSegmentsNearAccident(accidentId, distanceMeters);
    }

    public List<NearbyAccidentRow> findAccidentsNearSegment(Long segmentId, double distanceMeters) {
        return dao.findAccidentsNearSegment(segmentId, distanceMeters);
    }

    /** "API C": one Trail -> every distinct AccidentPoint within distanceMeters of any of its
     * TrailSegments, as a GeoJSON FeatureCollection<Point>. See mapper-spatial-query.xml,
     * findAccidentCandidatesForTrail, for the dedup/MIN(distance) SQL this reads. */
    public AccidentCandidateFeatureCollection findAccidentCandidatesForTrail(long trailId, double distanceMeters) {
        List<NearbyAccidentCandidateRow> rows = dao.findAccidentCandidatesForTrail(trailId, distanceMeters);

        List<AccidentCandidateFeature> features = new ArrayList<>(rows.size());
        for (NearbyAccidentCandidateRow row : rows) {
            JsonNode geometry = parseGeometry(row.getGeometryGeoJson());
            AccidentCandidateProperties properties = new AccidentCandidateProperties(
                    row.getAccidentId(), row.getReportNo(), row.getDispatchDate(),
                    row.getAccidentType(), row.getLocationName(), row.getDistanceToTrailMeters());
            features.add(new AccidentCandidateFeature(properties, geometry));
        }
        return new AccidentCandidateFeatureCollection(trailId, distanceMeters, features);
    }

    private JsonNode parseGeometry(String geoJson) {
        try {
            return objectMapper.readTree(geoJson);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid geometry GeoJSON stored in accident_point: " + geoJson, e);
        }
    }
}
