package com.season.semiproject.spatial.query;

import java.time.LocalDate;

/**
 * GeoJSON Feature properties for one Accident candidate. Only fields that already exist on
 * accident_point are exposed here (see accident-schema.sql) -- no property is invented for this
 * API. {@code distanceToTrailMeters} is Trail <-> AccidentPoint distance (PostGIS-computed
 * candidate radius), never the live user <-> AccidentPoint distance -- that stays a Frontend-only
 * concept (Kakao Maps distance, see docs/05-accident-spatial-query.md).
 */
public final class AccidentCandidateProperties {

    private final long accidentId;
    private final String reportNo;
    private final LocalDate dispatchDate;
    private final String accidentType;
    private final String locationName;
    private final double distanceToTrailMeters;

    public AccidentCandidateProperties(long accidentId, String reportNo, LocalDate dispatchDate,
            String accidentType, String locationName, double distanceToTrailMeters) {
        this.accidentId = accidentId;
        this.reportNo = reportNo;
        this.dispatchDate = dispatchDate;
        this.accidentType = accidentType;
        this.locationName = locationName;
        this.distanceToTrailMeters = distanceToTrailMeters;
    }

    public long getAccidentId() {
        return accidentId;
    }

    public String getReportNo() {
        return reportNo;
    }

    public LocalDate getDispatchDate() {
        return dispatchDate;
    }

    public String getAccidentType() {
        return accidentType;
    }

    public String getLocationName() {
        return locationName;
    }

    public double getDistanceToTrailMeters() {
        return distanceToTrailMeters;
    }
}
