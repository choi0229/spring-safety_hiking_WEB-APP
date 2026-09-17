package com.season.semiproject.spatial.legacy;

import java.util.List;

/**
 * One rendered legacy slope group -- equivalent to a single Kakao Polyline
 * segment drawn by the legacy frontend's {@code addRouteLayer()}. Only
 * groups with more than one coordinate are ever rendered (see
 * {@code if (group.length > 1)} in every one of the four legacy views);
 * 1-point remainder groups never produce a LegacySlopeGroup.
 *
 * This is a transient computation result, not a persisted entity -- Phase 7A
 * stores nothing.
 */
public record LegacySlopeGroup(
        int groupIndex,
        List<LegacyCoordinate> coordinates,
        double legacySlopeValue,
        String color
) {
}
