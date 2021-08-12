package ca.bc.gov.educ.api.dataconversion.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class FlywayMigrationStrategyImpl implements FlywayMigrationStrategy {

    private static Logger logger = LoggerFactory.getLogger(FlywayMigrationStrategyImpl.class);

    @Override
    public void migrate(Flyway flyway) {
        if (!flyway.validateWithResult().validationSuccessful) {
            flyway.repair();
        }

        logger.info("\n");
        logger.info("************FLYWAY-MIGRATE**********");
        flyway.migrate();
        logger.info("************FLYWAY-MIGRATE-END**********\n\n");
    }
}
