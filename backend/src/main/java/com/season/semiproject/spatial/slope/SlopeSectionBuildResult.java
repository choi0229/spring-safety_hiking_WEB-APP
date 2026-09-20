package com.season.semiproject.spatial.slope;

import java.util.Map;
import java.util.TreeMap;

/** Summary of one Slope Build run, returned for logging by SlopeSectionBuildRunner. */
public class SlopeSectionBuildResult {

    public final int totalSections;
    public final Map<Long, Integer> sectionCountByTrailId;

    public SlopeSectionBuildResult(int totalSections, Map<Long, Integer> sectionCountByTrailId) {
        this.totalSections = totalSections;
        this.sectionCountByTrailId = new TreeMap<>(sectionCountByTrailId);
    }

    @Override
    public String toString() {
        return "SlopeSectionBuildResult{totalSections=" + totalSections
                + ", sectionCountByTrailId=" + sectionCountByTrailId + '}';
    }
}
