package com.season.semiproject.spatial.geojson;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TrailGeoJsonDAO {

    @Autowired
    private SqlSession session;

    /** Single query -- returns the whole FeatureCollection as one JSON string (see mapper-trail-geojson.xml). */
    public String getTrailFeatureCollectionJson() {
        return session.selectOne("getTrailFeatureCollectionJson");
    }
}
