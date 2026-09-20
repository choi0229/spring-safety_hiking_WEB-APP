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

    /** Production API read path: already-persisted SlopeSection rows for one Trail/window, in
     * deterministic order. No computation happens on this path. */
    public List<SlopeSectionRow> findPersistedSections(long trailId, int windowMeters) {
        return session.selectList("findPersistedSections", Map.of("trailId", trailId, "windowMeters", windowMeters));
    }

    /** Build-time only (SlopeSectionBuildService): all TrailSegments of one Trail, joined to
     * their parent TrailFeature's DN -- one query. */
    public List<TrailSegmentElevationRow> findSegmentsForTrail(long trailId) {
        return session.selectList("findSegmentsForTrail", trailId);
    }

    /** Build-time only: cuts every requested (wkt, startFraction, endFraction) window in a
     * single batched query. */
    public List<SectionCutResultRow> cutSections(List<SectionCutRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        return session.selectList("cutSections", Map.of("windows", requests));
    }
}
