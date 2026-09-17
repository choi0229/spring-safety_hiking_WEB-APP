package com.season.semiproject.spatial.accident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.season.semiproject.spatial.FeatureValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Accident Raw Spatial Layer importer: GeoJSON Feature -> AccidentPoint, 1:1, no filtering, no
 * relation to TrailSegment computed or stored here (that is a query-time concern, see
 * com.season.semiproject.spatial.query). See docs/02-migration-and-data-quality.md ("Accident
 * Raw Data Import") and docs/05-accident-spatial-query.md for the design this implements.
 *
 * Only frontend/public/data/2023산악사고_인왕산.geojson (42 Features) is a valid input to this
 * service. The sibling file 2023산악사고_인왕산2.geojson ("Legacy UI Display Dataset") must never
 * be passed here -- see accident-schema.sql for why.
 *
 * Deliberately NOT wired into any Controller/API -- only AccidentImportRunner (active under the
 * `spatial-accident-import` profile) calls this.
 */
@Service
public class AccidentImportService {

    private static final Logger log = LoggerFactory.getLogger(AccidentImportService.class);
    // Same STRICT resolver/pattern as AccidentFeatureValidator (see its comment for why "uuuu"
    // not "yyyy") -- by the time this runs, validation already guarantees the date parses, so
    // this never throws in practice.
    private static final DateTimeFormatter YMD =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
    private static final String EXPECTED_CRS_NAME = "urn:ogc:def:crs:OGC:1.3:CRS84";

    private final AccidentImportDAO dao;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AccidentFeatureValidator validator = new AccidentFeatureValidator();

    @Autowired
    public AccidentImportService(AccidentImportDAO dao) {
        this.dao = dao;
    }

    public JsonNode loadGeoJson(InputStream in) throws IOException {
        return objectMapper.readTree(in);
    }

    /**
     * source_file replace strategy, same philosophy as TrailImportService: delete any previous
     * import for this source file, then re-insert everything in one transaction. Every Feature is
     * validated BEFORE any DB write; a single invalid Feature aborts the whole import with no
     * partial data committed, since the 42-Feature source file was already confirmed entirely
     * valid during Phase 8 analysis -- an unexpected failure here means the source changed and
     * must be investigated, not silently skipped.
     */
    @Transactional
    public AccidentImportResult importAccidents(String sourceFile, JsonNode geoJsonRoot) {
        JsonNode typeNode = geoJsonRoot.get("type");
        if (typeNode == null || !"FeatureCollection".equals(typeNode.asText())) {
            throw new AccidentImportException(
                    "root.type is not FeatureCollection (was: " + typeNode + ")");
        }

        JsonNode crsName = geoJsonRoot.path("crs").path("properties").path("name");
        if (crsName.isMissingNode()) {
            log.info("Accident GeoJSON has no declared CRS; assuming CRS84/EPSG:4326 per GeoJSON spec default");
        } else if (!EXPECTED_CRS_NAME.equals(crsName.asText())) {
            throw new AccidentImportException(
                    "Unexpected CRS (expected " + EXPECTED_CRS_NAME + ", was: " + crsName.asText()
                            + ") -- coordinates cannot be safely assumed to be EPSG:4326 lon/lat");
        } else {
            log.info("Accident GeoJSON CRS confirmed: {}", crsName.asText());
        }

        JsonNode features = geoJsonRoot.get("features");
        if (features == null || !features.isArray()) {
            throw new AccidentImportException("root.features is missing or not an array");
        }

        List<AccidentPointInsertParam> params = new ArrayList<>();
        for (int i = 0; i < features.size(); i++) {
            JsonNode feature = features.get(i);
            FeatureValidationResult validation = validator.validate(feature);
            if (!validation.isValid()) {
                throw new AccidentImportException(
                        "Import aborted before any DB write -- invalid Feature at index " + i
                                + " of " + sourceFile + ": " + validation.getReason());
            }

            JsonNode properties = feature.get("properties");
            JsonNode coordinates = feature.get("geometry").get("coordinates");

            AccidentPointInsertParam param = new AccidentPointInsertParam();
            param.setSourceFile(sourceFile);
            param.setSourceFeatureIndex(i);
            param.setReportNo(properties.get("msfrtn_resc_reprt_no").asText());
            param.setDispatchDate(LocalDate.parse(properties.get("dsp_ymd").asText(), YMD));
            param.setAccidentType(properties.get("acdnt_cause_asort_nm").asText());
            param.setLocationName(properties.get("emd_nm").asText());
            param.setLng(coordinates.get(0).asDouble());
            param.setLat(coordinates.get(1).asDouble());
            try {
                // The FULL original properties object is preserved here, not just the fields
                // projected into regular columns -- see accident-schema.sql for why.
                param.setRawPropertiesJson(objectMapper.writeValueAsString(properties));
            } catch (IOException e) {
                // Serializing a JsonNode we already parsed cannot fail in practice; wrapped only
                // to satisfy the checked-exception signature.
                throw new AccidentImportException(
                        "Failed to serialize properties for feature index " + i + ": " + e.getMessage());
            }
            params.add(param);
        }

        dao.deleteAccidentPointsBySourceFile(sourceFile);
        for (AccidentPointInsertParam param : params) {
            dao.insertAccidentPoint(param);
        }

        return new AccidentImportResult(sourceFile, features.size(), params.size());
    }
}
