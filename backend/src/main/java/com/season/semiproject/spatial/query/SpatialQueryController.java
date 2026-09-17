package com.season.semiproject.spatial.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Phase 8 spatial proximity API between AccidentPoint (Raw) and TrailSegment (Derived Network).
 * `distanceMeters` is always a caller-chosen search radius -- this API never hardcodes or implies
 * a "risk radius" (see docs/05-accident-spatial-query.md for why 30m/any fixed value is not
 * treated as a domain-meaningful threshold). Not yet wired into any Frontend view (Phase 9).
 */
@RestController
@RequestMapping("/api/spatial")
public class SpatialQueryController {

    private final SpatialQueryService service;

    @Autowired
    public SpatialQueryController(SpatialQueryService service) {
        this.service = service;
    }

    /** API A: Accident -> nearby TrailSegments, all candidates within distanceMeters, nearest first. */
    @GetMapping("/accidents/{accidentId}/nearby-segments")
    public ResponseEntity<?> nearbySegments(
            @PathVariable Long accidentId,
            @RequestParam double distanceMeters) {
        if (distanceMeters <= 0) {
            return ResponseEntity.badRequest().body("distanceMeters must be greater than 0");
        }
        if (!service.accidentExists(accidentId)) {
            return ResponseEntity.notFound().build();
        }
        List<NearbySegmentRow> result = service.findSegmentsNearAccident(accidentId, distanceMeters);
        return ResponseEntity.ok(result);
    }

    /** API B: TrailSegment -> nearby AccidentPoints, all candidates within distanceMeters, nearest first. */
    @GetMapping("/trail-segments/{segmentId}/nearby-accidents")
    public ResponseEntity<?> nearbyAccidents(
            @PathVariable Long segmentId,
            @RequestParam double distanceMeters) {
        if (distanceMeters <= 0) {
            return ResponseEntity.badRequest().body("distanceMeters must be greater than 0");
        }
        if (!service.segmentExists(segmentId)) {
            return ResponseEntity.notFound().build();
        }
        List<NearbyAccidentRow> result = service.findAccidentsNearSegment(segmentId, distanceMeters);
        return ResponseEntity.ok(result);
    }
}
