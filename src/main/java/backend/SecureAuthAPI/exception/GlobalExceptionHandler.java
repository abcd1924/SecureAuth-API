package backend.secureauthapi.exception;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import backend.secureauthapi.exception.auth.InvalidCredentialsException;
import backend.secureauthapi.exception.token.InvalidRefreshTokenException;
import backend.secureauthapi.exception.token.RefreshTokenReuseException;
import backend.secureauthapi.exception.user.PasswordChangeException;
import backend.secureauthapi.exception.user.UserAlreadyExistsException;
import backend.secureauthapi.exception.user.UserInactiveException;
import backend.secureauthapi.exception.user.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for the entire application.
 * Catches all exceptions thrown by controllers and services, converts them to
 * standardized HTTP responses with appropriate status codes and error messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        /**
         * Handles invalid credentials during login.
         * Returns a generic message to prevent user enumeration attacks.
         */
        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(
                        InvalidCredentialsException ex,
                        HttpServletRequest request) {

                logger.warn("Invalid credentials attempt on path: {}", request.getRequestURI());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .message("Invalid credentials")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("INVALID_CREDENTIALS")
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        /**
         * Handles authentication failures from Spring Security.
         * This catches BadCredentialsException thrown by AuthenticationManager when credentials are
         * invalid (wrong password or non-existent user).
         * Returns a generic message to prevent user enumeration attacks.
         */
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(
                        BadCredentialsException ex,
                        HttpServletRequest request) {

                logger.warn("Authentication failed on path: {}", request.getRequestURI());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .message("Invalid email or password")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("INVALID_CREDENTIALS")
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        /**
         * Handles attempts to authenticate with a disabled (inactive) account.
         * Spring Security throws this when UserDetails.isEnabled() returns false.
         */
        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<ErrorResponse> handleDisabledException(
                        DisabledException ex,
                        HttpServletRequest request) {

                logger.warn("Disabled account login attempt on path: {}", request.getRequestURI());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.FORBIDDEN.value())
                                .message("User account is inactive")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("USER_INACTIVE")
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        /**
         * Handles authorization failures (Spring Security 6+).
         * Thrown when an authenticated user lacks the required role/permission.
         * Example: USER role trying to access ADMIN endpoint.
         */
        @ExceptionHandler(AuthorizationDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAuthorizationDenied(
                        AuthorizationDeniedException ex,
                        HttpServletRequest request) {

                logger.warn("Authorization denied on path: {} - Reason: {}",
                                request.getRequestURI(), ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.FORBIDDEN.value())
                                .message("Access denied. Insufficient permissions.")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("ACCESS_DENIED")
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        /**
         * Handles access denied exceptions (Spring Security 5 and fallback).
         * Similar to AuthorizationDeniedException but for older versions.
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(
                        AccessDeniedException ex,
                        HttpServletRequest request) {

                logger.warn("Access denied on path: {} - Reason: {}",
                                request.getRequestURI(), ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.FORBIDDEN.value())
                                .message("Access denied. Insufficient permissions.")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("ACCESS_DENIED")
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        /**
         * Handles requests to protected resources without authentication.
         * Thrown when no credentials are provided for a secured endpoint.
         */
        @ExceptionHandler(InsufficientAuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleInsufficientAuthentication(
                        InsufficientAuthenticationException ex,
                        HttpServletRequest request) {

                logger.warn("Insufficient authentication on path: {}", request.getRequestURI());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .message("Full authentication is required to access this resource")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("AUTHENTICATION_REQUIRED")
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        /**
         * Handles authentication failures not caught by more specific handlers.
         * This is a catch-all for authentication-related issues.
         */
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(
                        AuthenticationException ex,
                        HttpServletRequest request) {

                logger.warn("Authentication failed on path: {} - Reason: {}",
                                request.getRequestURI(), ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .message("Authentication failed. Please login again.")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("AUTHENTICATION_FAILED")
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        /**
         * Handles invalid or expired refresh tokens.
         * Provides specific feedback since the user already possesses the token.
         */
        @ExceptionHandler(InvalidRefreshTokenException.class)
        public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
                        InvalidRefreshTokenException ex,
                        HttpServletRequest request) {

                logger.debug("Invalid refresh token: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("INVALID_REFRESH_TOKEN")
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        /**
         * Handles refresh token reuse detection (security incident).
         * This indicates a potential attack - all user sessions are revoked.
         */
        @ExceptionHandler(RefreshTokenReuseException.class)
        public ResponseEntity<ErrorResponse> handleRefreshTokenReuse(
                        RefreshTokenReuseException ex,
                        HttpServletRequest request) {

                logger.error("SECURITY ALERT - Token reuse detected on path: {}",
                                request.getRequestURI());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.FORBIDDEN.value())
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("TOKEN_REUSE_DETECTED")
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        /**
         * Handles user not found exceptions.
         * Only used in authenticated contexts where revealing user existence is safe.
         */
        @ExceptionHandler(UserNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleUserNotFound(
                        UserNotFoundException ex,
                        HttpServletRequest request) {

                logger.debug("User not found: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.NOT_FOUND.value())
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("USER_NOT_FOUND")
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        /**
         * Handles duplicate user registration attempts.
         * Provides specific feedback to improve user experience during registration.
         */
        @ExceptionHandler(UserAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
                        UserAlreadyExistsException ex,
                        HttpServletRequest request) {

                logger.debug("User already exists: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.CONFLICT.value())
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("USER_ALREADY_EXISTS")
                                .build();

                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        /**
         * Handles incorrect password during password change operations.
         * Returns 400 BAD_REQUEST since this is a validation error, not an authentication failure.
         */
        @ExceptionHandler(PasswordChangeException.class)
        public ResponseEntity<ErrorResponse> handleInvalidPassword(
                        PasswordChangeException ex,
                        HttpServletRequest request) {

                logger.debug("Invalid password attempt: {}", ex.getMessage());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("INVALID_PASSWORD")
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Handles attempts to access resources with an inactive user account.
         * Returns 403 FORBIDDEN since the account exists but access is denied.
         */
        @ExceptionHandler(UserInactiveException.class)
        public ResponseEntity<ErrorResponse> handleUserInactive(
                        UserInactiveException ex,
                        HttpServletRequest request) {

                logger.warn("Inactive user access attempt on path: {}", request.getRequestURI());

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.FORBIDDEN.value())
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("USER_INACTIVE")
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        /**
         * Handles validation errors from @Valid annotations on request DTOs.
         * Returns the first validation error message for simplicity.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationErrors(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                String errorMessage = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .findFirst()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .orElse("Validation failed");

                logger.debug("Validation error: {}", errorMessage);

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.BAD_REQUEST.value())
                                .message(errorMessage)
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("VALIDATION_ERROR")
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Handles all other unexpected exceptions.
         * Logs the full stack trace internally but returns a generic message to the
         * client to avoid exposing sensitive information.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {

                logger.error("Unexpected error on path: {}", request.getRequestURI(), ex);

                ErrorResponse error = ErrorResponse.builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .message("An unexpected error occurred")
                                .timestamp(Instant.now())
                                .path(request.getRequestURI())
                                .errorCode("INTERNAL_ERROR")
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
}