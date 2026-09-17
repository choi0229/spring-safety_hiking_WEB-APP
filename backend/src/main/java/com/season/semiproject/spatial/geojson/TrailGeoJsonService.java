package com.season.semiproject.spatial.geojson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Serves the Validated Raw Spatial Layer (`trail_feature`) as a GeoJSON FeatureCollection for
 * ordinary (non-slope) Frontend map rendering -- see docs/01-architecture-and-modeling.md
 * ("Trail GeoJSON API") and docs/06-frontend-api-compatibility.md for the full design.
 *
 * This is intentionally NOT the source for the 4 Legacy Slope Compatibility views
 * (MountainDetailView.vue, CompareCourseView.vue, MountainDetailView2.vue,
 * MobileMountainDetailView.vue) -- those keep reading the original static GeoJSON file. See
 * docs/06-frontend-api-compatibility.md for the measured reason (groupCoordinates() position
 * shift causes large legacy slope color differences when fed this DB-derived source).
 */
@Service
public class TrailGeoJsonService {

    private final TrailGeoJsonDAO dao;

    @Autowired
    public TrailGeoJsonService(TrailGeoJsonDAO dao) {
        this.dao = dao;
    }

    /** Already a complete, valid GeoJSON FeatureCollection JSON string (never null -- see mapper). */
    public String getTrailFeatureCollectionJson() {
        return dao.getTrailFeatureCollectionJson();
    }
}
