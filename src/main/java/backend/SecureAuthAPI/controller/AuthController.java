package backend.secureauthapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import backend.secureauthapi.dto.LoginRequest;
import backend.secureauthapi.dto.LoginResponse;
import backend.secureauthapi.dto.LogoutRequest;
import backend.secureauthapi.dto.MessageResponse;
import backend.secureauthapi.dto.RefreshTokenRequest;
import backend.secureauthapi.dto.RefreshTokenResponse;
import backend.secureauthapi.dto.RegisterRequest;
import backend.secureauthapi.dto.UserResponse;
import backend.secureauthapi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {

        UserResponse userResponse = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String deviceInfo = extractDeviceInfo(httpRequest);
        String ipAddress = extractIpAddress(httpRequest);

        LoginResponse loginResponse = authService.login(request, deviceInfo, ipAddress);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshTokenResponse refreshTokenResponse = authService.refreshToken(request);

        return ResponseEntity.ok(refreshTokenResponse);
    }

    @PostMapping("/logout")
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