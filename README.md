# SecureAuthAPI
> **Project Status: Core Features Implemented**  
> This project has successfully implemented the core authentication and authorization features. The API is functional and follows enterprise-grade best practices. Additional features and improvements are planned (see Roadmap).

## Overview
**SecureAuthAPI** is an enterprise-grade authentication and authorization REST API built with **Java 21** and **Spring Boot 3.5.9**. This project demonstrates production-ready backend development practices commonly used in modern enterprise applications.

The API provides a complete authentication system with JWT-based token management, role-based authorization, and secure user management—designed to implements real-world authentication services used in production environments.

### Project Goals
- Build a production-ready authentication API with enterprise standards
- Implement JWT authentication with access and refresh token patterns
- Apply clean architecture principles with proper separation of concerns
- Follow security best practices for credential management and token handling
- Demonstrate professional Java/Spring Boot development skills

---

## Technology Stack
### Core Technologies
- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.5.9** - Enterprise application framework
- **Spring Security** - Authentication and authorization framework
- **Spring Data JPA** - Data persistence layer
- **Hibernate** - ORM implementation

### Database
- **MySQL** - Production-grade relational database
- **JDBC** - Database connectivity

### Security & Authentication
- **JWT (JSON Web Tokens)** - Stateless authentication
- **BCrypt** - Password hashing algorithm
- **JJWT** - Java JWT library

### Development Tools
- **Maven** - Dependency management and build tool
- **Lombok** - Boilerplate code reduction
- **Spring Boot DevTools** - Development productivity

---

## Architecture
This project follows a layered architecture pattern with clear separation of concerns:

```
backend.SecureAuthAPI/
├── config/           # Configuration classes (Security, JWT, etc.)
├── controller/       # REST API endpoints
├── dto/              # Data Transfer Objects
├── model/            # JPA entities (database models)
├── exception/        # Custom exceptions and global error handling
├── repository/       # Data access layer
├── security/         # Security filters, JWT utilities
└── service/          # Business logic layer
```
### Authentication Flow
```mermaid
sequenceDiagram
    actor Client
    participant AuthController
    participant AuthService
    participant AuthManager as AuthenticationManager
    participant JwtUtils
    participant RefreshTokenService
    participant Database

    Client->>AuthController: POST /api/auth/login {email, password}
    AuthController->>AuthService: login(request, deviceInfo, ip)
    
    rect rgb(9, 44, 75)
        Note right of AuthService: Spring Security Validation
        AuthService->>AuthManager: authenticate(email, password)
        AuthManager-->>AuthService: Authentication (UserDetails)
    end

    AuthService->>JwtUtils: generateAccessToken(UserDetails)
    JwtUtils-->>AuthService: accessToken (JWT)

    AuthService->>Database: findByEmail(email)
    Database-->>AuthService: User entity

    AuthService->>RefreshTokenService: issueRefreshToken(user, device, ip)
    
    rect rgb(14, 84, 14)
        Note right of RefreshTokenService: Security Practice: Opaque Token
        RefreshTokenService->>RefreshTokenService: generate rawToken
        RefreshTokenService->>RefreshTokenService: hash(rawToken) -> tokenHash
        RefreshTokenService->>Database: save(tokenHash, expiresAt)
    end
    
    RefreshTokenService-->>AuthService: rawRefreshToken
    
    AuthService-->>AuthController: LoginResponse(Tokens + UserInfo)
    AuthController-->>Client: 200 OK {accessToken, refreshToken}
```

## Configuration
### Application Properties
The application uses environment variables for configuration, following the 12-factor app methodology:

```properties
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=db_secure_auth
DB_USER=root
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your_secret_key_minimum_256_bits
JWT_EXPIRATION_MS=900000          # 15 minutes
JWT_REFRESH_EXPIRATION_MS=604800000  # 7 days
```
---


