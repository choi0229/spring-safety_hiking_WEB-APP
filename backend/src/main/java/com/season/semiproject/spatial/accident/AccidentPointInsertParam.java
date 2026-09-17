package com.season.semiproject.spatial.accident;

import java.time.LocalDate;

/** MyBatis parameter for one `accident_point` row. */
public class AccidentPointInsertParam {

    private String sourceFile;
    private int sourceFeatureIndex;
    private String reportNo;
    private LocalDate dispatchDate;
    private String accidentType;
    private String locationName;
    private double lng;
    private double lat;
    private String rawPropertiesJson;

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public int getSourceFeatureIndex() {
        return sourceFeatureIndex;
    }

    public void setSourceFeatureIndex(int sourceFeatureIndex) {
        this.sourceFeatureIndex = sourceFeatureIndex;
    }

    public String getReportNo() {
        return reportNo;
    }

    public void setReportNo(String reportNo) {
        this.reportNo = reportNo;
    }

    public LocalDate getDispatchDate() {
        return dispatchDate;
    }

    public void setDispatchDate(LocalDate dispatchDate) {
        this.dispatchDate = dispatchDate;
    }

    public String getAccidentType() {
        return accidentType;
    }

    public void setAccidentType(String accidentType) {
        this.accidentType = accidentType;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public String getRawPropertiesJson() {
        return rawPropertiesJson;
    }

    public void setRawPropertiesJson(String rawPropertiesJson) {
        this.rawPropertiesJson = rawPropertiesJson;
    }
}
