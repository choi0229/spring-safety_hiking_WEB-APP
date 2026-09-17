package com.season.semiproject.spatial.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.season.semiproject.spatial.accident.AccidentImportService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-database verification of the AccidentPoint <-> TrailSegment Derived Spatial Relation
 * queries (see mapper-spatial-query.xml), run against the live Docker PostgreSQL/PostGIS
 * instance and the actual imported data. IDs are looked up by business key (report_no) rather
 * than hardcoded, so these tests keep working if the data is re-imported or the Network is
 * rebuilt (see docs/05-accident-spatial-query.md for why TrailSegment ids are not permanent).
 *
 * Read-only: no test here writes to accident_point/trail_segment, so no transactional rollback
 * is needed.
 */
@SpringBootTest
class SpatialQueryIntegrationTest {

    private static final String SOURCE_FILE = "2023산악사고_인왕산.geojson";
    private static final String GEOJSON_PATH = "../frontend/public/data/2023산악사고_인왕산.geojson";

    // The known-nearest pair from Phase 8 pre-analysis: report 20231103201R00346 sits ~1.46m
    // from a TrailSegment on 무악동구간.
    private static final String NEAREST_REPORT_NO = "20231103201R00346";
    // report 20231103201R00037 sits ~525m from its nearest TrailSegment (verified independently
    // via Python Haversine during Phase 8 pre-analysis).
    private static final String FAR_REPORT_NO = "20231103201R00037";

    @Autowired
    private AccidentImportService importService;

    @Autowired
    private SpatialQueryDAO dao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void ensureRealFileExists() {
        assertTrue(new File(GEOJSON_PATH).exists());
    }

    /** Makes sure accident_point actually has the real 42-row dataset before each test class run. */
    private void ensureImported() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new File(GEOJSON_PATH));
        importService.importAccidents(SOURCE_FILE, root);
    }

    private Long accidentIdByReportNo(String reportNo) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM accident_point WHERE source_file = ? AND report_no = ?",
                Long.class, SOURCE_FILE, reportNo);
    }

    @Test
    void nearestKnownAccidentReturnsExpectedDistanceAndCourse() throws Exception {
        ensureImported();
        Long accidentId = accidentIdByReportNo(NEAREST_REPORT_NO);

        List<NearbySegmentRow> results = dao.findSegmentsNearAccident(accidentId, 2.0);

        assertEquals(1, results.size(), "only one Segment should be within 2m of this accident");
        NearbySegmentRow nearest = results.get(0);
        assertEquals("무악동구간", nearest.getSourceCourseName());
        assertEquals(1.46, nearest.getDistanceMeters(), 0.01);
    }

    @Test
    void resultsAreSortedByDistanceAscending() throws Exception {
        ensureImported();
        Long accidentId = accidentIdByReportNo(NEAREST_REPORT_NO);

        List<NearbySegmentRow> results = dao.findSegmentsNearAccident(accidentId, 30.0);
        assertFalse(results.isEmpty());

        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getDistanceMeters() <= results.get(i).getDistanceMeters(),
                    "results must be sorted nearest-first");
        }
    }

    @Test
    void widerThresholdNeverReturnsFewerResults() throws Exception {
        ensureImported();
        Long accidentId = accidentIdByReportNo(FAR_REPORT_NO);

        int at100m = dao.findSegmentsNearAccident(accidentId, 100).size();
        int at300m = dao.findSegmentsNearAccident(accidentId, 300).size();
        int at600m = dao.findSegmentsNearAccident(accidentId, 600).size();

        assertTrue(at100m <= at300m, "widening the radius must never reduce the candidate count");
        assertTrue(at300m <= at600m, "widening the radius must never reduce the candidate count");
    }

    @Test
    void exhaustiveResultsIncludeMatchesOutsideAnySmallKnnWindow() throws Exception {
        // Regression guard against reintroducing a KNN-LIMIT shortcut (see mapper-spatial-query.xml):
        // this accident is known to have 24 distinct Segments within 30m (all short consecutive
        // Parts of the same Feature chain), which exceeds a small fixed KNN candidate window.
        ensureImported();
        Long accidentId = accidentIdByReportNo(NEAREST_REPORT_NO);

        List<NearbySegmentRow> results = dao.findSegmentsNearAccident(accidentId, 30.0);
        assertEquals(24, results.size());
    }

    @Test
    void onlyTwoOfFortyTwoAccidentsHaveAnySegmentWithinThirtyMeters() throws Exception {
        // Regression baseline from Phase 8 pre-analysis: at 30m, only 2 of 42 real accident
        // points have any TrailSegment nearby at all -- most of this dataset's coordinates are
        // hundreds of meters from the trail network. See docs/05-accident-spatial-query.md.
        ensureImported();
        List<Long> allAccidentIds = jdbcTemplate.queryForList(
                "SELECT id FROM accident_point WHERE source_file = ?", Long.class, SOURCE_FILE);
        assertEquals(42, allAccidentIds.size());

        long matchedCount = allAccidentIds.stream()
                .filter(id -> !dao.findSegmentsNearAccident(id, 30.0).isEmpty())
                .count();

        assertEquals(2, matchedCount);
    }

    @Test
    void apiAAndApiBAreSymmetricForTheSamePair() throws Exception {
        ensureImported();
        Long accidentId = accidentIdByReportNo(NEAREST_REPORT_NO);

        List<NearbySegmentRow> nearbySegments = dao.findSegmentsNearAccident(accidentId, 2.0);
        assertEquals(1, nearbySegments.size());
        Long segmentId = nearbySegments.get(0).getSegmentId();

        List<NearbyAccidentRow> nearbyAccidents = dao.findAccidentsNearSegment(segmentId, 2.0);
        assertTrue(nearbyAccidents.stream().anyMatch(row -> row.getAccidentId().equals(accidentId)),
                "the Segment->Accident direction must find the same accident the Accident->Segment direction found");
    }

    @Test
    void nonExistentIdsReturnZeroCounts() {
        assertEquals(0, dao.countAccidentById(999_999_999L));
        assertEquals(0, dao.countSegmentById(999_999_999L));
    }
}
