package com.season.semiproject.spatial.slope;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 12C: length-based coverage measurement, run against the real Docker PostgreSQL/PostGIS
 * data. Chain COUNT alone (e.g. "99 of 404 chains reachable at 20m", Phase 12A/12B) understates
 * coverage for a network with many very short fragments (see docs/09-slope-section-analysis.md)
 * -- this test measures actual network LENGTH coverage instead. Geometry-distance-error
 * measurement moved to SlopeSectionBuildServiceIntegrationTest after SlopeSection became a
 * persisted, window=20-only Derived Analysis Layer (see that class for why).
 *
 * Read-only; asserts nothing destructive. Numbers are also printed so they can be copied into
 * docs/09.
 */
@SpringBootTest
class SlopeCoverageAnalysisTest {

    private static final Map<Long, String> TRAIL_NAMES = Map.of(
            10L, "마루", 11L, "무악동구간", 12L, "홍제동구간", 13L, "부암동구간");
    private static final long[] TRAIL_IDS = { 10, 11, 12, 13 };
    private static final int[] WINDOWS = { 10, 20, 30 };

    @Autowired
    private SlopeSectionDAO dao;

    @Test
    void measureLengthCoverageAndGeometryErrorForAllTrailsAndWindows() {
        Map<Long, List<TrailSegmentElevationRow>> rowsByTrail = new LinkedHashMap<>();
        Map<Long, List<NetworkChain>> chainsByTrail = new LinkedHashMap<>();
        for (long trailId : TRAIL_IDS) {
            List<TrailSegmentElevationRow> rows = dao.findSegmentsForTrail(trailId);
            rowsByTrail.put(trailId, rows);
            chainsByTrail.put(trailId, NetworkChainBuilder.buildChains(rows));
        }

        // ---- Network length and elevation-profile coverage length (window-independent) ----
        double grandNetworkLength = 0;
        double grandElevationCoverage = 0;
        double grandInsufficientSamplesLength = 0;
        double grandOutsideInterpolationLength = 0;

        System.out.println("\n=== Phase 12C: Network length vs Elevation-profile coverage (window-independent) ===");
        for (long trailId : TRAIL_IDS) {
            double networkLength = 0;
            double elevationCoverage = 0;
            double insufficientSamples = 0;
            double outsideInterpolation = 0;
            for (NetworkChain chain : chainsByTrail.get(trailId)) {
                networkLength += chain.getTotalLengthMeters();
                ElevationProfile profile = ElevationProfile.build(chain);
                if (!profile.hasCoverage()) {
                    insufficientSamples += chain.getTotalLengthMeters();
                } else {
                    double covered = profile.getCoverageEndMeters() - profile.getCoverageStartMeters();
                    elevationCoverage += covered;
                    outsideInterpolation += (chain.getTotalLengthMeters() - covered);
                }
            }
            grandNetworkLength += networkLength;
            grandElevationCoverage += elevationCoverage;
            grandInsufficientSamplesLength += insufficientSamples;
            grandOutsideInterpolationLength += outsideInterpolation;
            System.out.printf(
                    "%-10s networkLength=%9.2fm  elevationCoverage=%9.2fm (%.1f%%)  insufficientSamples=%8.2fm  outsideInterpolation=%8.2fm%n",
                    TRAIL_NAMES.get(trailId), networkLength, elevationCoverage,
                    100.0 * elevationCoverage / networkLength, insufficientSamples, outsideInterpolation);

            assertTrue(elevationCoverage <= networkLength + 1e-6);
        }
        System.out.printf("%-10s networkLength=%9.2fm  elevationCoverage=%9.2fm (%.1f%%)  insufficientSamples=%8.2fm  outsideInterpolation=%8.2fm%n",
                "ALL", grandNetworkLength, grandElevationCoverage,
                100.0 * grandElevationCoverage / grandNetworkLength, grandInsufficientSamplesLength, grandOutsideInterpolationLength);

        // ---- Section-covered length per window, vs network and vs elevation coverage ----
        System.out.println("\n=== Phase 12C: Section-covered length per window ===");
        for (int window : WINDOWS) {
            double grandSectionCoverage = 0;
            double grandRemainderDropped = 0;
            System.out.printf("--- window=%dm ---%n", window);
            for (long trailId : TRAIL_IDS) {
                double networkLength = 0;
                double elevationCoverage = 0;
                double sectionCoverage = 0;
                for (NetworkChain chain : chainsByTrail.get(trailId)) {
                    networkLength += chain.getTotalLengthMeters();
                    ElevationProfile profile = ElevationProfile.build(chain);
                    if (!profile.hasCoverage()) {
                        continue;
                    }
                    elevationCoverage += (profile.getCoverageEndMeters() - profile.getCoverageStartMeters());
                    for (SlopeSectionResult r : SlopeSectionCalculator.computeSections(chain, window)) {
                        sectionCoverage += r.getDistanceMeters();
                    }
                }
                double remainderDropped = elevationCoverage - sectionCoverage;
                grandSectionCoverage += sectionCoverage;
                grandRemainderDropped += remainderDropped;
                System.out.printf(
                        "  %-10s network=%9.2fm  elevCoverage=%9.2fm  sectionCoverage=%9.2fm  vsNetwork=%.1f%%  vsElevCoverage=%.1f%%  remainderDropped=%.2fm%n",
                        TRAIL_NAMES.get(trailId), networkLength, elevationCoverage, sectionCoverage,
                        100.0 * sectionCoverage / networkLength, 100.0 * sectionCoverage / elevationCoverage,
                        remainderDropped);
                assertTrue(sectionCoverage <= elevationCoverage + 1e-6);
            }
            System.out.printf("  %-10s sectionCoverage total=%.2fm  vsNetwork=%.1f%%  vsElevCoverage=%.1f%%  remainderDropped total=%.2fm%n",
                    "ALL", grandSectionCoverage, 100.0 * grandSectionCoverage / grandNetworkLength,
                    100.0 * grandSectionCoverage / grandElevationCoverage, grandRemainderDropped);
        }

        // Geometry distance error (declared distanceMeters vs actual haversine length) was
        // measured here across windows 10/20/30 via the old compute-on-request
        // SlopeSectionService.computeSlopeSections(trailId, window). That method no longer
        // exists in that form -- SlopeSectionService is now a persisted-data Query service that
        // only ever serves window=20 (see docs/09-slope-section-analysis.md, "precompute +
        // persistence"). The equivalent check for the one window Production actually serves now
        // lives in SlopeSectionBuildServiceIntegrationTest#persistedGeometryLengthCloselyMatchesDeclaredDistance.

        printUncoveredCauseBreakdown(chainsByTrail, 20);
        printRepresentativeSectionTraces(chainsByTrail, 20);
    }

