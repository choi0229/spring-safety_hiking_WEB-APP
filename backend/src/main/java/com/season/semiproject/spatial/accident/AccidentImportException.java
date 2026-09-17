package com.season.semiproject.spatial.accident;

/**
 * Thrown when the Accident GeoJSON file contains anything unexpected (wrong root type, wrong
 * CRS, a Feature that fails structural validation, etc.). Intentionally a hard failure -- see
 * AccidentImportService for why anomalies are never silently skipped or coerced.
 */
public class AccidentImportException extends RuntimeException {

    public AccidentImportException(String message) {
        super(message);
    }
}
