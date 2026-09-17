package com.season.semiproject.spatial.network;

/** Summary of one network build run, returned for logging by NetworkBuildRunner. */
public class NetworkBuildResult {
    public final int lineParts;
    public final int nodes;
    public final int segments;

    public NetworkBuildResult(int lineParts, int nodes, int segments) {
        this.lineParts = lineParts;
        this.nodes = nodes;
        this.segments = segments;
    }

    @Override
    public String toString() {
        return "NetworkBuildResult{lineParts=" + lineParts + ", nodes=" + nodes + ", segments=" + segments + '}';
    }
}
