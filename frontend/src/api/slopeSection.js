// Phase 12D: shared client for the Network-based fixed-distance SlopeSection Backend API
// (see backend docs/09-slope-section-analysis.md). Used by the 4 slope visualization screens
// (MountainDetailView, CompareCourseView, MountainDetailView2, MobileMountainDetailView) so all
// of them use the exact same window size and color rule -- client-independent by design,
// replacing the old per-screen groupCoordinates(5/7/12) + calculateSlope().
//
// estimatedSlopePercent is a DEM-derived ESTIMATED slope, not a measured one -- see docs/09.

export const SLOPE_WINDOW_METERS = 20;

// `trail.id` is a PostgreSQL surrogate PK, not a domain constant -- it is whatever value a given
// database happened to assign on import (verified: the operating DB currently has 마루=10, a
// Fresh Rebuild produced 마루=1). This module MUST NOT hardcode those values (see docs/09,
// "trail.id is a surrogate PK"). Name-only compatibility between the `course` DB table's
// courseName ("무악동", no "구간" suffix -- verified against the real DB) and the Trail GeoJSON
// API's properties.PMNTN_NM ("무악동구간") is still needed, so that alias stays -- it maps
// names to names, never a name to a database id.
const COURSE_NAME_TO_SOURCE_COURSE_NAME = {
  '마루': '마루',
  '무악동': '무악동구간',
  '홍제동': '홍제동구간',
  '부암동': '부암동구간',
};

/**
 * Resolves the current database's Trail ID for a courseName, using only data the Backend
 * actually returned in `trailGeoJson` (see fetchTrailGeoJson()) -- never a hardcoded id and
 * never a fallback default. `trailGeoJson.features[].properties.trailId` is the Source of Truth
 * (see docs/09-slope-section-analysis.md); `courseName` may come from either the Trail GeoJSON's
 * own PMNTN_NM (exact match) or the shorter `course` DB table name (resolved through the alias
 * table above -- name-only, never id-only).
 *
 * Throws (never returns undefined/null and never silently picks a value) if courseName does not
 * resolve to a source course name that appears in trailGeoJson, or if that source course name
 * maps to more than one distinct trailId -- both are data inconsistencies the caller must not
 * paper over.
 */
export function resolveTrailId(courseName, trailGeoJson) {
  if (!courseName) {
    throw new Error('resolveTrailId: courseName is required');
  }
  if (!trailGeoJson || !Array.isArray(trailGeoJson.features)) {
    throw new Error('resolveTrailId: a Trail GeoJSON FeatureCollection (see fetchTrailGeoJson()) is required');
  }

  const sourceCourseName = COURSE_NAME_TO_SOURCE_COURSE_NAME[courseName] || courseName;
  const trailIds = new Set(
    trailGeoJson.features
      .filter((feature) => feature.properties && feature.properties.PMNTN_NM === sourceCourseName)
      .map((feature) => feature.properties.trailId)
  );

  if (trailIds.size === 0) {
    throw new Error(
        `resolveTrailId: no Trail found for courseName="${courseName}" (resolved sourceCourseName="${sourceCourseName}")`);
  }
  if (trailIds.size > 1) {
    throw new Error(
        `resolveTrailId: courseName="${courseName}" resolved to multiple distinct trailId values `
        + `[${[...trailIds].join(', ')}] -- this is a data inconsistency, not something to guess past`);
  }
  return trailIds.values().next().value;
}

/**
 * Every distinct trailId actually present in trailGeoJson, for the one screen
 * (MountainDetailView2.vue) that renders all courses at once instead of a single selected one --
 * also derived from Backend metadata only, never a hardcoded id list.
 */
export function resolveAllTrailIds(trailGeoJson) {
  if (!trailGeoJson || !Array.isArray(trailGeoJson.features)) {
    throw new Error('resolveAllTrailIds: a Trail GeoJSON FeatureCollection (see fetchTrailGeoJson()) is required');
  }
  const trailIds = new Set(
      trailGeoJson.features
          .map((feature) => feature.properties && feature.properties.trailId)
          .filter((trailId) => trailId !== undefined && trailId !== null));
  return [...trailIds];
}

// Thresholds chosen from the real 20m estimatedSlopePercent distribution (see docs/09, Phase
// 12C section 21-23): |slope| p90 is roughly 40%, so this flags only the steepest ~10% of
// sections in each direction. This is a presentation rule for this UI, not a validated hiking
// difficulty grade, and it is NOT the old Legacy threshold (30 / -15) -- the two slope formulas
// are not the same quantity (see docs/04-legacy-slope-compatibility.md), so their thresholds
// are not comparable.
const STEEP_UP_THRESHOLD = 40;
const STEEP_DOWN_THRESHOLD = -40;

const COLOR_STEEP_UP = '#FF4500';
const COLOR_STEEP_DOWN = '#1E90FF';
const COLOR_MODERATE = '#32CD32';

/**
 * Color for one SlopeSection's estimatedSlopePercent. Sign is preserved (positive = the chain's
 * canonical traversal direction goes up, negative = down) -- matching the old getColorBySlope()'s
 * direction-aware behavior.
 *
 * Returns `null` (never a color) for anything that is not a finite number
 * (undefined/null/NaN/Infinity/-Infinity) -- an invalid/missing slope is NOT the same thing as a
 * measured ~0% slope, so it must never be drawn as the moderate/flat color. Callers must skip
 * drawing a Slope Overlay Feature when this returns null; the Base Trail Layer already shows
 * that geometry (see docs/09-slope-section-analysis.md).
 */
export function getEstimatedSlopeColor(estimatedSlopePercent) {
  if (typeof estimatedSlopePercent !== 'number' || !Number.isFinite(estimatedSlopePercent)) {
    return null;
  }
  if (estimatedSlopePercent > STEEP_UP_THRESHOLD) return COLOR_STEEP_UP;
  if (estimatedSlopePercent < STEEP_DOWN_THRESHOLD) return COLOR_STEEP_DOWN;
  return COLOR_MODERATE;
}

/** Full Validated TrailFeature set (1706 Features) as one GeoJSON FeatureCollection. Each
 * Feature's properties now include `trailId` (the current database's `trail.id`) -- the Source
 * of Truth resolveTrailId()/resolveAllTrailIds() read from. */
export async function fetchTrailGeoJson() {
  const response = await fetch('/api/spatial/trails/geojson');
  if (!response.ok) {
    throw new Error(`trails/geojson request failed: HTTP ${response.status}`);
  }
  return response.json();
}

/** 20m (default) SlopeSection GeoJSON FeatureCollection for one Trail. */
export async function fetchSlopeSections(trailId, windowMeters = SLOPE_WINDOW_METERS) {
  const response = await fetch(`/api/spatial/trails/${trailId}/slope-sections?windowMeters=${windowMeters}`);
  if (!response.ok) {
    throw new Error(`slope-sections request failed: HTTP ${response.status}`);
  }
  return response.json();
}
