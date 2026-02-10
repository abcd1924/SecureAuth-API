package backend.secureauthapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import backend.secureauthapi.dto.request.LoginRequest;
import backend.secureauthapi.dto.request.LogoutRequest;
import backend.secureauthapi.dto.request.RefreshTokenRequest;
import backend.secureauthapi.dto.request.RegisterRequest;
import backend.secureauthapi.dto.response.LoginResponse;
import backend.secureauthapi.dto.response.MessageResponse;
import backend.secureauthapi.dto.response.RefreshTokenResponse;
import backend.secureauthapi.dto.response.UserResponse;
import backend.secureauthapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "Authentication",
    description = "User authentication and session management endpoints"
)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User successfully registered"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {

        UserResponse userResponse = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @Operation(summary = "Authenticate user and obtain JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "403", description = "Account is disabled")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String deviceInfo = extractDeviceInfo(httpRequest);
        String ipAddress = extractIpAddress(httpRequest);

        LoginResponse loginResponse = authService.login(request, deviceInfo, ipAddress);

        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Refresh access token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshTokenResponse refreshTokenResponse = authService.refreshToken(request);

        return ResponseEntity.ok(refreshTokenResponse);
    }

    @Operation(summary = "Logout user and invalidate refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {

        authService.logout(request);

        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    /**
     * Extracts device information from the User-Agent header.
     */
    private String extractDeviceInfo(HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");

        return (userAgent != null && !userAgent.isBlank()) ? userAgent : "Unknown Device";
    }

    /**
     * Extracts the client's IP address, handling proxied requests.
     */
    private String extractIpAddress(HttpServletRequest httpRequest) {
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String remoteAddr = httpRequest.getRemoteAddr();

        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "Unknown IP";
    }
}