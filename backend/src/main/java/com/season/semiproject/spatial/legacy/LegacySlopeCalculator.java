package com.season.semiproject.spatial.legacy;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure re-implementation of the legacy frontend's slope-coloring math --
 * verbatim ports of {@code calculateHaversineDistance}, {@code calculateSlope},
 * {@code getColorBySlope} and {@code groupCoordinates} as found (byte-identical
 * across all four legacy views) in MountainDetailView.vue,
 * CompareCourseView.vue, MountainDetailView2.vue and MobileMountainDetailView.vue.
 *
 * This class does not read GeoJSON and has no Spring/DB dependency, so it can
 * be unit-tested in isolation. Nothing here is a verified physical slope --
 * see {@link #calculateLegacySlope} for why.
 */
public final class LegacySlopeCalculator {

    private static final double EARTH_RADIUS_METERS = 6371e3;
    private static final double MIN_HORIZONTAL_DISTANCE_METERS = 1.0;
    private static final double STEEP_UP_THRESHOLD = 30.0;
    private static final double STEEP_DOWN_THRESHOLD = -15.0;
    private static final String COLOR_STEEP_UP = "#FF4500";
    private static final String COLOR_STEEP_DOWN = "#1E90FF";
    private static final String COLOR_FLAT = "#32CD32";

    private LegacySlopeCalculator() {
    }

    /** Verbatim port of the legacy {@code calculateHaversineDistance()}. */
    public static double haversineMeters(LegacyCoordinate a, LegacyCoordinate b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double deltaLat = Math.toRadians(b.lat() - a.lat());
        double deltaLon = Math.toRadians(b.lng() - a.lng());

        double sinLat = Math.sin(deltaLat / 2);
        double sinLon = Math.sin(deltaLon / 2);
        double h = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Verbatim port of the legacy {@code calculateSlope()}. The result is
     * NOT a standard slope percent (elevation delta / horizontal distance):
     * the legacy formula divides by the diagonal (hypotenuse of horizontal
     * distance and DN delta) instead, and returns 0 whenever the horizontal
     * distance is under 1 meter. Kept unchanged intentionally -- Phase 7A is
     * compatibility, not correction.
     */
    public static double calculateLegacySlope(LegacyCoordinate start, LegacyCoordinate end) {
        double horizontalDistance = haversineMeters(start, end);
        double deltaDn = end.dnValue() - start.dnValue();
        double diagonalDistance = Math.sqrt(horizontalDistance * horizontalDistance + deltaDn * deltaDn);

        if (horizontalDistance < MIN_HORIZONTAL_DISTANCE_METERS) {
            return 0;
        }
        return (deltaDn / diagonalDistance) * 100;
    }

    /** Verbatim port of the legacy {@code getColorBySlope()} thresholds. */
    public static String colorForLegacySlope(double legacySlopeValue) {
        if (legacySlopeValue > STEEP_UP_THRESHOLD) {
            return COLOR_STEEP_UP;
        }
        if (legacySlopeValue < STEEP_DOWN_THRESHOLD) {
            return COLOR_STEEP_DOWN;
        }
        return COLOR_FLAT;
    }

    /**
     * Verbatim port of the legacy {@code groupCoordinates()}: non-overlapping
     * fixed-size chunks in original order; the last chunk keeps whatever
     * remainder is left (including a single coordinate) rather than being
     * dropped or merged into the previous chunk.
     */
    public static List<List<LegacyCoordinate>> groupCoordinates(List<LegacyCoordinate> coordinates, int groupSize) {
        if (groupSize <= 0) {
            throw new IllegalArgumentException("groupSize must be positive: " + groupSize);
        }
        List<List<LegacyCoordinate>> groups = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i += groupSize) {
            int end = Math.min(i + groupSize, coordinates.size());
            groups.add(new ArrayList<>(coordinates.subList(i, end)));
        }
        return groups;
    }

    /**
     * Verbatim port of the legacy {@code addRouteLayer()} render guard
     * (identical in all four views): {@code if (group.length > 1)}. Groups
     * of size 0 or 1 are skipped entirely -- no slope, no color, no output --
     * they are not assigned slope=0.
     */
    public static List<LegacySlopeGroup> toRenderedGroups(List<List<LegacyCoordinate>> groups) {
        List<LegacySlopeGroup> rendered = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            List<LegacyCoordinate> group = groups.get(i);
            if (group.size() > 1) {
                LegacyCoordinate start = group.get(0);
                LegacyCoordinate end = group.get(group.size() - 1);
                double legacySlopeValue = calculateLegacySlope(start, end);
                String color = colorForLegacySlope(legacySlopeValue);
                rendered.add(new LegacySlopeGroup(i, group, legacySlopeValue, color));
            }
        }
        return rendered;
    }
}
