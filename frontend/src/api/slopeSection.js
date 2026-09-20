// Phase 12D: shared client for the Network-based fixed-distance SlopeSection Backend API
// (see backend docs/09-slope-section-analysis.md). Used by the 4 slope visualization screens
// (MountainDetailView, CompareCourseView, MountainDetailView2, MobileMountainDetailView) so all
// of them use the exact same Trail ID mapping, window size and color rule -- client-independent
// by design, replacing the old per-screen groupCoordinates(5/7/12) + calculateSlope().
//
// estimatedSlopePercent is a DEM-derived ESTIMATED slope, not a measured one -- see docs/09.

// Keys are the Trail GeoJSON API's properties.PMNTN_NM values (see docs/09). The `course` DB
// table's course_name is shorter for 3 of 4 courses ("무악동"/"홍제동"/"부암동", no "구간"
// suffix -- verified against the real DB) -- resolveTrailId() below matches the same way the
// legacy PMNTN_NM.includes(courseName) filter always did, so callers should use resolveTrailId()
// rather than indexing this map directly with a `course` table courseName.
export const TRAIL_ID_BY_COURSE_NAME = {
  '마루': 10,
  '무악동구간': 11,
  '홍제동구간': 12,
  '부암동구간': 13,
};

export const SLOPE_WINDOW_METERS = 20;

/**
 * Resolves a Trail ID from a courseName that may come from either source: the Trail GeoJSON's
 * PMNTN_NM (exact match) or the `course` DB table's shorter course_name (substring of a
 * PMNTN_NM). Returns undefined if no known course matches.
 */
export function resolveTrailId(courseName) {
  if (!courseName) {
    return undefined;
  }
  if (TRAIL_ID_BY_COURSE_NAME[courseName] !== undefined) {
    return TRAIL_ID_BY_COURSE_NAME[courseName];
  }
  const matchedPmntnNm = Object.keys(TRAIL_ID_BY_COURSE_NAME).find((pmntnNm) => pmntnNm.includes(courseName));
  return matchedPmntnNm ? TRAIL_ID_BY_COURSE_NAME[matchedPmntnNm] : undefined;
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

/** Full Validated TrailFeature set (1706 Features) as one GeoJSON FeatureCollection. */
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
