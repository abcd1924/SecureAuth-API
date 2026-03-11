package backend.SecureAuthAPI.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import backend.secureauthapi.dto.request.LoginRequest;
import backend.secureauthapi.dto.request.RegisterRequest;
import backend.secureauthapi.model.Role;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;

@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private UserRepository userRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    private static final String VALID_NAME = "John Doe";
    private static final String VALID_EMAIL = "john.doe@test.com";
    private static final String VALID_PASSWORD = "SecurePass123!";

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @Transactional
        @DisplayName("Should return 201 and persist user when request is valid")
        void register_validRequest_returns201AndPersistUser() throws Exception {

            // Arrange
            RegisterRequest request = new RegisterRequest(VALID_NAME, VALID_EMAIL,
                    VALID_PASSWORD);

            // Act
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    // Assert
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value(VALID_NAME))
                    .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.isActive").value(true))
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());

            User savedUser = userRepository.findByEmail(VALID_EMAIL).orElse(null);
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getName()).isEqualTo(VALID_NAME);
            assertThat(savedUser.getRole()).isEqualTo(Role.USER);
            assertThat(savedUser.isActive()).isTrue();
            assertThat(savedUser.getPasswordHash()).isNotEqualTo(VALID_PASSWORD);
            assertThat(passwordEncoder.matches(VALID_PASSWORD,
                    savedUser.getPasswordHash()))
                            .isTrue();
        }

        @Test
        @DisplayName("Should return 409 when email already exists")
        void register_duplicateEmail_returns409() throws Exception {

            // Arrange
            User existingUser = new User(VALID_NAME, VALID_EMAIL,
                    passwordEncoder.encode(VALID_PASSWORD), Role.USER);

            userRepository.save(existingUser);

            RegisterRequest duplicateRequest =
                    new RegisterRequest("Jane Doe", VALID_EMAIL,
                            VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode")
                            .value("USER_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void register_invalidEmailFormat_returns400() throws Exception {

            // Arrange
            RegisterRequest request = new RegisterRequest(VALID_NAME, "notanemail",
                    VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("email")))
                    .andExpect(jsonPath("$.errorCode")
                            .value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 when password does not meet security requirements")
        void register_weakPassword_returns400() throws Exception {

            // Arrange
            RegisterRequest request = new RegisterRequest(VALID_NAME, VALID_EMAIL,
                    "weakPassword");

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message",
                            containsString("password")))
                    .andExpect(jsonPath("$.errorCode")
                            .value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 when required fields are blank")
        void register_blankFields_returns400() throws Exception {

            // Arrange
            String blankBodyJson = """
                    {
                            "name": "",
                            "email": "",
                            "password": ""
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(blankBodyJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode")
                            .value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @Transactional
        @DisplayName("Should return 200 with tokens when credentials are valid")
        void login_validCredentials_return200WithTokens() throws Exception {

            // Arrange
            User user = new User(VALID_NAME, VALID_EMAIL,
                    passwordEncoder.encode(VALID_PASSWORD),
                    Role.USER);

            userRepository.save(user);

            LoginRequest request = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isString())
                    .andExpect(jsonPath("$.refreshToken").isString())
                    .andExpect(jsonPath("$.tokenType").value("BEARER"))
                    .andExpect(jsonPath("$.expiresAt").isString())
                    .andExpect(jsonPath("$.user.email").value(VALID_EMAIL))
                    .andExpect(jsonPath("$.user.role").value("USER"))
                    .andExpect(jsonPath("$.user.password").doesNotExist())
                    .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        }

        @Test
        @Transactional
        @DisplayName("Should return 401 when password is incorrect")
        void login_wrongPassword_returns401() throws Exception {

            // Arrange
            User user = new User(VALID_NAME, VALID_EMAIL, passwordEncoder.encode(VALID_PASSWORD),
                    Role.USER);

            userRepository.save(user);

            LoginRequest request = new LoginRequest(VALID_EMAIL, "WrongPassword999!");

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Should return 401 when email does not exist")
        void login_nonExistentEmail_returns401() throws Exception {

            // Arrange
            LoginRequest request = new LoginRequest("ghost@test.com", VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        }

        @Test
        @Transactional
        @DisplayName("Should return 401 when account is disabled")
        void login_disabledAccount_returns401() throws Exception {

            // Arrange
            User disabledUser = new User(VALID_NAME, VALID_EMAIL,
                    passwordEncoder.encode(VALID_PASSWORD), Role.USER);

            disabledUser.setActive(false);

            userRepository.save(disabledUser);

            LoginRequest request = new LoginRequest(VALID_EMAIL, VALID_PASSWORD);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("USER_INACTIVE"));
        }
    }
}
