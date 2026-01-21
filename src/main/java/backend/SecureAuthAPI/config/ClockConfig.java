package backend.secureauthapi.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for time-related beans.
 * Provides a Clock instance for consistent time handling across the
 * application.
 * Using dependency injection for Clock enables easier testing with fixed or
 * mocked time.
 */
@Configuration
public class ClockConfig {

    /**
     * Provides a system UTC clock for the application.
     * Using UTC ensures consistency across different server timezones and avoids
     * daylight saving time issues.
     *
     * @return a Clock instance set to UTC timezone
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}