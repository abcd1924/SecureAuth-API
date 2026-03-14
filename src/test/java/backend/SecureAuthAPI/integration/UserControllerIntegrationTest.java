package backend.SecureAuthAPI.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import backend.secureauthapi.dto.request.UpdateProfileRequest;
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;

@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String VALID_NAME = "John Doe";
    private static final String VALID_EMAIL = "john.doe@test.com";
    private static final String VALID_PASSWORD = "SecurePass123!";

    @Nested
    @DisplayName("GET /api/users/me")
    class GetCurrentUserTests {

        @Test
        @Transactional
        @DisplayName("Should return 200 with current user when token is valid")
        void getCurrentUser_validToken_returns200WithUserData() throws Exception {

            // Arrange
            User savedUser = saveUser(VALID_EMAIL, VALID_PASSWORD);
            String accessToken = loginAndGetAccessToken(VALID_EMAIL, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(get("/api/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedUser.getId()))
                    .andExpect(jsonPath("$.name").value(VALID_NAME))
                    .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.isActive").value(true))
                    .andExpect(jsonPath("$.createdAt").isString())
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("Should return 401 when token is missing")
        void getCurrentUser_missingToken_returns401() throws Exception {

            // Act & Assert
            mockMvc.perform(get("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Authentication required"))
                    .andExpect(jsonPath("$.path").value("/api/users/me"));
        }

        @Test
        @DisplayName("Should return 401 when token is invalid")
        void getCurrentUser_invalidToken_returns401() throws Exception {

            // Act & Assert
            mockMvc.perform(get("/api/users/me")
                    .header("Authorization", "Bearer invalid.jwt.token")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Authentication required"))
                    .andExpect(jsonPath("$.path").value("/api/users/me"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me")
    class UpdateProfileTests {

        @Test
        @Transactional
        @DisplayName("Should return 200 and persist updated name when request is valid")
        void updateProfile_validRequest_returns200AndPersistsName() throws Exception {

            // Arrange
            saveUser(VALID_EMAIL, VALID_PASSWORD);
            String accessToken = loginAndGetAccessToken(VALID_EMAIL, VALID_PASSWORD);
            UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe");

            // Act & Assert
            mockMvc.perform(patch("/api/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Jane Doe"))
                    .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.isActive").value(true));

            User updatedUser = userRepository.findByEmail(VALID_EMAIL).orElse(null);
            assertThat(updatedUser).isNotNull();
            assertThat(updatedUser.getName()).isEqualTo("Jane Doe");
        }

        @Test
        @Transactional
        @DisplayName("Should return 400 when name is blank")
        void updateProfile_blankName_returns400() throws Exception {

            // Arrange
            saveUser(VALID_EMAIL, VALID_PASSWORD);
            String accessToken = loginAndGetAccessToken(VALID_EMAIL, VALID_PASSWORD);

            String blankBodyJson = """
                    {
                            "name": ""
                    }
                    """;

            // Act & Assert
            mockMvc.perform(patch("/api/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(blankBodyJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("name")))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @Transactional
        @DisplayName("Should return 400 when name contains invalid characters")
        void updateProfile_invalidNamePattern_returns400() throws Exception {

            // Arrange
            saveUser(VALID_EMAIL, VALID_PASSWORD);
            String accessToken = loginAndGetAccessToken(VALID_EMAIL, VALID_PASSWORD);
            UpdateProfileRequest request = new UpdateProfileRequest("John123");

            // Act & Assert
            mockMvc.perform(patch("/api/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Name contains invalid characters")))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 401 when token is missing")
        void updateProfile_missingToken_returns401() throws Exception {

            // Arrange
            UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe");

            // Act & Assert
            mockMvc.perform(patch("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"))
                    .andExpect(jsonPath("$.message").value("Authentication required"))
                    .andExpect(jsonPath("$.path").value("/api/users/me"));
        }
    }

    // Helpers
    private User saveUser(String email, String rawPassword) {
        User user = new User(VALID_NAME, email, passwordEncoder.encode(rawPassword), Role.USER);
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
