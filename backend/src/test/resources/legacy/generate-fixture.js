// Independent JS reference generator for Phase 7A parity testing.
// Re-implements (verbatim, not calling into) the exact functions found in
// frontend/src/views/MountainDetailView.vue (confirmed byte-identical across
// MountainDetailView.vue / CompareCourseView.vue / MountainDetailView2.vue /
// MobileMountainDetailView.vue except for the groupSize argument).
//
// This script is a one-off verification/regeneration tool, not part of the
// Maven build (no npm dependency added; run manually with plain `node`).
// Its output (legacy-slope-fixture.json, in this same directory) is
// committed as a static Java test fixture so the Java implementation is
// checked against a reference that is NOT derived from the Java code
// itself. Re-run with `node generate-fixture.js` from this directory if the
// source GeoJSON ever changes.

const fs = require('fs');
const path = require('path');

const GEOJSON_PATH = path.resolve(__dirname, '../../../../../frontend/public/data/인왕산ele copy.geojson');

function deg2rad(deg) {
  return deg * (Math.PI / 180);
}

function calculateHaversineDistance(coord1, coord2) {
  const R = 6371e3;
  const lat1 = deg2rad(coord1.lat);
  const lat2 = deg2rad(coord2.lat);
  const deltaLat = deg2rad(coord2.lat - coord1.lat);
  const deltaLon = deg2rad(coord2.lng - coord1.lng);

  const a =
    Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) *
    Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function calculateSlope(start, end) {
  const horizontalDistance = calculateHaversineDistance(
    { lat: start.lat, lng: start.lng },
    { lat: end.lat, lng: end.lng }
  );
  const elevationChange = end.elevation - start.elevation;
  const diagonalDistance = Math.sqrt(
    Math.pow(horizontalDistance, 2) + Math.pow(elevationChange, 2)
  );
  if (horizontalDistance < 1) {
    return 0;
  }
  return (elevationChange / diagonalDistance) * 100;
}

function getColorBySlope(slope) {
  if (slope > 30) return '#FF4500';
  if (slope < -15) return '#1E90FF';
  return '#32CD32';
}

function groupCoordinates(coordinates, groupSize) {
  const groups = [];
  for (let i = 0; i < coordinates.length; i += groupSize) {
    groups.push(coordinates.slice(i, i + groupSize));
  }
  return groups;
}

function processGeoJSON(geojsonData, courseName) {
  let allCoordinates = [];
  geojsonData.features.forEach((feature) => {
    if (feature.properties.PMNTN_NM && feature.properties.PMNTN_NM.includes(courseName)) {
      let coordinates = [];
      if (feature.geometry.type === 'MultiLineString') {
        feature.geometry.coordinates.forEach(line => {
          coordinates = coordinates.concat(line.map((coord) => ({
            lng: coord[0],
            lat: coord[1],
            elevation: feature.properties.DN || 0
          })));
        });
      } else if (feature.geometry.type === 'LineString') {
        coordinates = feature.geometry.coordinates.map((coord) => ({
          lng: coord[0],
          lat: coord[1],
          elevation: feature.properties.DN || 0
        }));
      }
      allCoordinates = allCoordinates.concat(coordinates);
    }
  });
  return allCoordinates;
}

const geojsonData = JSON.parse(fs.readFileSync(GEOJSON_PATH, 'utf-8'));

const courseNames = ['마루', '무악동', '홍제동', '부암동'];
const groupSizes = [5, 7, 12];

const fixture = { cases: [] };

for (const courseName of courseNames) {
  const allCoordinates = processGeoJSON(geojsonData, courseName);
  for (const groupSize of groupSizes) {
    const groups = groupCoordinates(allCoordinates, groupSize);
    const renderedGroups = [];
    groups.forEach((group, groupIndex) => {
      if (group.length > 1) {
        const startPoint = group[0];
        const endPoint = group[group.length - 1];
        const slope = calculateSlope(startPoint, endPoint);
        const color = getColorBySlope(slope);
        const horizontalDistance = calculateHaversineDistance(startPoint, endPoint);
        const deltaDn = endPoint.elevation - startPoint.elevation;
        renderedGroups.push({
          groupIndex,
          size: group.length,
          start: { lng: startPoint.lng, lat: startPoint.lat, dn: startPoint.elevation },
          end: { lng: endPoint.lng, lat: endPoint.lat, dn: endPoint.elevation },
          horizontalDistance,
          deltaDn,
          legacySlopeValue: slope,
          color
        });
      }
    });

    const colorCounts = { '#FF4500': 0, '#1E90FF': 0, '#32CD32': 0 };
    renderedGroups.forEach(g => { colorCounts[g.color]++; });

    fixture.cases.push({
      courseName,
      groupSize,
      totalCoordinateCount: allCoordinates.length,
      totalGroupCount: groups.length,
      renderedGroupCount: renderedGroups.length,
      colorCounts,
      renderedGroups
    });

    console.log(`${courseName} / groupSize=${groupSize}: coords=${allCoordinates.length} totalGroups=${groups.length} rendered=${renderedGroups.length} colors=${JSON.stringify(colorCounts)}`);
  }
}

const OUT_PATH = path.resolve(__dirname, 'legacy-slope-fixture.json');
fs.writeFileSync(OUT_PATH, JSON.stringify(fixture));
console.log('Wrote fixture to', OUT_PATH);
