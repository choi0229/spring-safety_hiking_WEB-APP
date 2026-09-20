package com.season.semiproject.spatial.query;

import java.time.LocalDate;

/**
 * One AccidentPoint candidate returned by "Trail -> nearby Accidents" (API C). One row per
 * distinct accidentId -- {@code distanceToTrailMeters} is the MIN distance across every
 * TrailSegment of the requested Trail within distanceMeters (see mapper-spatial-query.xml,
 * findAccidentCandidatesForTrail), never a specific Segment's distance. Deliberately not a
 * persisted entity -- computed fresh on every request, same as {@link NearbyAccidentRow}.
 */
public class NearbyAccidentCandidateRow {

    private Long accidentId;
    private String reportNo;
    private LocalDate dispatchDate;
    private String accidentType;
    private String locationName;
    private String geometryGeoJson;
    private double distanceToTrailMeters;

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

    public String getGeometryGeoJson() {
        return geometryGeoJson;
    }

    public void setGeometryGeoJson(String geometryGeoJson) {
        this.geometryGeoJson = geometryGeoJson;
    }

    public double getDistanceToTrailMeters() {
        return distanceToTrailMeters;
    }

    public void setDistanceToTrailMeters(double distanceToTrailMeters) {
        this.distanceToTrailMeters = distanceToTrailMeters;
    }
}
