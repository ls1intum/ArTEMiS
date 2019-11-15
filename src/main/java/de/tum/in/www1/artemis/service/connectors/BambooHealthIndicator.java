package de.tum.in.www1.artemis.service.connectors;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("bamboo")
@Component
public class BambooHealthIndicator implements HealthIndicator {

    private final ContinuousIntegrationService continuousIntegrationService;

    public BambooHealthIndicator(ContinuousIntegrationService continuousIntegrationService) {
        this.continuousIntegrationService = continuousIntegrationService;
    }

    @Override
    public Health health() {
        return continuousIntegrationService.health().asActuatorHealth();
    }
}
