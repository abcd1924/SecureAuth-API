package backend.secureauthapi.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import backend.secureauthapi.dto.MessageResponse;
import backend.secureauthapi.dto.UpdateUserRoleRequest;
import backend.secureauthapi.dto.UpdateUserStatusRequest;
import backend.secureauthapi.dto.UserResponse;
import backend.secureauthapi.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    /**
     * Retrieves a paginated list of all users in the system.
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        Page<UserResponse> users = userService.getAllUsers(pageable);

        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable @Positive(message = "Id must be positive") Long id) {

        UserResponse user = userService.getUserById(id);

        return ResponseEntity.ok(user);
    }

    /**
     * Updates the role of a specific user.
     * 
     * All active refresh tokens for the user will be revoked to ensure
     * the new role takes effect immediately on next login.
     */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable @Positive(message = "Id must be positive") Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        UserResponse userResponse = userService.updateUserRole(id, request);

        return ResponseEntity.ok(userResponse);
    }

    /**
     * Activates or deactivates a user account.
     * 
     * When deactivating:
     * - All active refresh tokens are revoked
     * - User cannot log in until reactivated
     * - Existing access tokens remain valid until expiration
     * 
     * This is a soft delete - user data is preserved.
     */
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<MessageResponse> updateUserStatus(
            @PathVariable @Positive(message = "Id must be positive") Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        MessageResponse messageResponse = userService.updateUserStatus(id, request);

        return ResponseEntity.ok(messageResponse);
    }
}