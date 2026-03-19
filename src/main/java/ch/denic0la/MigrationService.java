package ch.denic0la;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MigrationService {
    private static final Logger log = LoggerFactory.getLogger(MigrationService.class);
    @Inject
    Flyway flyway;

    public void checkMigration() {
        log.info(flyway.info().current().getVersion().toString());
    }
}
