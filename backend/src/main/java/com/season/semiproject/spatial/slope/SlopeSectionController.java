package com.season.semiproject.spatial.slope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 12B verification API: Network-based fixed-distance SlopeSection GeoJSON, computed on
 * request from TrailSegment/TrailNode + TrailFeature.dn_value -- see
 * docs/09-slope-section-analysis.md. Not yet wired into any Frontend screen; the 4 Legacy slope
 * views are untouched (see docs/04-legacy-slope-compatibility.md).
 */
@RestController
@RequestMapping("/api/spatial/trails")
public class SlopeSectionController {

    private final SlopeSectionService service;

    @Autowired
    public SlopeSectionController(SlopeSectionService service) {
        this.service = service;
    }

    @GetMapping("/{trailId}/slope-sections")
    public ResponseEntity<?> slopeSections(
            @PathVariable long trailId,
            @RequestParam int windowMeters) {
        if (!SlopeSectionService.ALLOWED_WINDOW_METERS.contains(windowMeters)) {
            return ResponseEntity.badRequest()
                    .body("windowMeters must be one of " + SlopeSectionService.ALLOWED_WINDOW_METERS);
        }
        if (!service.trailExists(trailId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service.computeSlopeSections(trailId, windowMeters));
    }
}
