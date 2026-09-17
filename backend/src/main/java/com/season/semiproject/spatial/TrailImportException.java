package com.season.semiproject.spatial;

/**
 * Thrown when the GeoJSON file contains a Feature that is neither valid-and-matched to a
 * declared Trail, nor explicitly accounted for in the manifest's excludedFeatures list.
 * This is intentionally a hard failure -- see TrailImportService for why unexpected anomalies
 * are never silently skipped or coerced.
 */
public class TrailImportException extends RuntimeException {

    public TrailImportException(String message) {
        super(message);
    }
}
