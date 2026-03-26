package backend.SecureAuthAPI.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.github.benmanes.caffeine.cache.Cache;
import com.jayway.jsonpath.JsonPath;
import backend.secureauthapi.dto.request.LoginRequest;
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;
import backend.secureauthapi.security.filter.RateLimitFilter;
import io.github.bucket4j.Bucket;

class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Cache<String, Bucket> rateLimitCache;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private WebApplicationContext context;

    private static final String VALID_NAME = "John Doe";
    private static final String VALID_EMAIL = "john.doe@test.com";
    private static final String VALID_PASSWORD = "SecurePass123!";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(rateLimitFilter)
                .build();

        // Clear rate limit cache before each test
        rateLimitCache.invalidateAll();
    }

    @Nested
    @DisplayName("Auth Endpoint Rate Limiting (/api/auth/login)")
    class AuthEndpointRateLimitTests {

        @Test
        @Transactional
        @DisplayName("Should allow requests under limit (10/min)")
        void authLimit_underLimit_allowsRequests() throws Exception {

            // Arrange
            saveUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, Role.USER);
            LoginRequest request = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);
            String testIp = "192.168.1.100";

            // Act & Assert
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(header().exists("RateLimit-Remaining"));
            }
        }

        @Test
        @Transactional
        @DisplayName("Should return 429 when auth limit exceeded")
        void authLimit_exceededLimit_returns429WithErrorResponse() throws Exception {

            // Arrange
            saveUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, Role.USER);
            LoginRequest request = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);
            String testIp = "192.168.1.100";

            // Act
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(header().exists("RateLimit-Remaining"));
            }

            // Assert
            mockMvc.perform(post("/api/auth/login")
                    .header("X-Forwarded-For", testIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                    .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));
        }

        @Test
        @Transactional
        @DisplayName("Should include RateLimit headers in 429 response")
        void authLimit_exceededLimit_includesRateLimitHeaders() throws Exception {

            // Arrange
            saveUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, Role.USER);
            LoginRequest request = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);
            String testIp = "192.168.1.100";

            // Act
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(header().exists("RateLimit-Remaining"));
            }

            // Assert
            mockMvc.perform(post("/api/auth/login")
                    .header("X-Forwarded-For", testIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                    .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"))
                    .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."))
                    .andExpect(header().string("RateLimit-Remaining", "0"))
                    .andExpect(header().string("RateLimit-Limit", "10"))
                    .andExpect(header().string("RateLimit-Reset", greaterThan("0")))
                    .andExpect(header().string("RateLimit-Reset", lessThanOrEqualTo("60")))
                    .andExpect(header().string("Retry-After", greaterThan("0")))
                    .andExpect(header().string("Retry-After", lessThanOrEqualTo("60")));
        }
    }

    @Nested
    @DisplayName("General API Endpoint Rate Limiting (/api/users/me)")
    class GeneralEndpointRateLimitTests {

        @Test
        @Transactional
        @DisplayName("Should allow requests under limit (60/min)")
        void generalLimit_underLimit_allowsRequests() throws Exception {

            // Arrange
            saveUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, Role.USER);
            String accessToken = loginAndGetAccessToken(VALID_EMAIL, VALID_PASSWORD);
            String testIp = "192.168.1.200";

            // Act & Assert
            for (int i = 0; i < 60; i++) {
                mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(header().exists("RateLimit-Remaining"));
            }
        }

        @Test
        @Transactional
        @DisplayName("Should return 429 when general limit exceeded")
        void generalLimit_exceededLimit_returns429() throws Exception {

            // Arrange
            saveUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, Role.USER);
            String accessToken = loginAndGetAccessToken(VALID_EMAIL, VALID_PASSWORD);
            String testIp = "192.168.1.200";

            // Act
            for (int i = 0; i < 60; i++) {
                mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(header().exists("RateLimit-Remaining"));
            }

            // Assert
            mockMvc.perform(get("/api/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Forwarded-For", testIp)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                    .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."))
                    .andExpect(jsonPath("$.path").value("/api/users/me"))
                    .andExpect(header().string("RateLimit-Remaining", "0"))
                    .andExpect(header().string("RateLimit-Limit", "60"));
        }
    }

    @Nested
    @DisplayName("Rate Limit Reset Behavior")
    class RateLimitResetTests {

        @Test
        @Transactional
        @DisplayName("Should reset limit after time window expires")
        void limitReset_afterTimeWindow_allowsNewRequests() throws Exception {

            // Arrange
            saveUser(VALID_NAME, VALID_EMAIL, VALID_PASSWORD, Role.USER);
            LoginRequest request = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);
            String testIp = "192.168.1.250";

            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());
            }

            // Act
            MvcResult blockedResult = mockMvc.perform(post("/api/auth/login")
                    .header("X-Forwarded-For", testIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andReturn();

            String retryAfterHeader = blockedResult.getResponse().getHeader("Retry-After");
            long retryAfterSeconds = Long.parseLong(retryAfterHeader);

            // Wait slightly longer than Retry-After to avoid timing flakiness.
            Thread.sleep((retryAfterSeconds + 1L) * 1000L);

            // Assert
            mockMvc.perform(post("/api/auth/login")
                    .header("X-Forwarded-For", testIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("RateLimit-Remaining"));
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
}