    private record Traced(long trailId, NetworkChain chain, ElevationProfile profile, SlopeSectionResult section) {
    }

    /** §19/§20: pick one rising, falling, flat, steep and partial SlopeSection at window=20m and
     * print a full human-traceable computation trace, including the two DN samples bracketing
     * each section boundary. Debug/report-only -- none of this is exposed via the API. */
    private void printRepresentativeSectionTraces(Map<Long, List<NetworkChain>> chainsByTrail, int window) {
        System.out.println("\n=== Phase 12C: Representative SlopeSection traces (window=" + window + "m) ===");
        List<Traced> all = new ArrayList<>();
        for (Map.Entry<Long, List<NetworkChain>> entry : chainsByTrail.entrySet()) {
            for (NetworkChain chain : entry.getValue()) {
                ElevationProfile profile = ElevationProfile.build(chain);
                for (SlopeSectionResult r : SlopeSectionCalculator.computeSections(chain, window)) {
                    all.add(new Traced(entry.getKey(), chain, profile, r));
                }
            }
        }

        printOneTrace("RISING (largest positive slope)",
                all.stream().max(java.util.Comparator.comparingDouble(t -> t.section().getEstimatedSlopePercent())));
        printOneTrace("FALLING (largest negative slope)",
                all.stream().min(java.util.Comparator.comparingDouble(t -> t.section().getEstimatedSlopePercent())));
        printOneTrace("FLAT (closest to 0%)",
                all.stream().min(java.util.Comparator.comparingDouble(t -> Math.abs(t.section().getEstimatedSlopePercent()))));
        printOneTrace("STEEP (largest |slope|, non-extreme)",
                all.stream().filter(t -> Math.abs(t.section().getEstimatedSlopePercent()) < 100)
                        .max(java.util.Comparator.comparingDouble(t -> Math.abs(t.section().getEstimatedSlopePercent()))));
        printOneTrace("PARTIAL section example",
                all.stream().filter(t -> t.section().isPartialSection()).findFirst());
    }

