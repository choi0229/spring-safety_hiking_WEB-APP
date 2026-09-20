package com.season.semiproject.spatial.slope;

/**
 * Thrown when the Slope build produces a result that fails an internal sanity check (e.g. the
 * number of rows actually persisted doesn't match the number computed). Never silently ignored --
 * @Transactional rolls back the whole build so the previous slope_section content (if any) is
 * preserved.
 */
public class SlopeSectionBuildException extends RuntimeException {
    public SlopeSectionBuildException(String message) {
        super(message);
    }
}
