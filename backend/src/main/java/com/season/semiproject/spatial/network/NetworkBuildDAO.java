package com.season.semiproject.spatial.network;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NetworkBuildDAO {

    @Autowired
    private SqlSession session;

    public void deleteAllTrailSegments() {
        session.delete("deleteAllTrailSegments");
    }

    public void deleteAllTrailNodes() {
        session.delete("deleteAllTrailNodes");
    }

    public void createLinePartTempTable() {
        session.update("createLinePartTempTable");
    }

    public int countLineParts() {
        return session.selectOne("countLineParts");
    }

    public void insertNodesFromParts() {
        session.insert("insertNodesFromParts");
    }

    public int countTrailNodes() {
        return session.selectOne("countTrailNodes");
    }

    public void insertSegmentsFromParts() {
        session.insert("insertSegmentsFromParts");
    }

    public int countTrailSegments() {
        return session.selectOne("countTrailSegments");
    }
}
