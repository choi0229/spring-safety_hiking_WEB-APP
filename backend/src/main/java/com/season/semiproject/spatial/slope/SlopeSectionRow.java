package com.season.semiproject.spatial.slope;

/** One persisted `slope_section` row, read back by the Query-time {@link SlopeSectionService}. */
public class SlopeSectionRow {

    private long id;
    private long trailId;
    private int chainSequence;
    private int sectionSequence;
    private int windowM;
    private double distanceM;
    private double estimatedElevationStart;
    private double estimatedElevationEnd;
    private double estimatedElevationDelta;
    private double estimatedSlopePercent;
    private String dataQualityFlag;
    private String geometryGeoJson;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTrailId() {
        return trailId;
    }

    public void setTrailId(long trailId) {
        this.trailId = trailId;
    }

    public int getChainSequence() {
        return chainSequence;
    }

    public void setChainSequence(int chainSequence) {
        this.chainSequence = chainSequence;
    }

    public int getSectionSequence() {
        return sectionSequence;
    }

    public void setSectionSequence(int sectionSequence) {
        this.sectionSequence = sectionSequence;
    }

    public int getWindowM() {
        return windowM;
    }

    public void setWindowM(int windowM) {
        this.windowM = windowM;
    }

    public double getDistanceM() {
        return distanceM;
    }

    public void setDistanceM(double distanceM) {
        this.distanceM = distanceM;
    }

    public double getEstimatedElevationStart() {
        return estimatedElevationStart;
    }

    public void setEstimatedElevationStart(double estimatedElevationStart) {
        this.estimatedElevationStart = estimatedElevationStart;
    }

    public double getEstimatedElevationEnd() {
        return estimatedElevationEnd;
    }

    public void setEstimatedElevationEnd(double estimatedElevationEnd) {
        this.estimatedElevationEnd = estimatedElevationEnd;
    }

    public double getEstimatedElevationDelta() {
        return estimatedElevationDelta;
    }

    public void setEstimatedElevationDelta(double estimatedElevationDelta) {
        this.estimatedElevationDelta = estimatedElevationDelta;
    }

    public double getEstimatedSlopePercent() {
        return estimatedSlopePercent;
    }

    public void setEstimatedSlopePercent(double estimatedSlopePercent) {
        this.estimatedSlopePercent = estimatedSlopePercent;
    }

    public String getDataQualityFlag() {
        return dataQualityFlag;
    }

    public void setDataQualityFlag(String dataQualityFlag) {
        this.dataQualityFlag = dataQualityFlag;
    }

    public String getGeometryGeoJson() {
        return geometryGeoJson;
    }

    public void setGeometryGeoJson(String geometryGeoJson) {
        this.geometryGeoJson = geometryGeoJson;
    }
}
