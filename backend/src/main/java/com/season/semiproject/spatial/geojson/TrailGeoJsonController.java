package com.season.semiproject.spatial.geojson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trail GeoJSON compatibility API -- returns the full Validated TrailFeature set as one
 * FeatureCollection, in the same property shape (PMNTN_NM/DN) the legacy static GeoJSON file
 * used, so existing non-slope Frontend screens can switch by changing only their fetch URL.
 * See docs/06-frontend-api-compatibility.md.
 *
 * The Mapper already returns a complete JSON string (see mapper-trail-geojson.xml), so this
 * Controller returns it via ResponseEntity&lt;String&gt; with an explicit application/json
 * Content-Type -- this avoids Jackson re-serializing (and thus re-quoting) a string that is
 * already valid JSON.
 */
@RestController
@RequestMapping("/api/spatial/trails")
public class TrailGeoJsonController {

    private final TrailGeoJsonService service;

    @Autowired
    public TrailGeoJsonController(TrailGeoJsonService service) {
        this.service = service;
    }

    @GetMapping(value = "/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTrailGeoJson() {
        String json = service.getTrailFeatureCollectionJson();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }
}
