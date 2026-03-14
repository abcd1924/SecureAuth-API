package backend.SecureAuthAPI.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;

@DisplayName("AdminController Integration Tests")
class AdminControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String VALID_PASSWORD = "SecurePass123!";

    @Nested
    @DisplayName("GET /api/admin/users")
    class GetAllUsersTests {

        @Test
        @Transactional
        @DisplayName("Should return 200 with paginated users when requester is admin")
        void getAllUsers_adminToken_returns200WithPaginatedResponse() throws Exception {

            // Arrange
            String adminEmail = uniqueEmail("admin-list");
            String userEmail = uniqueEmail("user-list");

            saveUser("Admin User", adminEmail, VALID_PASSWORD, Role.ADMIN);
            saveUser("Regular User", userEmail, VALID_PASSWORD, Role.USER);

            String adminAccessToken = loginAndGetAccessToken(adminEmail, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(get("/api/admin/users")
                    .header("Authorization", "Bearer " + adminAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)))
                    .andExpect(jsonPath("$.size").isNumber())
                    .andExpect(jsonPath("$.number").isNumber());
        }

        @Test
        @Transactional
        @DisplayName("Should return 200 and respect pagination parameters")
        void getAllUsers_adminToken_respectsPaginationParams() throws Exception {

            // Arrange
            String adminEmail = uniqueEmail("admin-page");
            saveUser("Admin User", adminEmail, VALID_PASSWORD, Role.ADMIN);
            saveUser("User 1", uniqueEmail("user1"), VALID_PASSWORD, Role.USER);
            saveUser("User 2", uniqueEmail("user2"), VALID_PASSWORD, Role.USER);

            String adminAccessToken = loginAndGetAccessToken(adminEmail, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(get("/api/admin/users")
                    .param("page", "0")
                    .param("size", "2")
                    .header("Authorization", "Bearer " + adminAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        @DisplayName("Should return 401 when token is missing")
        void getAllUsers_missingToken_returns401() throws Exception {

            // Act & Assert
            mockMvc.perform(get("/api/admin/users")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Authentication required"))
                    .andExpect(jsonPath("$.path").value("/api/admin/users"));
        }

        @Test
        @Transactional
        @DisplayName("Should return 403 when requester is not admin")
        void getAllUsers_userToken_returns403() throws Exception {

            // Arrange
            String userEmail = uniqueEmail("non-admin-list");
            saveUser("Regular User", userEmail, VALID_PASSWORD, Role.USER);

            String userAccessToken = loginAndGetAccessToken(userEmail, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(get("/api/admin/users")
                    .header("Authorization", "Bearer " + userAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/users/{id}")
    class GetUserByIdTests {

        @Test
        @Transactional
        @DisplayName("Should return 200 with user data when requester is admin and user exists")
        void getUserById_existingUserAndAdminToken_returns200WithUserData() throws Exception {

            // Arrange
            String adminEmail = uniqueEmail("admin-id");
            String targetUserEmail = uniqueEmail("target-id");

            saveUser("Admin User", adminEmail, VALID_PASSWORD, Role.ADMIN);
            User targetUser = saveUser("Target User", targetUserEmail, VALID_PASSWORD, Role.USER);

            String adminAccessToken = loginAndGetAccessToken(adminEmail, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(get("/api/admin/users/{id}", targetUser.getId())
                    .header("Authorization", "Bearer " + adminAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(targetUser.getId()))
                    .andExpect(jsonPath("$.name").value("Target User"))
                    .andExpect(jsonPath("$.email").value(targetUserEmail))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.isActive").value(true))
                    .andExpect(jsonPath("$.createdAt").isString())
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        @Transactional
        @DisplayName("Should return 404 when requester is admin and user does not exist")
        void getUserById_nonExistentUser_returns404() throws Exception {

            // Arrange
            String adminEmail = uniqueEmail("admin-missing");
            saveUser("Admin User", adminEmail, VALID_PASSWORD, Role.ADMIN);

            String adminAccessToken = loginAndGetAccessToken(adminEmail, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(get("/api/admin/users/{id}", 999999L)
                    .header("Authorization", "Bearer " + adminAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
        }

        @Test
        @Transactional
        @DisplayName("Should return 400 when id is not positive")
        void getUserById_nonPositiveId_returns400() throws Exception {

            // Arrange
            String adminEmail = uniqueEmail("admin-invalid-id");
            saveUser("Admin User", adminEmail, VALID_PASSWORD, Role.ADMIN);

            String adminAccessToken = loginAndGetAccessToken(adminEmail, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(get("/api/admin/users/{id}", 0)
                    .header("Authorization", "Bearer " + adminAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Id must be positive")));
        }

        @Test
        @DisplayName("Should return 401 when token is missing")
        void getUserById_missingToken_returns401() throws Exception {

            // Act & Assert
            mockMvc.perform(get("/api/admin/users/{id}", 1L)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Authentication required"))
                    .andExpect(jsonPath("$.path").value("/api/admin/users/1"));
        }

        @Test
        @Transactional
        @DisplayName("Should return 403 when requester is not admin")
        void getUserById_userToken_returns403() throws Exception {

            // Arrange
            String userEmail = uniqueEmail("non-admin-id");

            User targetUser = saveUser("Target User", uniqueEmail("target-non-admin"),
                    VALID_PASSWORD, Role.USER);

            saveUser("Regular User", userEmail, VALID_PASSWORD, Role.USER);

            String userAccessToken = loginAndGetAccessToken(userEmail, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(get("/api/admin/users/{id}", targetUser.getId())
                    .header("Authorization", "Bearer " + userAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
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

    private String uniqueEmail(String prefix) {
        return prefix + "." + System.nanoTime() + "@test.com";
    }
}
