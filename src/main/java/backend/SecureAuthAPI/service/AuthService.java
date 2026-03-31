package backend.secureauthapi.service;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import backend.secureauthapi.dto.request.LoginRequest;
import backend.secureauthapi.dto.request.LogoutRequest;
import backend.secureauthapi.dto.request.RefreshTokenRequest;
import backend.secureauthapi.dto.request.RegisterRequest;
import backend.secureauthapi.dto.response.LoginResponse;
import backend.secureauthapi.dto.response.MessageResponse;
import backend.secureauthapi.dto.response.RefreshTokenResponse;
import backend.secureauthapi.dto.response.UserResponse;
import backend.secureauthapi.exception.auth.InvalidCredentialsException;
import backend.secureauthapi.exception.user.UserAlreadyExistsException;
import backend.secureauthapi.mapper.UserMapper;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;
import backend.secureauthapi.security.UserDetailsImpl;
import backend.secureauthapi.security.jwt.JwtUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final UserDetailsService userDetailsService;
    private final MeterRegistry meterRegistry;

    @Value("${security.jwt.expiration-ms}")
    private Long jwtExpirationMs;

    @Transactional
    public UserResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new UserAlreadyExistsException();
        }

        User user = userMapper.toEntity(registerRequest,
                passwordEncoder.encode(registerRequest.password()));

        User savedUser = userRepository.save(user);

        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public LoginResponse login(LoginRequest loginRequest, String deviceInfo, String ipAddress) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(),
                            loginRequest.password()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            String accessToken = jwtUtils.generateAccessToken(userDetails);

            User user = userRepository.findByEmail(loginRequest.email())
                    .orElseThrow(() -> new InvalidCredentialsException());

            String refreshToken = refreshTokenService.issueRefreshToken(user, deviceInfo, ipAddress);

            Instant expiresAt = Instant.now().plusMillis(jwtExpirationMs);

            UserResponse userResponse = userMapper.toUserResponse(user);
            
            outcome = "success";
            return new LoginResponse(accessToken, refreshToken, expiresAt, userResponse);
        } finally {
            meterRegistry.counter("auth.login.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("auth.login.duration")
                    .description("Login request duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));
        }
    }

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            User user = refreshTokenService.getUserFromRefreshToken(refreshTokenRequest.refreshToken());

            String newRefreshToken = refreshTokenService.rotateAndIssueRefreshToken(refreshTokenRequest.refreshToken());

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            String newAccessToken = jwtUtils.generateAccessToken(userDetails);

            Instant expiresAt = Instant.now().plusMillis(jwtExpirationMs);

            outcome = "success";
            return new RefreshTokenResponse(newAccessToken, newRefreshToken, expiresAt);
        } finally {
            meterRegistry.counter("auth.refresh.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("auth.refresh.duration")
                    .description("Refresh token request duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry));
        }
    }

    @Transactional
    public MessageResponse logout(LogoutRequest logoutRequest) {

        refreshTokenService.revokeToken(logoutRequest.refreshToken());

        meterRegistry.counter("auth.logout").increment();

        return new MessageResponse("Logout successful");
    }
}