    private void printOneTrace(String label, java.util.Optional<Traced> optional) {
        if (optional.isEmpty()) {
            System.out.println(label + ": none found");
            return;
        }
        Traced t = optional.get();
        NetworkChain chain = t.chain();
        ElevationProfile profile = t.profile();
        SlopeSectionResult r = t.section();

        System.out.printf("%n--- %s ---%n", label);
        System.out.printf("trail=%d chainIndex=%d sectionIndex=%d partial=%b%n",
                t.trailId(), chain.getChainIndex(), r.getSectionIndex(), r.isPartialSection());
        System.out.printf("chainRange=[%.3f,%.3f]m distance=%.3fm%n",
                r.getChainStartMeters(), r.getChainEndMeters(), r.getDistanceMeters());
        System.out.printf("estimatedElevationStart=%.3f estimatedElevationEnd=%.3f delta=%.3f slope=%.2f%%%n",
                r.getEstimatedElevationStart(), r.getEstimatedElevationEnd(), r.getEstimatedElevationDelta(),
                r.getEstimatedSlopePercent());

        ElevationSample bracketStart = bracketBefore(profile, r.getChainStartMeters());
        ElevationSample bracketStartNext = bracketAfterOf(profile, bracketStart);
        System.out.printf("bracketing DN samples for start=%.3fm: [featureId=%d dn=%.1f @%.3fm] -> [featureId=%d dn=%.1f @%.3fm]%n",
                r.getChainStartMeters(), bracketStart.getSourceTrailFeatureId(), bracketStart.getSourceDn(),
                bracketStart.getDistanceAlongChainMeters(),
                bracketStartNext.getSourceTrailFeatureId(), bracketStartNext.getSourceDn(),
                bracketStartNext.getDistanceAlongChainMeters());

        ElevationSample bracketEnd = bracketAfter(profile, r.getChainEndMeters());
        ElevationSample bracketEndPrev = bracketBeforeOf(profile, bracketEnd);
        System.out.printf("bracketing DN samples for end=%.3fm:   [featureId=%d dn=%.1f @%.3fm] -> [featureId=%d dn=%.1f @%.3fm]%n",
                r.getChainEndMeters(), bracketEndPrev.getSourceTrailFeatureId(), bracketEndPrev.getSourceDn(),
                bracketEndPrev.getDistanceAlongChainMeters(),
                bracketEnd.getSourceTrailFeatureId(), bracketEnd.getSourceDn(), bracketEnd.getDistanceAlongChainMeters());
    }

    private ElevationSample bracketBefore(ElevationProfile profile, double distance) {
        List<ElevationSample> samples = profile.getSamplesForTrace();
        ElevationSample result = samples.get(0);
        for (ElevationSample s : samples) {
            if (s.getDistanceAlongChainMeters() <= distance + 1e-6) {
                result = s;
            }
        }
        return result;
    }

    private ElevationSample bracketAfter(ElevationProfile profile, double distance) {
        List<ElevationSample> samples = profile.getSamplesForTrace();
        for (ElevationSample s : samples) {
            if (s.getDistanceAlongChainMeters() >= distance - 1e-6) {
                return s;
            }
        }
        return samples.get(samples.size() - 1);
    }

    private ElevationSample bracketAfterOf(ElevationProfile profile, ElevationSample sample) {
        List<ElevationSample> samples = profile.getSamplesForTrace();
        int idx = samples.indexOf(sample);
        return samples.get(Math.min(idx + 1, samples.size() - 1));
    }

    private ElevationSample bracketBeforeOf(ElevationProfile profile, ElevationSample sample) {
        List<ElevationSample> samples = profile.getSamplesForTrace();
        int idx = samples.indexOf(sample);
        return samples.get(Math.max(idx - 1, 0));
    }

