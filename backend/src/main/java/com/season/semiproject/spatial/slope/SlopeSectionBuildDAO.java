package com.season.semiproject.spatial.slope;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Write-side access for the `slope_section` Derived Analysis Layer, used only by
 * {@link SlopeSectionBuildService} (the `spatial-slope-build` profile). Never called from the
 * Production API request path -- see {@link SlopeSectionService} for the read-only Query side. */
@Component
public class SlopeSectionBuildDAO {

    @Autowired
    private SqlSession session;

    /** Every Trail id, so the build can iterate all of them without hardcoding a list. */
    public List<Long> findAllTrailIds() {
        return session.selectList("findAllTrailIds");
    }

    public void deleteAllSlopeSections() {
        session.delete("deleteAllSlopeSections");
    }

    /** Single bulk INSERT for the whole build (all Trails' sections at once) -- no N+1. */
    public void insertSlopeSections(List<SlopeSectionInsertParam> params) {
        if (params.isEmpty()) {
            return;
        }
        session.insert("insertSlopeSections", Map.of("rows", params));
    }

    public int countAllSlopeSections() {
        return session.selectOne("countAllSlopeSections");
    }
}
