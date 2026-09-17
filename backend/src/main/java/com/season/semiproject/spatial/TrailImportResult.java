package com.season.semiproject.spatial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Summary of one import run, returned for logging by SpatialImportRunner. */
public class TrailImportResult {

    public final String sourceFile;
    public final int totalFeaturesInFile;
    public int includedFeatureCount = 0;
    public final Map<String, Integer> excludedCounts = new LinkedHashMap<>();
    public final List<TrailSummary> trails = new ArrayList<>();

    public TrailImportResult(String sourceFile, int totalFeaturesInFile) {
        this.sourceFile = sourceFile;
        this.totalFeaturesInFile = totalFeaturesInFile;
    }

    public static class TrailSummary {
        public final String pmntnNm;
        public final Long trailId;
        public final Integer courseId;
        public final int featureCount;

        public TrailSummary(String pmntnNm, Long trailId, Integer courseId, int featureCount) {
            this.pmntnNm = pmntnNm;
            this.trailId = trailId;
            this.courseId = courseId;
            this.featureCount = featureCount;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TrailImportResult{sourceFile=").append(sourceFile)
          .append(", totalFeaturesInFile=").append(totalFeaturesInFile)
          .append(", includedFeatureCount=").append(includedFeatureCount)
          .append(", excludedCounts=").append(excludedCounts)
          .append(", trails=[");
        for (TrailSummary t : trails) {
            sb.append("\n  {pmntnNm=").append(t.pmntnNm)
              .append(", trailId=").append(t.trailId)
              .append(", courseId=").append(t.courseId)
              .append(", featureCount=").append(t.featureCount).append('}');
        }
        sb.append("\n]}");
        return sb.toString();
    }
}
