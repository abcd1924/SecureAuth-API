package backend.secureauthapi.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import backend.secureauthapi.dto.request.ChangePasswordRequest;
import backend.secureauthapi.dto.request.UpdateProfileRequest;
import backend.secureauthapi.dto.request.UpdateUserRoleRequest;
import backend.secureauthapi.dto.request.UpdateUserStatusRequest;
import backend.secureauthapi.dto.response.MessageResponse;
import backend.secureauthapi.dto.response.UserResponse;
import backend.secureauthapi.exception.user.PasswordChangeException;
import backend.secureauthapi.exception.user.UserNotFoundException;
import backend.secureauthapi.mapper.UserMapper;
import backend.secureauthapi.model.User;
import backend.secureauthapi.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final MeterRegistry meterRegistry;

    /**
     * User methods
     */

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            User user = getCurrentAuthenticatedUser();

            outcome = "success";
            return userMapper.toUserResponse(user);
        } finally {
            meterRegistry.counter("user.current.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("user.current.duration")
                    .description("Current user lookup duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry));
        }
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest updateProfileRequest) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            User user = getCurrentAuthenticatedUser();

            user.setName(updateProfileRequest.name());

            User updatedUser = userRepository.save(user);

            outcome = "success";
            return userMapper.toUserResponse(updatedUser);
        } finally {
            meterRegistry.counter("user.profile.update.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("user.profile.update.duration")
                    .description("Profile update duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry));
        }
    }

    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest changePasswordRequest) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            User user = getCurrentAuthenticatedUser();

            if (!passwordEncoder.matches(changePasswordRequest.currentPassword(),
                    user.getPasswordHash())) {
                throw new PasswordChangeException("Current password is incorrect");
            }

            user.setPasswordHash(passwordEncoder.encode(changePasswordRequest.newPassword()));
            userRepository.save(user);

            refreshTokenService.revokeAllTokensByUser(user.getId());

            outcome = "success";
            return new MessageResponse("Password changed successfully. Please login again.");
        } finally {
            meterRegistry.counter("user.password.change.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("user.password.change.duration")
                    .description("Password change duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry));
        }
    }

    @Transactional
    public MessageResponse deactivateAccount() {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            User user = getCurrentAuthenticatedUser();

            user.setActive(false);
            userRepository.save(user);

            refreshTokenService.revokeAllTokensByUser(user.getId());

            meterRegistry.counter("user.account.deactivated").increment();
            outcome = "success";

            return new MessageResponse("Account deactivated successfully");
        } finally {
            meterRegistry.counter("user.account.deactivate.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("user.account.deactivate.duration")
                    .description("Account deactivation duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry));
        }
    }

    private User getCurrentAuthenticatedUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException());
    }

    /*
     * Admin Methods
     */

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            Page<UserResponse> users = userRepository
                    .findAll(pageable)
                    .map(userMapper::toUserResponse);

            outcome = "success";
            return users;
        } finally {
            meterRegistry.counter("user.admin.list.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("user.admin.list.duration")
                    .description("Admin user list retrieval duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry));
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            UserResponse user = userRepository.findById(id)
                    .map(userMapper::toUserResponse)
                    .orElseThrow(() -> new UserNotFoundException());

            outcome = "success";
            return user;
        } finally {
            meterRegistry.counter("user.admin.detail.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("user.admin.detail.duration")
                    .description("Admin user detail retrieval duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry));
        }
    }

    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException());

            user.setRole(request.role());
            User updatedUser = userRepository.save(user);

            // Revoke all active sessions and force user to login again
            refreshTokenService.revokeAllTokensByUser(id);

            outcome = "success";
            return userMapper.toUserResponse(updatedUser);
        } finally {
            meterRegistry.counter("user.admin.role.update.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("user.admin.role.update.duration")
                    .description("Admin user role update duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry));
        }
    }

    @Transactional
    public MessageResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException());

            user.setActive(request.active());
            userRepository.save(user);

            // If deactivating, revoke all active sessions
            if (!request.active()) {
                refreshTokenService.revokeAllTokensByUser(id);
                meterRegistry.counter("user.account.deactivated.by.admin").increment();
            } else {
                meterRegistry.counter("user.account.activated.by.admin").increment();
            }

            String message = request.active()
                    ? "User account activated successfully"
                    : "User account deactivated successfully";

            outcome = "success";
            return new MessageResponse(message);
        } finally {
            meterRegistry.counter("user.admin.status.update.attempts", "outcome", outcome).increment();
            sample.stop(Timer.builder("user.admin.status.update.duration")
                    .description("Admin user status update duration")
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry));
        }
    }
}
