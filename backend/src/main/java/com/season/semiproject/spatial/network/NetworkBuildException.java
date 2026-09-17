package com.season.semiproject.spatial.network;

/**
 * Thrown when the Network build produces a result that fails an internal sanity check (e.g. a
 * LineString Part whose endpoint didn't resolve to any TrailNode). Never silently ignored --
 * @Transactional rolls back the whole build so the previous network (if any) is preserved.
 */
public class NetworkBuildException extends RuntimeException {
    public NetworkBuildException(String message) {
        super(message);
    }
}