## Features Implemented
### Phase 1: Foundation (Completed)
- [x] Project initialization with Spring Boot 3.5.9
- [x] MySQL database configuration
- [x] JWT configuration and utilities
- [x] Layered architecture setup
- [x] Entity models (User, RefreshToken, Role)
- [x] Repository layer (UserRepository, RefreshTokenRepository)
- [x] Service layer (AuthService, UserService, RefreshTokenService)

### Phase 2: Core Features (Completed)
- [x] User registration with email validation
- [x] User login with JWT generation (Access + Refresh tokens)
- [x] Token refresh mechanism
- [x] Logout with token invalidation
- [x] BCrypt password encryption

### Phase 3: Security & Authorization (Completed)
- [x] Spring Security configuration
- [x] JWT authentication filter
- [x] Role-based authorization (USER, ADMIN, AUDITOR, SUPPORT)
- [x] Global exception handling with custom exceptions
- [x] Input validation with Bean Validation

### Phase 4: Advanced Features (Completed)
- [x] User profile management (GET /api/users/me)
- [x] Admin endpoints (user management, role assignment)
- [x] User enable/disable functionality
- [x] User role update functionality

### Phase 5: Documentation & Testing (In Progress)
- [X] Swagger/OpenAPI documentation
- [X] Unit tests
- [X] Controller Integration tests
- [X] Postman collection
- [ ] Docker deployment guide

---

## API Endpoints
### Authentication Endpoints
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register a new user | No |
| POST | `/api/auth/login` | Login and receive JWT tokens | No |
| POST | `/api/auth/refresh` | Refresh access token using refresh token | No |
| POST | `/api/auth/logout` | Logout and invalidate refresh token | Yes |

### User Endpoints
| Method | Endpoint | Description | Auth Required | Role Required |
|--------|----------|-------------|---------------|---------------|
| GET | `/api/users/me` | Get current user profile | Yes | Any authenticated user |
| PATCH | `/api/users/me` | Update current user name | Yes | Any authenticated user |
| PUT | `/api/users/me/password` | Change current user password | Yes | Any authenticated user |
| DELETE | `/api/users/me` | Deactivates current user account | Yes | Any authenticated user |

### Admin Endpoints
| Method | Endpoint | Description | Auth Required | Role Required |
|--------|----------|-------------|---------------|---------------|
| GET | `/api/admin/users` | Get all users | Yes | ADMIN |
| GET | `/api/admin/users/{id}` | Get user by ID | Yes | ADMIN |
| PATCH | `/api/admin/users/{id}/role` | Update user role | Yes | ADMIN |
| PATCH | `/api/admin/users/{id}/status` | Activates or deactivates a user account | Yes | ADMIN |

---

## API Documentation
### Interactive API Documentation (Swagger UI)

This API includes interactive documentation powered by Swagger/OpenAPI, allowing you to explore and test all endpoints directly from your browser.

**Access Swagger UI:** `http://localhost:8080/swagger-ui.html`

**Features:**
- Browse all available endpoints organized by tags
- View request/response schemas with examples
- Test endpoints directly from the browser
- Authenticate with JWT to test protected endpoints

### How to use Swagger UI
1. Start the application (use Getting Started section)
2. Open Swagger UI in your browser: `http://localhost:8080/swagger-ui.html`
3. Authenticate (for protected endpoints):
- Click the **"Authorize"** button (🔓 icon) at the top right
- Register first using `POST /api/auth/register` to create a user
- Login first using `POST /api/auth/login` to obtain an access token
- Copy the `accessToken` from the response
- Paste it in the authorization modal (without "Bearer " prefix)
- Click **"Authorize"**
- Now you can test protected endpoints

4. Test an endpoint:
- Expand any endpoint (e.g., `GET /api/users/me`)
- Click **"Try it out"**
- Fill in any required parameters
- Click **"Execute"**
- View the response

<div align = "center">
  <h2>Swagger UI Screenshot</h2>
  <img src="docs/images/swagger-ui-screenshot.png" alt="Swagger UI Screenshot" width = "600">
  <p><em>Interactive API documentation showing all available endpoints</em></p> 
