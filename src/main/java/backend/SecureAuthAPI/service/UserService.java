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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    /**
     * User methods
     */

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {

        User user = getCurrentAuthenticatedUser();

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest updateProfileRequest) {

        User user = getCurrentAuthenticatedUser();

        user.setName(updateProfileRequest.name());

        User updatedUser = userRepository.save(user);

        return userMapper.toUserResponse(updatedUser);
    }

    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest changePasswordRequest) {

        User user = getCurrentAuthenticatedUser();

        if (!passwordEncoder.matches(changePasswordRequest.currentPassword(),
                user.getPasswordHash())) {
            throw new PasswordChangeException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userRepository.save(user);

        refreshTokenService.revokeAllTokensByUser(user.getId());

        return new MessageResponse("Password changed successfully. Please login again.");
    }

    @Transactional
    public MessageResponse deactivateAccount() {

        User user = getCurrentAuthenticatedUser();

        user.setActive(false);
        userRepository.save(user);

        refreshTokenService.revokeAllTokensByUser(user.getId());

        return new MessageResponse("Account deactivated successfully");
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

        return userRepository
                .findAll(pageable)
                .map(userMapper::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {

        return userRepository.findById(id)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new UserNotFoundException());
    }

    @Transactional
    public UserResponse updateUserRole(Long id, UpdateUserRoleRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException());

        user.setRole(request.role());
        User updatedUser = userRepository.save(user);

        // Revoke all active sessions and force user to login again
        refreshTokenService.revokeAllTokensByUser(id);

        return userMapper.toUserResponse(updatedUser);
    }

    @Transactional
    public MessageResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException());

        user.setActive(request.active());
        userRepository.save(user);

        // If deactivating, revoke all active sessions
        if (!request.active()) {
            refreshTokenService.revokeAllTokensByUser(id);
        }

        String message = request.active()
                ? "User account activated successfully"
                : "User account deactivated successfully";

        return new MessageResponse(message);
    }
}