    /** §6/§7: classify uncovered network length by cause at the recommended default window, and
     * print the largest few examples per cause with a representative location for manual review. */
    private void printUncoveredCauseBreakdown(Map<Long, List<NetworkChain>> chainsByTrail, int window) {
        System.out.println("\n=== Phase 12C: Uncovered-length cause breakdown (window=" + window + "m) ===");
        record ChainLoss(long trailId, int chainIndex, double lengthMeters, String cause, double[] startCoord) {
        }
        List<ChainLoss> insufficientSamples = new ArrayList<>();
        List<ChainLoss> outsideInterpolation = new ArrayList<>();
        List<ChainLoss> remainderDropped = new ArrayList<>();

        double totalInsufficient = 0, totalOutside = 0, totalRemainder = 0;

        for (Map.Entry<Long, List<NetworkChain>> entry : chainsByTrail.entrySet()) {
            for (NetworkChain chain : entry.getValue()) {
                double[] startCoord = chain.getSegments().get(0).getOrientedCoordinates().get(0);
                ElevationProfile profile = ElevationProfile.build(chain);
                if (!profile.hasCoverage()) {
                    insufficientSamples.add(new ChainLoss(entry.getKey(), chain.getChainIndex(),
                            chain.getTotalLengthMeters(), "INSUFFICIENT_DN_SAMPLES", startCoord));
                    totalInsufficient += chain.getTotalLengthMeters();
                    continue;
                }
                double covered = profile.getCoverageEndMeters() - profile.getCoverageStartMeters();
                double outside = chain.getTotalLengthMeters() - covered;
                if (outside > 1e-6) {
                    outsideInterpolation.add(new ChainLoss(entry.getKey(), chain.getChainIndex(), outside,
                            "OUTSIDE_INTERPOLATION_COVERAGE", startCoord));
                    totalOutside += outside;
                }
                double sectionCovered = SlopeSectionCalculator.computeSections(chain, window).stream()
                        .mapToDouble(SlopeSectionResult::getDistanceMeters).sum();
                double remainder = covered - sectionCovered;
                if (remainder > 1e-6) {
                    remainderDropped.add(new ChainLoss(entry.getKey(), chain.getChainIndex(), remainder,
                            "REMAINDER_BELOW_PARTIAL_THRESHOLD", startCoord));
                    totalRemainder += remainder;
                }
            }
        }

        System.out.printf("totals: INSUFFICIENT_DN_SAMPLES=%.2fm  OUTSIDE_INTERPOLATION_COVERAGE=%.2fm  REMAINDER_BELOW_PARTIAL_THRESHOLD=%.2fm%n",
                totalInsufficient, totalOutside, totalRemainder);

        printTop5(insufficientSamples, ChainLoss::lengthMeters, ChainLoss::trailId, ChainLoss::chainIndex, ChainLoss::startCoord);
        printTop5(outsideInterpolation, ChainLoss::lengthMeters, ChainLoss::trailId, ChainLoss::chainIndex, ChainLoss::startCoord);
        printTop5(remainderDropped, ChainLoss::lengthMeters, ChainLoss::trailId, ChainLoss::chainIndex, ChainLoss::startCoord);

        assertTrue(totalInsufficient >= 0 && totalOutside >= 0 && totalRemainder >= 0);
    }

    private <T> void printTop5(List<T> items, java.util.function.ToDoubleFunction<T> lengthFn,
            java.util.function.ToLongFunction<T> trailFn, java.util.function.ToIntFunction<T> chainFn,
            java.util.function.Function<T, double[]> coordFn) {
        items.sort((a, b) -> Double.compare(lengthFn.applyAsDouble(b), lengthFn.applyAsDouble(a)));
        int limit = Math.min(5, items.size());
        System.out.printf("  top %d of %d items:%n", limit, items.size());
        for (int i = 0; i < limit; i++) {
            T item = items.get(i);
            double[] coord = coordFn.apply(item);
            System.out.printf("    trail=%d chainIndex=%d length=%.2fm startCoord=[%.6f,%.6f]%n",
                    trailFn.applyAsLong(item), chainFn.applyAsInt(item), lengthFn.applyAsDouble(item),
                    coord[0], coord[1]);
        }
    }

}
