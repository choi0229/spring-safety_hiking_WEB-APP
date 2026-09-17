package com.season.semiproject.spatial;

/** Result of structural validation for a single GeoJSON Feature. */
public class FeatureValidationResult {

    private final boolean valid;
    private final String reason;

    private FeatureValidationResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public static FeatureValidationResult valid() {
        return new FeatureValidationResult(true, null);
    }

    public static FeatureValidationResult invalid(String reason) {
        return new FeatureValidationResult(false, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }
}
