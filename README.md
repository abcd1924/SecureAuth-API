# SecureAuthAPI

> **✅ Project Status: Core Features Implemented**  
> This project has successfully implemented the core authentication and authorization features. The API is functional and follows enterprise-grade best practices. Additional features and improvements are planned (see Roadmap).

## 📋 Overview

**SecureAuthAPI** is an enterprise-grade authentication and authorization REST API built with **Java 21** and **Spring Boot 3.5.9**. This project demonstrates production-ready backend development practices commonly used in modern enterprise applications.

The API provides a complete authentication system with JWT-based token management, role-based authorization, and secure user management—designed to implements real-world authentication services used in production environments.

### 🎯 Project Goals

- Build a **production-ready** authentication API with enterprise standards
- Implement **JWT authentication** with access and refresh token patterns
- Apply **clean architecture** principles with proper separation of concerns
- Follow **security best practices** for credential management and token handling
- Demonstrate **professional Java/Spring Boot development** skills

---

## 🛠️ Technology Stack

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

## 🏗️ Architecture

This project follows a **layered architecture** pattern with clear separation of concerns:

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

## ⚙️ Configuration

### Application Properties

The application uses environment variables for configuration, following the **12-factor app** methodology:

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


## ✨ Features Implemented

### ✅ Phase 1: Foundation (Completed)
- [x] Project initialization with Spring Boot 3.5.9
- [x] MySQL database configuration
- [x] JWT configuration and utilities
- [x] Layered architecture setup
- [x] Entity models (User, RefreshToken, Role)
- [x] Repository layer (UserRepository, RefreshTokenRepository)
- [x] Service layer (AuthService, UserService, RefreshTokenService)

### ✅ Phase 2: Core Features (Completed)
- [x] User registration with email validation
- [x] User login with JWT generation (Access + Refresh tokens)
- [x] Token refresh mechanism
- [x] Logout with token invalidation
- [x] BCrypt password encryption

### ✅ Phase 3: Security & Authorization (Completed)
- [x] Spring Security configuration
- [x] JWT authentication filter
- [x] Role-based authorization (USER, ADMIN, AUDITOR, SUPPORT)
- [x] Global exception handling with custom exceptions
- [x] Input validation with Bean Validation

### ✅ Phase 4: Advanced Features (Completed)
- [x] User profile management (GET /api/users/me)
- [x] Admin endpoints (user management, role assignment)
- [x] User enable/disable functionality
- [x] User role update functionality

### 🔄 Phase 5: Documentation & Testing (In Progress)
- [ ] Swagger/OpenAPI documentation
- [ ] Unit tests
- [ ] Integration tests
- [ ] Postman collection
- [ ] Docker deployment guide

---

## 📡 API Endpoints

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
| PATCH | `/api/users/me/password` | Change current user password | Yes | Any authenticated user |
| DELETE | `/api/users/me` | Deactivates current user account | Yes | Any authenticated user |

### Admin Endpoints

| Method | Endpoint | Description | Auth Required | Role Required |
|--------|----------|-------------|---------------|---------------|
| GET | `/api/admin/users` | Get all users | Yes | ADMIN |
| GET | `/api/admin/users/{id}` | Get user by ID | Yes | ADMIN |
| PATCH | `/api/admin/users/{id}/role` | Update user role | Yes | ADMIN |
| PATCH | `/api/admin/users/{id}/status` | Activates or deactivates a user account | Yes | ADMIN |

---

## 🚀 Getting Started

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

## 🔮 Future Improvements

### Planned Features
- [ ] Email verification for new registrations
- [ ] Password reset functionality
- [ ] Account lockout after failed login attempts
- [ ] Audit logging for security events
- [ ] Rate limiting for API endpoints
- [ ] Swagger/OpenAPI documentation
- [ ] Comprehensive test suite (unit + integration)
- [ ] Docker containerization
- [ ] CI/CD pipeline setup
- [ ] Performance monitoring and metrics

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

This project is developed for educational and portfolio purposes.

---

## 👤 Author

**Valentino Castro**
- LinkedIn: https://www.linkedin.com/in/valentino-castro-0a929831a
- GitHub: https://github.com/abcd1924

---

## 🙏 Acknowledgments

This project was built to demonstrate enterprise-level Java backend development skills, following industry best practices commonly used in production environments at technology companies in the United States.

---

**Note**: This is a portfolio project demonstrating enterprise-level Java backend development. The core authentication and authorization features are fully implemented and functional. Future enhancements are planned to further showcase advanced backend development skills.