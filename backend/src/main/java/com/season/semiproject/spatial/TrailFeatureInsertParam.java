package com.season.semiproject.spatial;

/** MyBatis parameter for one `trail_feature` row. `sequence` is the source-order sequence
 *  defined in docs/02-migration-and-data-quality.md -- NOT a network/traversal order. */
public class TrailFeatureInsertParam {

    private Long trailId;
    private int sequence;
    private int sourceFeatureIndex;
    private String geometryJson;
    private double dnValue;

    public Long getTrailId() {
        return trailId;
    }

    public void setTrailId(Long trailId) {
        this.trailId = trailId;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getSourceFeatureIndex() {
        return sourceFeatureIndex;
    }

    public void setSourceFeatureIndex(int sourceFeatureIndex) {
        this.sourceFeatureIndex = sourceFeatureIndex;
    }

    public String getGeometryJson() {
        return geometryJson;
    }

    public void setGeometryJson(String geometryJson) {
        this.geometryJson = geometryJson;
    }

    public double getDnValue() {
        return dnValue;
    }

    public void setDnValue(double dnValue) {
        this.dnValue = dnValue;
    }
}