</div>

### OpenAPI Specification
The raw OpenAPI 3.0 specification is available at: `http://localhost:8080/v3/api-docs`

---

## Postman Collection
A ready-to-use Postman Collection and Environment are available in `docs/postman/`.

- Collection: `docs/postman/SecureAuthAPI.postman_collection.json`
- Environment: `docs/postman/SecureAuthAPI.postman_environment.json`

### Quick Start

1. Import both files into Postman.
2. Select the `SecureAuth - Local` environment.
3. Run `Authentication > login` to capture `access_token` and `refresh_token` automatically.
4. Call protected endpoints from `User Profile` and `Admin Management`.

### Notes

- Tokens are intentionally blank in the committed environment template.
- Use `Setup and Testing` folder requests to validate `401 Unauthorized` and `403 Forbidden` behavior.

---

## Rate Limiting
This API includes per-IP rate limiting to reduce abuse and protect authentication endpoints.

### Default Limits
- **Auth endpoints** (`/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`): **10 requests/minute per IP**
- **Other API endpoints** (`/api/**`): **60 requests/minute per IP**

### Configuration
Limits are configurable through environment variables (no code changes required):

```properties
RATE_LIMIT_AUTH_REQUESTS_PER_MINUTE=10
RATE_LIMIT_API_REQUESTS_PER_MINUTE=60
```

If values are not provided, the API uses the defaults above.

### Behavior When Limit Is Exceeded
- Status: **429 Too Many Requests**
- JSON error response:

```json
{
  "status": 429,
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again later.",
  "path": "/api/auth/login",
  "timestamp": "2026-03-27T12:34:56Z"
}
```

- Response headers:
  - `RateLimit-Limit`: endpoint group limit (for example, `10` or `60`)
  - `RateLimit-Remaining`: remaining tokens in current window
  - `RateLimit-Reset`: seconds until next token is available
  - `Retry-After`: seconds to wait before retrying (present on blocked requests)

### Quick Verification
Run this from a terminal to trigger the auth limit:

```bash
for i in {1..11}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"user@example.com","password":"WrongPass123!"}'
done
```

Expected result:
- First requests return `401` (invalid credentials but still counted by the limiter)
- Once the auth bucket is exhausted, response becomes `429`

---

## Monitoring & Metrics
This API includes Spring Boot Actuator and Micrometer Prometheus integration to provide health probes, runtime metrics, and business metrics for authentication flows.

### Monitoring Setup
Dependencies used:
- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`

Main actuator configuration:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.probes.enabled=true
management.endpoint.health.roles=ADMIN
management.endpoint.health.show-details=when-authorized
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
```

### Endpoint Access and Security
Monitoring endpoints follow least-privilege rules:

| Endpoint | Access | Purpose |
|---|---|---|
| `/actuator/health` | Public | General health status for platform checks |
| `/actuator/health/liveness` | Public | Liveness probe |
| `/actuator/health/readiness` | Public | Readiness probe |
| `/actuator/info` | Public (if exposed) | Build/application info |
| `/actuator/metrics` | ADMIN only | Structured metric names and details |
| `/actuator/prometheus` | ADMIN only | Prometheus scrape output |

Security notes:
- Public probe endpoints are intentionally available for container orchestrators and platform health checks.
- Sensitive metric endpoints are restricted to authenticated users with `ROLE_ADMIN`.
- Existing API endpoint protections remain unchanged.

### Custom Business Metrics
Custom metrics are emitted for security-critical flows, including:
- Login attempts and latency (`auth.login.*`)
- Token refresh attempts and latency (`auth.refresh.*`, `token.refresh.*`)
- Account action and deactivation metrics (`user.account.*`)

### Quick Verification Commands
1. Verify public health endpoint:

```bash
curl -i http://localhost:8080/actuator/health
```

2. Verify public liveness/readiness probes:

