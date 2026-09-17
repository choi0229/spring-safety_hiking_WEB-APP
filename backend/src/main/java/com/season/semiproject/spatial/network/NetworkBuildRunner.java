package com.season.semiproject.spatial.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * One-shot Derived Network Layer build job. Only runs under the `spatial-network-build` Spring
 * profile (see application-spatial-network-build.properties, which also sets
 * spring.main.web-application-type=none so this never binds the HTTP port).
 *
 * A normal `./mvnw spring-boot:run` never touches this class -- Spring only instantiates
 * @Profile("spatial-network-build") beans when that profile is active. Run explicitly with:
 *   SPRING_PROFILES_ACTIVE=spatial-network-build ./mvnw spring-boot:run
 *
 * Exits the JVM when done (success or failure), same pattern as SpatialImportRunner (Phase 5).
 */
@Component
@Profile("spatial-network-build")
public class NetworkBuildRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NetworkBuildRunner.class);

    private final NetworkBuildService buildService;
    private final ConfigurableApplicationContext context;

    @Autowired
    public NetworkBuildRunner(NetworkBuildService buildService, ConfigurableApplicationContext context) {
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
            log.info("Network build starting.");
            NetworkBuildResult result = buildService.buildNetwork();
            log.info("Network build finished successfully: {}", result);
            return 0;
        } catch (NetworkBuildException e) {
            log.error("Network build FAILED (aborted, transaction rolled back): {}", e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            log.error("Network build FAILED with an unexpected error; the transaction was rolled "
                    + "back so the previous network (if any) is intact: {}", e.getMessage(), e);
            return 1;
        }
    }
}
