package com.season.semiproject.spatial.accident;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * One-shot Accident Raw Spatial Layer import job. Only runs under the
 * `spatial-accident-import` Spring profile (see application-spatial-accident-import.properties,
 * which also sets spring.main.web-application-type=none), mirroring SpatialImportRunner /
 * NetworkBuildRunner's execution style. A normal `./mvnw spring-boot:run` never touches this
 * class. Run explicitly with:
 *   SPRING_PROFILES_ACTIVE=spatial-accident-import ./mvnw spring-boot:run
 */
@Component
@Profile("spatial-accident-import")
public class AccidentImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AccidentImportRunner.class);

    private final AccidentImportService importService;
    private final ConfigurableApplicationContext context;

    @Value("${spatial.accident-import.geojson-file:../frontend/public/data/2023산악사고_인왕산.geojson}")
    private String geoJsonFilePath;

    // Stored in accident_point.source_file -- deliberately the plain filename (matching the
    // Trail importer's convention), not the full path, since this is a stable identity key for
    // the source_file replace strategy regardless of where the file happens to be read from.
    @Value("${spatial.accident-import.source-file:2023산악사고_인왕산.geojson}")
    private String sourceFileName;

    @Autowired
    public AccidentImportRunner(AccidentImportService importService, ConfigurableApplicationContext context) {
        this.importService = importService;
        this.context = context;
    }

    @Override
    public void run(String... args) {
        final int exitCode = doImport();
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    private int doImport() {
        try {
            log.info("Accident import starting. geoJsonFile={}, sourceFile={}", geoJsonFilePath, sourceFileName);

            JsonNode geoJsonRoot;
            try (InputStream in = new FileSystemResource(geoJsonFilePath).getInputStream()) {
                geoJsonRoot = importService.loadGeoJson(in);
            }

            AccidentImportResult result = importService.importAccidents(sourceFileName, geoJsonRoot);
            log.info("Accident import finished successfully: {}", result);
            return 0;
        } catch (AccidentImportException e) {
            log.error("Accident import FAILED (aborted, no partial data committed): {}", e.getMessage());
            return 1;
        } catch (IOException e) {
            log.error("Accident import FAILED to read input file: {}", e.getMessage(), e);
            return 1;
        } catch (RuntimeException e) {
            // Covers DB-level failures (e.g. a constraint violation) that Spring's @Transactional
            // rolls back automatically -- see docs/02-migration-and-data-quality.md.
            log.error("Accident import FAILED with an unexpected error; the transaction was rolled back "
                    + "so no partial data was committed: {}", e.getMessage(), e);
            return 1;
        }
    }
}
