package ch.denic0la;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;

@Readiness
@ApplicationScoped
public class ReadinessCheck implements HealthCheck {

    @Inject
    Flyway flyway;

    @Override
    public HealthCheckResponse call() {
        if (flyway.info().current() != null) {
            return HealthCheckResponse.up("database-migration");
        }
        return HealthCheckResponse.down("database-migration");
    }
}
