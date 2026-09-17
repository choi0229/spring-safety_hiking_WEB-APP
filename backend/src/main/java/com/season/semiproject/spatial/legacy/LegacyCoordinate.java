package com.season.semiproject.spatial.legacy;

/**
 * One flattened coordinate as produced by the legacy frontend's
 * {@code processGeoJSON()} (see MountainDetailView.vue). {@code dnValue} is
 * the raw GeoJSON {@code properties.DN} value carried through unchanged --
 * it is NOT a verified elevation (see docs/01-architecture-and-modeling.md).
 * The legacy JS calls this field "elevation"; this type intentionally does
 * not, to avoid restating an unverified physical claim.
 */
public record LegacyCoordinate(double lng, double lat, double dnValue) {
}
