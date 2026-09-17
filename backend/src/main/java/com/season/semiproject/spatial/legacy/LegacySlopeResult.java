package com.season.semiproject.spatial.legacy;

import java.util.List;

/**
 * {@code totalGroupCount} is the size of the raw {@code groupCoordinates()}
 * output (including any 1-point remainder group); {@code renderedGroups} is
 * what the legacy frontend actually draws (1-point groups excluded). The two
 * counts differ by exactly one whenever the flattened coordinate count is
 * not a multiple of groupSize and leaves a 1-point remainder.
 */
public record LegacySlopeResult(
        int totalGroupCount,
        List<LegacySlopeGroup> renderedGroups
) {
}
