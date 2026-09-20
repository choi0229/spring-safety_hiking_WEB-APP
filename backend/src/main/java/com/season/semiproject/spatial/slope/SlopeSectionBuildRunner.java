package com.season.semiproject.spatial.slope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * One-shot Derived Analysis Layer (SlopeSection) build job. Only runs under the
 * `spatial-slope-build` Spring profile (see application-spatial-slope-build.properties, which
 * also sets spring.main.web-application-type=none so this never binds the HTTP port).
 *
 * A normal `./mvnw spring-boot:run` never touches this class -- Spring only instantiates
 * @Profile("spatial-slope-build") beans when that profile is active. Run explicitly with:
 *   SPRING_PROFILES_ACTIVE=spatial-slope-build ./mvnw spring-boot:run
 *
 * MUST be run after the Network build (`spatial-network-build` profile) -- SlopeSection is
 * derived from trail_segment/trail_node, so a stale or missing Network produces a stale or
 * missing Analysis Layer (see docs/09-slope-section-analysis.md, pipeline order).
 *
 * Exits the JVM when done (success or failure), same pattern as NetworkBuildRunner (Phase 6).
 */
@Component
@Profile("spatial-slope-build")
public class SlopeSectionBuildRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SlopeSectionBuildRunner.class);

    private final SlopeSectionBuildService buildService;
    private final ConfigurableApplicationContext context;

    @Autowired
    public SlopeSectionBuildRunner(SlopeSectionBuildService buildService, ConfigurableApplicationContext context) {
        this.buildService = buildService;
        this.context = context;
    }

    @Override
    public void run(String... args) {
        final int exitCode = doBuild();
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }

    private int doBuild() {
        try {
            log.info("Slope section build starting.");
            SlopeSectionBuildResult result = buildService.buildAll();
            log.info("Slope section build finished successfully: {}", result);
            return 0;
        } catch (SlopeSectionBuildException e) {
            log.error("Slope section build FAILED (aborted, transaction rolled back): {}", e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            log.error("Slope section build FAILED with an unexpected error; the transaction was "
                    + "rolled back so the previous slope_section content (if any) is intact: {}", e.getMessage(), e);
            return 1;
        }
    }
}
