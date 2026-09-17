package com.season.semiproject.spatial;

import com.fasterxml.jackson.databind.JsonNode;
import com.season.semiproject.spatial.manifest.TrailImportManifest;
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
 * One-shot Raw Spatial Layer import job. Only runs under the `spatial-import` Spring profile
 * (see application-spatial-import.properties, which also sets
 * spring.main.web-application-type=none so this never binds the HTTP port or starts Tomcat).
 *
 * A normal `./mvnw spring-boot:run` (no profile, or any other profile) never touches this class
 * at runtime -- Spring only instantiates @Profile("spatial-import") beans when that profile is
 * active. Run explicitly with:
 *   SPRING_PROFILES_ACTIVE=spatial-import ./mvnw spring-boot:run
 *
 * Exits the JVM when done (success or failure) since this is a batch job, not a long-running
 * server -- see docs/02-migration-and-data-quality.md for why this execution style was chosen.
 */
@Component
@Profile("spatial-import")
public class SpatialImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpatialImportRunner.class);

    private final TrailImportService importService;
    private final ConfigurableApplicationContext context;

    @Value("${spatial.import.manifest-path:classpath:spatial-import/trail-import-manifest.json}")
    private String manifestPath;

    @Value("${spatial.import.geojson-file:../frontend/public/data/인왕산ele copy.geojson}")
    private String geoJsonFilePath;

    @Autowired
    public SpatialImportRunner(TrailImportService importService, ConfigurableApplicationContext context) {
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
            log.info("Spatial import starting. manifest={}, geoJsonFile={}", manifestPath, geoJsonFilePath);

            TrailImportManifest manifest;
            try (InputStream manifestStream = openManifest()) {
                manifest = importService.loadManifest(manifestStream);
            }

            JsonNode geoJsonRoot;
            try (InputStream geoJsonStream = new FileSystemResource(geoJsonFilePath).getInputStream()) {
                geoJsonRoot = importService.loadGeoJson(geoJsonStream);
            }

            TrailImportResult result = importService.importTrails(manifest, geoJsonRoot);
            log.info("Spatial import finished successfully:\n{}", result);
            return 0;
        } catch (TrailImportException e) {
            log.error("Spatial import FAILED (aborted, no partial data committed): {}", e.getMessage());
            return 1;
        } catch (IOException e) {
            log.error("Spatial import FAILED to read input files: {}", e.getMessage(), e);
            return 1;
        } catch (RuntimeException e) {
            // Covers DB-level failures (e.g. a FK/constraint violation during insert) that
            // Spring's @Transactional rolls back automatically -- see
            // docs/02-migration-and-data-quality.md for the fixture-based rollback test that
            // exercises exactly this path. Logged distinctly from TrailImportException because
            // it means a row that passed Java-level validation was still rejected by the DB.
            log.error("Spatial import FAILED with an unexpected error; the transaction was rolled back "
                    + "so no partial data was committed: {}", e.getMessage(), e);
            return 1;
        }
    }

    private InputStream openManifest() throws IOException {
        if (manifestPath.startsWith("classpath:")) {
            String resourcePath = manifestPath.substring("classpath:".length());
            InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (in == null) {
                throw new IOException("Manifest not found on classpath: " + resourcePath);
            }
            return in;
        }
        return new FileSystemResource(manifestPath).getInputStream();
    }
}
