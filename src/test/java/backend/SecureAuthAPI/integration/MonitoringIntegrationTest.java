package backend.SecureAuthAPI.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import backend.secureauthapi.dto.request.LoginRequest;
import backend.secureauthapi.dto.request.RefreshTokenRequest;
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;

@DisplayName("Monitoring Integration Tests")
class MonitoringIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String VALID_PASSWORD = "SecurePass123!";

    @Nested
    @DisplayName("Health Probe Endpoints")
    class ProbeEndpointTests {

        @Test
        @DisplayName("Should return 200 for /actuator/health without authentication")
        void actuatorHealth_withoutToken_returns200() throws Exception {

            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").isString());
        }

        @Test
        @DisplayName("Should return 200 for /actuator/health/liveness without authentication")
        void actuatorHealthLiveness_withoutToken_returns200() throws Exception {

            mockMvc.perform(get("/actuator/health/liveness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").isString());
        }

        @Test
        @DisplayName("Should return 200 for /actuator/health/readiness without authentication")
        void actuatorHealthReadiness_withoutToken_returns200() throws Exception {

            mockMvc.perform(get("/actuator/health/readiness"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").isString());
        }
    }

    @Nested
    @DisplayName("Actuator Security")
    class ActuatorSecurityTests {

        @Test
        @DisplayName("Should return 401 for /actuator/metrics without authentication")
        void actuatorMetrics_withoutToken_returns401() throws Exception {

            mockMvc.perform(get("/actuator/metrics"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 for /actuator/prometheus without authentication")
        void actuatorPrometheus_withoutToken_returns401() throws Exception {

            mockMvc.perform(get("/actuator/prometheus"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @Transactional
        @DisplayName("Should return 200 for /actuator/metrics with ADMIN token")
        void actuatorMetrics_withAdminToken_returns200() throws Exception {

            String adminEmail = uniqueEmail("admin-metrics");
            saveUser("Admin Metrics", adminEmail, VALID_PASSWORD, Role.ADMIN);
            String adminAccessToken = loginAndGetAccessToken(adminEmail, VALID_PASSWORD);

            mockMvc.perform(get("/actuator/metrics")
                    .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json")))
                    .andExpect(jsonPath("$.names").isArray())
                    .andExpect(jsonPath("$.names", hasItem("auth.login.attempts")));
        }

        @Test
        @Transactional
        @DisplayName("Should return 200 and valid exposition format for /actuator/prometheus with ADMIN token")
        void actuatorPrometheus_withAdminToken_returns200AndOpenMetricsText() throws Exception {

            String adminEmail = uniqueEmail("admin-prometheus");
            saveUser("Admin Prom", adminEmail, VALID_PASSWORD, Role.ADMIN);
            String adminAccessToken = loginAndGetAccessToken(adminEmail, VALID_PASSWORD);

            mockMvc.perform(get("/actuator/prometheus")
                    .header("Authorization", "Bearer " + adminAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string(containsString("# HELP")))
                    .andExpect(content().string(containsString("# TYPE")));
        }
    }

    @Nested
    @DisplayName("Custom Metric Presence")
    class CustomMetricPresenceTests {

        @Test
        @Transactional
        @DisplayName("Should expose login metrics after one failure and one success")
        void prometheus_afterLogin_containsAuthLoginMetrics() throws Exception {

            String userEmail = uniqueEmail("user-login-metrics");
            saveUser("User Login", userEmail, VALID_PASSWORD, Role.USER);

            LoginRequest failedLogin = new LoginRequest(userEmail, "WrongPassword999!");
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failedLogin)))
                    .andExpect(status().isUnauthorized());

            LoginRequest successfulLogin = new LoginRequest(userEmail, VALID_PASSWORD);
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(successfulLogin)))
                    .andExpect(status().isOk());

            String adminToken = createAdminAndGetToken("admin-check-login");

            mockMvc.perform(get("/actuator/prometheus")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("auth_login_attempts_total")))
                    .andExpect(content().string(containsString("outcome=\"success\"")))
                    .andExpect(content().string(containsString("outcome=\"failure\"")))
                    .andExpect(content().string(containsString("auth_login_duration_seconds")));
        }

        @Test
        @Transactional
        @DisplayName("Should expose refresh metrics after token refresh flow")
        void prometheus_afterRefresh_containsAuthRefreshMetrics() throws Exception {

            String userEmail = uniqueEmail("user-refresh-metrics");
            saveUser("User Refresh", userEmail, VALID_PASSWORD, Role.USER);

            String refreshToken = loginAndGetRefreshToken(userEmail, VALID_PASSWORD);
            RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

            mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk());

            String adminToken = createAdminAndGetToken("admin-check-refresh");

            mockMvc.perform(get("/actuator/prometheus")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("auth_refresh_attempts_total")))
                    .andExpect(content().string(containsString("auth_refresh_duration_seconds")));
        }

        @Test
        @Transactional
        @DisplayName("Should expose account action metrics after account deactivation")
        void prometheus_afterDeactivate_containsAccountActionMetric() throws Exception {

            String userEmail = uniqueEmail("user-deactivate-metrics");
            saveUser("User Deactivate", userEmail, VALID_PASSWORD, Role.USER);
            String userAccessToken = loginAndGetAccessToken(userEmail, VALID_PASSWORD);

            mockMvc.perform(delete("/api/users/me")
                    .header("Authorization", "Bearer " + userAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            String adminToken = createAdminAndGetToken("admin-check-deactivate");

            mockMvc.perform(get("/actuator/prometheus")
                    .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("user_account_deactivate_attempts_total")))
                    .andExpect(content().string(containsString("outcome=\"success\"")));
        }
    }

    // Helpers
    private User saveUser(String name, String email, String rawPassword, Role role) {
        User user = new User(name, email, passwordEncoder.encode(rawPassword), role);
        return userRepository.save(user);
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(loginResponse, "$.accessToken");
    }

    private String loginAndGetRefreshToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(loginResponse, "$.refreshToken");
    }

    private String createAdminAndGetToken(String emailPrefix) throws Exception {
        String adminEmail = uniqueEmail(emailPrefix);
        saveUser("Monitoring Admin", adminEmail, VALID_PASSWORD, Role.ADMIN);
        return loginAndGetAccessToken(adminEmail, VALID_PASSWORD);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + System.nanoTime() + "@test.com";
    }
}
