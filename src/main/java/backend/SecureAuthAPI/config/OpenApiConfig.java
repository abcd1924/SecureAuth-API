package backend.secureauthapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Provides interactive API documentation at /swagger-ui.html
 * and OpenAPI specification at /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .externalDocs(externalDocumentation())
                // NOTE: Security requirement is NOT added globally here.
                // It's applied per-endpoint using @SecurityRequirement annotation
                // to avoid requiring authentication on public endpoints like /api/auth/login
                .components(
                        new Components()
                                .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
    }

    /**
     * API metadata information displayed in Swagger UI
     */
    private Info apiInfo() {
        return new Info()
                .title("SecureAuth-API")
                .description("""
                        Secure Authentication and Authorization REST API built with Spring Boot 3 and Java 21.

                        **Features:**
                        - JWT-based authentication with access and refresh tokens
                        - Role-based access control (RBAC) with multiple roles
                        - Secure password encryption using BCrypt
                        - Token refresh mechanism for extended sessions
                        - RESTful API design following industry best practices

                        **Available Roles:** USER, ADMIN, AUDITOR, SUPPORT

                        **How to use:**
                        1. Register a new user via POST /api/auth/register
                        2. Login to obtain JWT tokens via POST /api/auth/login
                        3. Click the 'Authorize' button and enter your access token
                        4. Test protected endpoints
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Valentino Castro")
                        .email("valentinocastro3@gmail.com")
                        .url("https://github.com/abcd1924"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * JWT Bearer authentication scheme configuration
     */
    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Enter the JWT access token obtained from the login endpoint.

                        **Format:** Just paste the token value (without 'Bearer ' prefix)

                        **Example workflow:**
                        1. Call POST /api/auth/login with valid credentials
                        2. Copy the 'accessToken' from the response
                        3. Click 'Authorize' button above
                        4. Paste the token and click 'Authorize'
                        """);
    }

    /**
     * Server configurations for different environments
     */
    private List<Server> serverList() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Local Development Server")
        // Here you can add more servers for different environments like production
        // new Server()
        // .url("https://api.secureauth.example.com")
        // .description("Production Server")
        );
    }

    /**
     * External documentation link
     */
    private ExternalDocumentation externalDocumentation() {
        return new ExternalDocumentation()
                .description("GitHub Repository - Documentation and Source Code")
                .url("https://github.com/abcd1924/SecureAuth-API");
    }
}
