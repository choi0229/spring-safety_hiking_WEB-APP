package com.season.semiproject.spatial.slope;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * A piecewise-linear "DEM-derived estimated elevation" profile along one {@link NetworkChain},
 * built from that chain's {@link ElevationSample}s. Deliberately never extrapolates: any distance
 * outside {@code [coverageStartMeters, coverageEndMeters]} has no defined elevation (see
 * docs/09-slope-section-analysis.md, "Sample 외삽 금지") -- SlopeSections are only ever generated
 * inside the covered range.
 */
public final class ElevationProfile {

    private final List<ElevationSample> samples;

    private ElevationProfile(List<ElevationSample> samples) {
        this.samples = samples;
    }

    /**
     * Builds one profile per chain from its Segment usages, grouping by
     * (sourceTrailFeatureId) and placing one sample at the length-weighted midpoint of that
     * Feature's Segments within this chain. Samples landing at (numerically) identical distances
     * are merged by averaging their DN -- an edge case not seen in the current dataset.
     */
    public static ElevationProfile build(NetworkChain chain) {
        Map<Long, double[]> spanByFeature = new HashMap<>(); // featureId -> {minStart, maxEnd, dn}
        for (ChainSegmentUsage usage : chain.getSegments()) {
            spanByFeature.compute(usage.getSourceTrailFeatureId(), (id, existing) -> {
                if (existing == null) {
                    return new double[] { usage.getCumulativeStartMeters(), usage.getCumulativeEndMeters(),
                            usage.getSourceDn() };
                }
                existing[0] = Math.min(existing[0], usage.getCumulativeStartMeters());
                existing[1] = Math.max(existing[1], usage.getCumulativeEndMeters());
                return existing;
            });
        }

        List<ElevationSample> raw = new ArrayList<>();
        spanByFeature.forEach((featureId, span) -> {
            double distance = (span[0] + span[1]) / 2.0;
            raw.add(new ElevationSample(featureId, distance, span[2]));
        });
        raw.sort(Comparator.comparingDouble(ElevationSample::getDistanceAlongChainMeters));

        List<ElevationSample> merged = new ArrayList<>();
        for (ElevationSample sample : raw) {
            if (!merged.isEmpty()
                    && Double.compare(merged.get(merged.size() - 1).getDistanceAlongChainMeters(),
                            sample.getDistanceAlongChainMeters()) == 0) {
                ElevationSample prev = merged.remove(merged.size() - 1);
                double avgDn = (prev.getSourceDn() + sample.getSourceDn()) / 2.0;
                merged.add(new ElevationSample(prev.getSourceTrailFeatureId(),
                        prev.getDistanceAlongChainMeters(), avgDn));
            } else {
                merged.add(sample);
            }
        }
        return new ElevationProfile(merged);
    }

    public boolean hasCoverage() {
        return samples.size() >= 2;
    }

    /**
     * Debug/trace helper only (Phase 12C, §20) -- lets tests print which two DN samples bracket
     * a given SlopeSection's boundary, for human-traceable verification. Never exposed through
     * the production API/DTOs.
     */
    public List<ElevationSample> getSamplesForTrace() {
        return samples;
    }

    public double getCoverageStartMeters() {
        return samples.get(0).getDistanceAlongChainMeters();
    }

    public double getCoverageEndMeters() {
        return samples.get(samples.size() - 1).getDistanceAlongChainMeters();
    }

    public int getSampleCount() {
        return samples.size();
    }

    /** Empty if {@code distanceMeters} is outside the covered range or there are &lt;2 samples. */
    public OptionalDouble elevationAt(double distanceMeters) {
        if (!hasCoverage()) {
            return OptionalDouble.empty();
        }
        double eps = 1e-6;
        if (distanceMeters < getCoverageStartMeters() - eps || distanceMeters > getCoverageEndMeters() + eps) {
            return OptionalDouble.empty();
        }
        for (int i = 0; i < samples.size() - 1; i++) {
            ElevationSample a = samples.get(i);
            ElevationSample b = samples.get(i + 1);
            if (distanceMeters >= a.getDistanceAlongChainMeters() - eps
                    && distanceMeters <= b.getDistanceAlongChainMeters() + eps) {
                double span = b.getDistanceAlongChainMeters() - a.getDistanceAlongChainMeters();
                if (span <= eps) {
                    return OptionalDouble.of(a.getSourceDn());
                }
                double fraction = (distanceMeters - a.getDistanceAlongChainMeters()) / span;
                return OptionalDouble.of(a.getSourceDn() + fraction * (b.getSourceDn() - a.getSourceDn()));
            }
        }
        return OptionalDouble.empty();
    }
}
