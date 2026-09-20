package com.season.semiproject.spatial.slope;

import com.season.semiproject.spatial.legacy.LegacyCoordinate;
import com.season.semiproject.spatial.legacy.LegacySlopeCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-database verification of the Slope Build (precompute) pipeline: {@link SlopeSectionBuildService}
 * relocates the exact computation orchestration the old compute-on-request SlopeSectionService
 * used to run per-HTTP-request (NetworkChainBuilder/ElevationProfile/SlopeSectionCalculator/
 * ChainDistanceLocator, all unchanged) and persists the result into `slope_section` instead. See
 * docs/09-slope-section-analysis.md.
 *
 * This is a destructive-and-rebuild test class by design (buildAll() deletes and repopulates the
 * whole table every time it runs) -- that is exactly the build's own documented behavior, not a
 * side effect introduced by testing it. Every test leaves slope_section fully rebuilt and
 * consistent afterward.
 */
@SpringBootTest
class SlopeSectionBuildServiceIntegrationTest {

    private static final int WINDOW = SlopeSectionBuildService.PRODUCTION_WINDOW_METERS;

    @Autowired
    private SlopeSectionBuildService buildService;

    @Autowired
    private SlopeSectionBuildDAO buildDao;

    @Autowired
    private SlopeSectionDAO readDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void buildProducesTheKnownTotalAndPerTrailCounts() {
        SlopeSectionBuildResult result = buildService.buildAll();

        assertEquals(351, result.totalSections);
        assertEquals(Map.of(10L, 251, 11L, 49, 12L, 21, 13L, 30), result.sectionCountByTrailId);
        assertEquals(351, buildDao.countAllSlopeSections());
    }

