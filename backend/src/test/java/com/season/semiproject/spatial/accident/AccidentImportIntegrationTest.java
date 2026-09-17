package com.season.semiproject.spatial.accident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-database verification against the live Docker PostgreSQL/PostGIS instance (see
 * docs/02-migration-and-data-quality.md, "Accident Raw Data Import"). Every test method is
 * wrapped in a transaction that Spring's test framework rolls back afterward, so none of these
 * tests leave any permanent change -- whatever accident_point state existed before this class ran
 * (e.g. from a real `spatial-accident-import` run) is restored after each test.
 *
 * Requires accident-schema.sql to already be applied and the real source GeoJSON file to be
 * reachable at the same relative path AccidentImportRunner uses by default.
 */
@SpringBootTest
@Transactional
class AccidentImportIntegrationTest {

    private static final String SOURCE_FILE = "2023산악사고_인왕산.geojson";
    private static final String GEOJSON_PATH = "../frontend/public/data/2023산악사고_인왕산.geojson";

    @Autowired
    private AccidentImportService importService;

    @Autowired
    private AccidentImportDAO dao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode loadRealGeoJson() throws Exception {
        return mapper.readTree(new File(GEOJSON_PATH));
    }

    private int countAccidentPoints() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accident_point WHERE source_file = ?", Integer.class, SOURCE_FILE);
        return count == null ? 0 : count;
    }

    @Test
    void importingRealFileYields42Rows() throws Exception {
        JsonNode root = loadRealGeoJson();

        AccidentImportResult result = importService.importAccidents(SOURCE_FILE, root);

        assertEquals(42, result.totalFeaturesInFile);
        assertEquals(42, result.importedCount);
        assertEquals(42, countAccidentPoints());
    }

    @Test
    void reimportIsIdempotent() throws Exception {
        JsonNode root = loadRealGeoJson();

        importService.importAccidents(SOURCE_FILE, root);
        assertEquals(42, countAccidentPoints());

        importService.importAccidents(SOURCE_FILE, root);
        assertEquals(42, countAccidentPoints(), "re-running the import must not duplicate rows");
    }

    @Test
    void uniqueConstraintRejectsDuplicateSourceFeatureIndex() throws Exception {
        importService.importAccidents(SOURCE_FILE, loadRealGeoJson());

        AccidentPointInsertParam duplicate = new AccidentPointInsertParam();
        duplicate.setSourceFile(SOURCE_FILE);
        duplicate.setSourceFeatureIndex(0); // already used by the real import above
        duplicate.setReportNo("FAKE-DUPLICATE");
        duplicate.setDispatchDate(LocalDate.of(2099, 1, 1));
        duplicate.setAccidentType("테스트");
        duplicate.setLocationName("테스트동");
        duplicate.setLng(126.9);
        duplicate.setLat(37.5);
        duplicate.setRawPropertiesJson("{}");

        assertThrows(DataIntegrityViolationException.class, () -> dao.insertAccidentPoint(duplicate));
    }

    @Test
    void duplicateCoordinateAccidentsAreStoredAsSeparateRows() throws Exception {
        importService.importAccidents(SOURCE_FILE, loadRealGeoJson());

        // Known ground truth for this exact source file (see docs/02): 10 groups of Features
        // share exact coordinates with a different report_no, covering 21 of the 42 rows.
        List<Map<String, Object>> dupGroups = jdbcTemplate.queryForList(
                "SELECT geom, COUNT(*) AS cnt FROM accident_point WHERE source_file = ? "
                        + "GROUP BY geom HAVING COUNT(*) > 1", SOURCE_FILE);
        assertEquals(10, dupGroups.size());

        Integer rowsInDupGroups = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accident_point WHERE source_file = ? AND geom IN ("
                        + "  SELECT geom FROM accident_point WHERE source_file = ? GROUP BY geom HAVING COUNT(*) > 1"
                        + ")", Integer.class, SOURCE_FILE, SOURCE_FILE);
        assertEquals(21, rowsInDupGroups);
    }

    @Test
    void geometryIsValidPointWithSrid4326() throws Exception {
        importService.importAccidents(SOURCE_FILE, loadRealGeoJson());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ST_SRID(geom) AS srid, GeometryType(geom) AS gtype, "
                        + "ST_IsValid(geom) AS valid, ST_IsEmpty(geom) AS empty "
                        + "FROM accident_point WHERE source_file = ?", SOURCE_FILE);

        assertEquals(42, rows.size());
        for (Map<String, Object> row : rows) {
            assertEquals(4326, row.get("srid"));
            assertEquals("POINT", row.get("gtype"));
            assertEquals(true, row.get("valid"));
            assertEquals(false, row.get("empty"));
        }
    }

    @Test
    void rawPropertiesPreservesFullOriginalPropertiesObject() throws Exception {
        JsonNode root = loadRealGeoJson();
        importService.importAccidents(SOURCE_FILE, root);

        JsonNode expectedProperties = null;
        for (JsonNode feature : root.get("features")) {
            if ("20231103201R00037".equals(feature.get("properties").get("msfrtn_resc_reprt_no").asText())) {
                expectedProperties = feature.get("properties");
                break;
            }
        }
        assertNotNull(expectedProperties);

        String rawJson = jdbcTemplate.queryForObject(
                "SELECT raw_properties::text FROM accident_point WHERE source_file = ? AND report_no = ?",
                String.class, SOURCE_FILE, "20231103201R00037");

        JsonNode actualProperties = mapper.readTree(rawJson);
        assertEquals(expectedProperties, actualProperties);
    }

    @Test
    void rollbackLeavesNoPartialDataOnValidationFailure() throws Exception {
        int before = countAccidentPoints();

        JsonNode badRoot = mapper.readTree("""
            {"type":"FeatureCollection","features":[
               {"type":"Feature",
                "properties":{"msfrtn_resc_reprt_no":"OK-1","dsp_ymd":20230115,
                               "acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동"},
                "geometry":{"type":"Point","coordinates":[126.96,37.58]}},
               {"type":"Feature",
                "properties":{"msfrtn_resc_reprt_no":"BAD-2"},
                "geometry":{"type":"Point","coordinates":[126.97,37.59]}}
             ]}
            """);

        assertThrows(AccidentImportException.class,
                () -> importService.importAccidents(SOURCE_FILE, badRoot));

        assertEquals(before, countAccidentPoints(),
                "a failed import must leave the previous source_file state completely untouched");
    }
}
