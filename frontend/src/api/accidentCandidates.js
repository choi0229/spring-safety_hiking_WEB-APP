// Phase 13: shared client for the Trail-level AccidentPoint proximity candidate API
// (GET /api/spatial/trails/{trailId}/nearby-accidents). PostGIS only selects which AccidentPoint
// rows are spatially near a Trail's Network (ST_DWithin/ST_Distance, see
// docs/05-accident-spatial-query.md) -- comparing those candidates against the user's live
// position stays a Frontend/Kakao Maps distance concern (see RecordView.vue).

/**
 * FeatureCollection<Point> of AccidentPoint candidates within distanceMeters of ANY TrailSegment
 * of the given trailId. One Feature per distinct accidentId (deduped server-side).
 */
export async function fetchNearbyAccidents(trailId, distanceMeters) {
  const response = await fetch(`/api/spatial/trails/${trailId}/nearby-accidents?distanceMeters=${distanceMeters}`);
  if (!response.ok) {
    throw new Error(`nearby-accidents request failed: HTTP ${response.status} (trailId=${trailId})`);
  }
  return response.json();
}
