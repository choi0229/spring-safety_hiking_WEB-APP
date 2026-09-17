package com.season.semiproject.spatial.query;

import java.time.LocalDate;

/**
 * One AccidentPoint candidate returned by "Segment -> nearby Accidents" (API B). Deliberately not
 * a persisted entity -- this is a Derived Spatial Relation, computed fresh on every request (see
 * mapper-spatial-query.xml).
 */
public class NearbyAccidentRow {

    private Long accidentId;
    private String reportNo;
    private LocalDate dispatchDate;
    private String accidentType;
    private String locationName;
    private double distanceMeters;

    public Long getAccidentId() {
        return accidentId;
    }

    public void setAccidentId(Long accidentId) {
        this.accidentId = accidentId;
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

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
}
