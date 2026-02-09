package backend.secureauthapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import backend.secureauthapi.dto.ChangePasswordRequest;
import backend.secureauthapi.dto.MessageResponse;
import backend.secureauthapi.dto.UpdateProfileRequest;
import backend.secureauthapi.dto.UserResponse;
import backend.secureauthapi.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {

        UserResponse userResponse = userService.getCurrentUser();

        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse userResponse = userService.updateProfile(request);

        return ResponseEntity.ok(userResponse);
    }

    /**
     * Changes the password of the currently authenticated user.
     * Requires the current password for verification.
     * All active sessions will be terminated after password change.
     */
    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        MessageResponse messageResponse = userService.changePassword(request);

        return ResponseEntity.ok(messageResponse);
    }

    /**
     * Deactivates the account of the currently authenticated user.
     * This is a soft delete - the account is marked as inactive but not removed.
     * All active sessions will be terminated.
     */
    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deactivateAccount() {

        MessageResponse messageResponse = userService.deactivateAccount();

        return ResponseEntity.ok(messageResponse);
    }
}