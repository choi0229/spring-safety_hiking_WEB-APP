package com.season.semiproject.spatial.query;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SpatialQueryDAO {

    @Autowired
    private SqlSession session;

    public int countAccidentById(Long accidentId) {
        return session.selectOne("countAccidentById", accidentId);
    }

    public int countSegmentById(Long segmentId) {
        return session.selectOne("countSegmentById", segmentId);
    }

    /** Exhaustive within-threshold search -- no KNN LIMIT shortcut (see mapper-spatial-query.xml). */
    public List<NearbySegmentRow> findSegmentsNearAccident(Long accidentId, double distanceMeters) {
        Map<String, Object> params = new HashMap<>();
        params.put("accidentId", accidentId);
        params.put("distanceMeters", distanceMeters);
        return session.selectList("findSegmentsNearAccident", params);
    }

    /** Exhaustive within-threshold search -- no KNN LIMIT shortcut (see mapper-spatial-query.xml). */
    public List<NearbyAccidentRow> findAccidentsNearSegment(Long segmentId, double distanceMeters) {
        Map<String, Object> params = new HashMap<>();
        params.put("segmentId", segmentId);
        params.put("distanceMeters", distanceMeters);
        return session.selectList("findAccidentsNearSegment", params);
    }
}