```bash
curl -i http://localhost:8080/actuator/health/liveness
curl -i http://localhost:8080/actuator/health/readiness
```

3. Verify protected metrics endpoint (requires ADMIN token):

```bash
curl -i http://localhost:8080/actuator/metrics \
  -H "Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN"
```

4. Verify Prometheus scrape output (requires ADMIN token):

```bash
curl -i http://localhost:8080/actuator/prometheus \
  -H "Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN"
```

Expected checks:
- `/actuator/health*` responds successfully for healthy state.
- `/actuator/metrics` and `/actuator/prometheus` return `401` without token.
- Prometheus output contains `# HELP` and `# TYPE` lines.

### Example Prometheus Scrape Config
If Prometheus runs outside the API process, set the scrape target to this service:

```yaml
scrape_configs:
  - job_name: secureauthapi
    metrics_path: /actuator/prometheus
    scheme: http
    static_configs:
      - targets: ["localhost:8080"]
```

If your Prometheus setup supports auth headers, include an ADMIN bearer token for this endpoint.

### Degraded and Unhealthy Health States
Health behavior is also validated when dependencies fail.
- Integration coverage includes a failing health indicator scenario that verifies the API reports `DOWN` with `503 Service Unavailable` for `/actuator/health`.
- This confirms probe behavior is reliable for outage detection.

### Troubleshooting Monitoring Misconfigurations
1. `500` on `/actuator/prometheus` in tests:
- Ensure Prometheus export is enabled in the active profile.
- Use:

```properties
management.prometheus.metrics.export.enabled=true
```

2. Endpoint not found (`404`):
- Confirm exposure includes `prometheus` and `metrics`.

3. `401` on `/actuator/metrics` or `/actuator/prometheus`:
- This is expected without an ADMIN token.
- Use a valid access token for a user with `ROLE_ADMIN`.

4. Probe details missing in `/actuator/health`:
- Details are intentionally limited by `management.endpoint.health.show-details=when-authorized`.
- Authenticate with an authorized role to view component details.

---

## Getting Started
### Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- **MySQL 8.0+**
- IDE with Java support (IntelliJ IDEA, Eclipse, VS Code)

### Installation

1. **Clone the repository**
   ```bash
   git clone 
   cd SecureAuthAPI
   ```

2. **Create MySQL database**
   ```sql
   CREATE DATABASE db_secure_auth;
   ```

3. **Configure environment variables**
   
   Create a `.env` file or set the following environment variables:
   ```properties
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=db_secure_auth
   DB_USER=root
   DB_PASSWORD=your_password
   JWT_SECRET=your_secret_key_minimum_256_bits_long_for_security
   JWT_EXPIRATION_MS=900000
   JWT_REFRESH_EXPIRATION_MS=604800000
   ```

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

6. **Access the API**
   
   The API will be available at: `http://localhost:8080`

### Example Usage

**Register a new user:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

**Refresh token:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

**Logout:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

**Access protected endpoint:**
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Future Improvements

### Planned Features
- [ ] Email verification for new registrations
- [ ] Password reset functionality
- [ ] Account lockout after failed login attempts
- [ ] Audit logging for security events
- [X] Rate limiting for API endpoints
- [X] Swagger/OpenAPI documentation
- [X] Comprehensive test suite (unit + integration)
- [X] Performance monitoring and metrics
- [ ] Docker containerization
- [ ] CI/CD pipeline setup

---

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

This project is developed for educational and portfolio purposes.

---

## Author
**Valentino Castro**
- LinkedIn: https://www.linkedin.com/in/valentino-castro-0a929831a
- GitHub: https://github.com/abcd1924

---

## Acknowledgments
This project was built to demonstrate enterprise-level Java backend development skills, following industry best practices commonly used in production environments at technology companies in the United States.

---

**Note**: This is a portfolio project demonstrating enterprise-level Java backend development. The core authentication and authorization features are fully implemented and functional. Future enhancements are planned to further showcase advanced backend development skills.
