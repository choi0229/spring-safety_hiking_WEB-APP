package com.season.semiproject.spatial.slope;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SlopeSectionDAO {

    @Autowired
    private SqlSession session;

    public int countTrailById(long trailId) {
        return session.selectOne("countTrailById", trailId);
    }

    /** All TrailSegments of one Trail, joined to their parent TrailFeature's DN -- one query. */
    public List<TrailSegmentElevationRow> findSegmentsForTrail(long trailId) {
        return session.selectList("findSegmentsForTrail", trailId);
    }

    /** Cuts every requested (wkt, startFraction, endFraction) window in a single batched query. */
    public List<SectionCutResultRow> cutSections(List<SectionCutRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        return session.selectList("cutSections", Map.of("windows", requests));
    }
}