    @Test
    void everyWindowMIsExactlyProductionWindow() {
        buildService.buildAll();
        Integer distinctWindows = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT window_m) FROM slope_section", Integer.class);
        Integer windowValue = jdbcTemplate.queryForObject(
                "SELECT DISTINCT window_m FROM slope_section", Integer.class);
        assertEquals(1, distinctWindows);
        assertEquals(WINDOW, windowValue);
    }

    @Test
    void geometryIsValidAcrossEveryRow() {
        buildService.buildAll();
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM slope_section", Integer.class);
        Integer validSrid = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM slope_section WHERE ST_SRID(geom) = 4326", Integer.class);
        Integer validType = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM slope_section WHERE GeometryType(geom) = 'LINESTRING'", Integer.class);
        Integer notEmpty = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM slope_section WHERE NOT ST_IsEmpty(geom)", Integer.class);
        Integer isValid = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM slope_section WHERE ST_IsValid(geom)", Integer.class);

        assertEquals(total, validSrid, "every row must be SRID 4326");
        assertEquals(total, validType, "every row must be a LineString");
        assertEquals(total, notEmpty, "no row may have empty geometry");
        assertEquals(total, isValid, "every geometry must be ST_IsValid");
    }

    @Test
    void numericFieldsAreAllFiniteAndSane() {
        buildService.buildAll();
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM slope_section", Integer.class);
        Integer positiveDistance = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM slope_section WHERE distance_m > 0", Integer.class);
        Integer finiteSlope = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM slope_section WHERE estimated_slope_percent = estimated_slope_percent "
                        + "AND estimated_slope_percent NOT IN ('Infinity'::float8, '-Infinity'::float8)",
                Integer.class);
        Integer validFlag = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM slope_section WHERE data_quality_flag IS NULL OR data_quality_flag = 'PARTIAL_SECTION'",
                Integer.class);

        assertEquals(total, positiveDistance);
        assertEquals(total, finiteSlope);
        assertEquals(total, validFlag);
    }

    @Test
    void slopeFormulaHoldsForEveryPersistedRow() {
        buildService.buildAll();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT distance_m, estimated_elevation_delta, estimated_slope_percent FROM slope_section");
        for (Map<String, Object> row : rows) {
            double distance = ((Number) row.get("distance_m")).doubleValue();
            double delta = ((Number) row.get("estimated_elevation_delta")).doubleValue();
            double slope = ((Number) row.get("estimated_slope_percent")).doubleValue();
            assertEquals(delta / distance * 100.0, slope, 1e-6);
        }
    }

    /** Approximates the actual geometry length via haversine and compares it against the
     * declared distance_m, reusing the same formula LegacySlopeCalculator already uses --
     * confirms the Phase 12C per-Segment geometry cut accuracy survives persistence intact. */
    @Test
    void persistedGeometryLengthCloselyMatchesDeclaredDistance() {
        buildService.buildAll();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT distance_m, ST_AsGeoJSON(geom) AS geojson FROM slope_section WHERE data_quality_flag IS NULL");

        double maxRelErrorPercent = 0;
        for (Map<String, Object> row : rows) {
            double declared = ((Number) row.get("distance_m")).doubleValue();
            List<double[]> coords = GeoJsonLineStringParser.parseLineString((String) row.get("geojson"));
            double actual = 0;
            for (int i = 0; i < coords.size() - 1; i++) {
                LegacyCoordinate a = new LegacyCoordinate(coords.get(i)[0], coords.get(i)[1], 0);
                LegacyCoordinate b = new LegacyCoordinate(coords.get(i + 1)[0], coords.get(i + 1)[1], 0);
                actual += LegacySlopeCalculator.haversineMeters(a, b);
            }
            double relErrorPercent = Math.abs(actual - declared) / declared * 100.0;
            maxRelErrorPercent = Math.max(maxRelErrorPercent, relErrorPercent);
        }
        // Phase 12C measured median ~0.18%, p95 ~1.58%, max ~5.34% on the compute-on-request
        // path (see docs/09) -- persistence must not make this worse. 10% is a generous ceiling.
        assertTrue(maxRelErrorPercent < 10.0, "max relative geometry length error was " + maxRelErrorPercent + "%");
    }

    @Test
    void buildIsIdempotentAcrossTwoConsecutiveRuns() {
        SlopeSectionBuildResult first = buildService.buildAll();
        SlopeSectionBuildResult second = buildService.buildAll();

        assertEquals(first.totalSections, second.totalSections);
        assertEquals(first.sectionCountByTrailId, second.sectionCountByTrailId);
        assertEquals(351, buildDao.countAllSlopeSections());

        Integer duplicateGroups = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ("
                        + "SELECT trail_id, window_m, chain_sequence, section_sequence, COUNT(*) c "
                        + "FROM slope_section GROUP BY 1,2,3,4 HAVING COUNT(*) > 1"
                        + ") dup",
                Integer.class);
        assertEquals(0, duplicateGroups, "no (trail_id, window_m, chain_sequence, section_sequence) duplicates allowed");
    }

    @Test
    void failedRebuildInsideOneTransactionLeavesPreviousDataIntact() {
        SlopeSectionBuildResult baseline = buildService.buildAll();
        assertEquals(351, baseline.totalSections);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        // Deliberately violate chk_slope_section_window_m (window_m must be 20) inside the same
        // delete-then-insert transaction shape buildAll() uses, to prove a mid-build failure
        // rolls back the DELETE too -- not just the failing INSERT.
        assertThrows(DataIntegrityViolationException.class, () -> tx.executeWithoutResult(status -> {
            buildDao.deleteAllSlopeSections();
            SlopeSectionInsertParam invalid = new SlopeSectionInsertParam(
                    10L, 0, 0, 99, 20.0, 100.0, 105.0, 5.0, 25.0, null,
                    "LINESTRING(126.99 37.60, 126.991 37.601)");
            buildDao.insertSlopeSections(List.of(invalid));
        }));

        assertEquals(351, buildDao.countAllSlopeSections(),
                "a failed build must leave the previous slope_section content intact, not an empty table");

        // Leave the table in the normal, fully-valid state for any test that runs after this one.
        buildService.buildAll();
        assertEquals(351, buildDao.countAllSlopeSections());
    }

    @Test
    void readDaoStillExposesTheRawComputationQueriesForBuildUseOnly() {
        // Confirms findSegmentsForTrail/cutSections (used only by SlopeSectionBuildService) are
        // still reachable through the shared SlopeSectionDAO -- i.e. the Build/Query split did
        // not accidentally delete the compute-time data access the build depends on.
        List<TrailSegmentElevationRow> rows = readDao.findSegmentsForTrail(13);
        assertFalse(rows.isEmpty());
    }
}
