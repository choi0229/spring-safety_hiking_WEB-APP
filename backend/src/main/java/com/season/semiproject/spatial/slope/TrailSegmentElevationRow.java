package com.season.semiproject.spatial.slope;

/**
 * One TrailSegment plus its parent TrailFeature's DN value and its own geometry, fetched in a
 * single Mapper query per Trail (see mapper-slope-section.xml, {@code findSegmentsForTrail}) so
 * building a Trail's whole chain graph never issues one query per Segment/Feature/Node (Phase 12B,
 * see docs/09-slope-section-analysis.md).
 *
 * {@code dnValue} is the source GeoJSON {@code properties.DN} carried on the parent TrailFeature --
 * see {@link ElevationSample} for why it is treated as an unverified "DEM-derived representative
 * elevation sample", never as a measured/exact elevation.
 */
public class TrailSegmentElevationRow {

    private Long segmentId;
    private Long fromNodeId;
    private Long toNodeId;
    private double lengthMeters;
    private Long sourceTrailFeatureId;
    private double sourceDn;
    /** Raw {@code ST_AsGeoJSON(geom)} of this Segment, coordinates in from_node -> to_node order. */
    private String geometryGeoJson;

    public Long getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(Long segmentId) {
        this.segmentId = segmentId;
    }

    public Long getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(Long fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public Long getToNodeId() {
        return toNodeId;
    }

    public void setToNodeId(Long toNodeId) {
        this.toNodeId = toNodeId;
    }

    public double getLengthMeters() {
        return lengthMeters;
    }

    public void setLengthMeters(double lengthMeters) {
        this.lengthMeters = lengthMeters;
    }

    public Long getSourceTrailFeatureId() {
        return sourceTrailFeatureId;
    }

    public void setSourceTrailFeatureId(Long sourceTrailFeatureId) {
        this.sourceTrailFeatureId = sourceTrailFeatureId;
    }

    public double getSourceDn() {
        return sourceDn;
    }

    public void setSourceDn(double sourceDn) {
        this.sourceDn = sourceDn;
    }

    public String getGeometryGeoJson() {
        return geometryGeoJson;
    }

    public void setGeometryGeoJson(String geometryGeoJson) {
        this.geometryGeoJson = geometryGeoJson;
    }
}
