package backend.SecureAuthAPI.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DisplayName("Monitoring Degraded Health Integration Tests")
@Import(MonitoringDegradedHealthIntegrationTest.FailingDependencyConfig.class)
class MonitoringDegradedHealthIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should report DOWN and 503 when a dependency health indicator fails")
    void actuatorHealth_whenDependencyFails_reportsDown() throws Exception {

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @TestConfiguration
    static class FailingDependencyConfig {

        @Bean(name = "fakeDependency")
        HealthIndicator fakeDependencyHealthIndicator() {
            return () -> Health.down()
                    .withDetail("reason", "simulated dependency failure")
                    .build();
        }
    }
}
