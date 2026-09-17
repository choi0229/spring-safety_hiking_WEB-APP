package com.season.semiproject.spatial.accident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pure orchestration tests using a mocked DAO -- exercises validation/abort/param-building logic
 * without needing a database. Real database behavior (constraints, parity, idempotency) is
 * verified against the live Docker PostgreSQL/PostGIS instance by AccidentImportIntegrationTest.
 */
class AccidentImportServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode parse(String json) throws Exception {
        return mapper.readTree(json);
    }

    @Test
    void importsAllValidFeaturesPreservingRawPropertiesAndOrder() throws Exception {
        AccidentImportDAO dao = mock(AccidentImportDAO.class);
        AccidentImportService service = new AccidentImportService(dao);

        JsonNode root = parse("""
            {"type":"FeatureCollection",
             "crs":{"type":"name","properties":{"name":"urn:ogc:def:crs:OGC:1.3:CRS84"}},
             "features":[
               {"type":"Feature",
                "properties":{"msfrtn_resc_reprt_no":"REPORT-A","dsp_ymd":20230115,
                               "acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동","extra_field":"kept"},
                "geometry":{"type":"Point","coordinates":[126.96,37.58]}},
               {"type":"Feature",
                "properties":{"msfrtn_resc_reprt_no":"REPORT-B","dsp_ymd":20230116,
                               "acdnt_cause_asort_nm":"질환","emd_nm":"홍제동"},
                "geometry":{"type":"Point","coordinates":[126.97,37.59]}}
             ]}
            """);

        AccidentImportResult result = service.importAccidents("test.geojson", root);

        assertEquals(2, result.totalFeaturesInFile);
        assertEquals(2, result.importedCount);

        InOrder order = inOrder(dao);
        order.verify(dao).deleteAccidentPointsBySourceFile("test.geojson");

        ArgumentCaptor<AccidentPointInsertParam> captor = ArgumentCaptor.forClass(AccidentPointInsertParam.class);
        order.verify(dao, times(2)).insertAccidentPoint(captor.capture());

        List<AccidentPointInsertParam> params = captor.getAllValues();
        assertEquals(0, params.get(0).getSourceFeatureIndex());
        assertEquals("REPORT-A", params.get(0).getReportNo());
        assertEquals(LocalDate.of(2023, 1, 15), params.get(0).getDispatchDate());
        assertEquals("사고부상", params.get(0).getAccidentType());
        assertEquals("부암동", params.get(0).getLocationName());
        assertEquals(126.96, params.get(0).getLng());
        assertEquals(37.58, params.get(0).getLat());
        assertTrue(params.get(0).getRawPropertiesJson().contains("extra_field"),
                "raw_properties must preserve the FULL original properties object, not just the projected columns");

        assertEquals(1, params.get(1).getSourceFeatureIndex());
        assertEquals("REPORT-B", params.get(1).getReportNo());
    }

    @Test
    void deletesBeforeInsertingForSourceFileReplaceStrategy() throws Exception {
        AccidentImportDAO dao = mock(AccidentImportDAO.class);
        AccidentImportService service = new AccidentImportService(dao);

        JsonNode root = parse("""
            {"type":"FeatureCollection","features":[
               {"type":"Feature",
                "properties":{"msfrtn_resc_reprt_no":"REPORT-A","dsp_ymd":20230115,
                               "acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동"},
                "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
             ]}
            """);

        service.importAccidents("test.geojson", root);

        verify(dao, times(1)).deleteAccidentPointsBySourceFile("test.geojson");
    }

    @Test
    void abortsBeforeAnyWriteWhenAFeatureFailsValidation() throws Exception {
        AccidentImportDAO dao = mock(AccidentImportDAO.class);
        AccidentImportService service = new AccidentImportService(dao);

        JsonNode root = parse("""
            {"type":"FeatureCollection","features":[
               {"type":"Feature",
                "properties":{"msfrtn_resc_reprt_no":"REPORT-A","dsp_ymd":20230115,
                               "acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동"},
                "geometry":{"type":"Point","coordinates":[126.96,37.58]}},
               {"type":"Feature",
                "properties":{"msfrtn_resc_reprt_no":"REPORT-B"},
                "geometry":{"type":"Point","coordinates":[126.97,37.59]}}
             ]}
            """);

        assertThrows(AccidentImportException.class, () -> service.importAccidents("test.geojson", root));

        verify(dao, never()).deleteAccidentPointsBySourceFile(any());
        verify(dao, never()).insertAccidentPoint(any());
    }

    @Test
    void rejectsWrongRootType() throws Exception {
        AccidentImportDAO dao = mock(AccidentImportDAO.class);
        AccidentImportService service = new AccidentImportService(dao);

        JsonNode root = parse("""
            {"type":"Feature","features":[]}
            """);

        assertThrows(AccidentImportException.class, () -> service.importAccidents("test.geojson", root));
        verifyNoInteractions(dao);
    }

    @Test
    void rejectsUnexpectedCrs() throws Exception {
        AccidentImportDAO dao = mock(AccidentImportDAO.class);
        AccidentImportService service = new AccidentImportService(dao);

        JsonNode root = parse("""
            {"type":"FeatureCollection",
             "crs":{"type":"name","properties":{"name":"urn:ogc:def:crs:EPSG::5179"}},
             "features":[]}
            """);

        assertThrows(AccidentImportException.class, () -> service.importAccidents("test.geojson", root));
        verifyNoInteractions(dao);
    }

    @Test
    void missingCrsIsAcceptedAndAssumedCrs84() throws Exception {
        AccidentImportDAO dao = mock(AccidentImportDAO.class);
        AccidentImportService service = new AccidentImportService(dao);

        JsonNode root = parse("""
            {"type":"FeatureCollection","features":[]}
            """);

        AccidentImportResult result = service.importAccidents("test.geojson", root);
        assertEquals(0, result.importedCount);
    }

    @Test
    void allowsTwoFeaturesWithIdenticalCoordinatesButDifferentReportNo() throws Exception {
        AccidentImportDAO dao = mock(AccidentImportDAO.class);
        AccidentImportService service = new AccidentImportService(dao);

        JsonNode root = parse("""
            {"type":"FeatureCollection","features":[
               {"type":"Feature",
                "properties":{"msfrtn_resc_reprt_no":"REPORT-A","dsp_ymd":20230115,
                               "acdnt_cause_asort_nm":"사고부상","emd_nm":"부암동"},
                "geometry":{"type":"Point","coordinates":[126.96,37.58]}},
               {"type":"Feature",
                "properties":{"msfrtn_resc_reprt_no":"REPORT-B","dsp_ymd":20230116,
                               "acdnt_cause_asort_nm":"질환","emd_nm":"홍제동"},
                "geometry":{"type":"Point","coordinates":[126.96,37.58]}}
             ]}
            """);

        AccidentImportResult result = service.importAccidents("test.geojson", root);

        assertEquals(2, result.importedCount);
        verify(dao, times(2)).insertAccidentPoint(any());
    }
}
