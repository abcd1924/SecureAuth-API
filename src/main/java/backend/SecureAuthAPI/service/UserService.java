package backend.secureauthapi.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import backend.secureauthapi.dto.ChangePasswordRequest;
import backend.secureauthapi.dto.MessageResponse;
import backend.secureauthapi.dto.UpdateProfileRequest;
import backend.secureauthapi.dto.UserResponse;
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
}