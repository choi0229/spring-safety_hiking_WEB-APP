package com.season.semiproject.spatial;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TrailImportDAO {

    @Autowired
    private SqlSession session;

    public void deleteTrailsBySourceFile(String sourceFile) {
        session.delete("deleteTrailsBySourceFile", sourceFile);
    }

    /** Inserts the trail row and returns the generated id (also set on param.id by MyBatis). */
    public Long insertTrail(TrailInsertParam param) {
        session.insert("insertTrail", param);
        return param.getId();
    }

    public void insertTrailFeature(TrailFeatureInsertParam param) {
        session.insert("insertTrailFeature", param);
    }
}
