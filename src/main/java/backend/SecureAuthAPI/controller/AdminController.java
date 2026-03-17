package backend.secureauthapi.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import backend.secureauthapi.dto.request.UpdateUserRoleRequest;
import backend.secureauthapi.dto.request.UpdateUserStatusRequest;
import backend.secureauthapi.dto.response.MessageResponse;
import backend.secureauthapi.dto.response.UserResponse;
import backend.secureauthapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin", description = "Administrative endpoints for user management (ADMIN role required)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @Operation(summary = "Get all users in the system", description = """
            Returns a paginated list of all users in the system.

            **Default sorting:** By creation date (newest first)
            **Default page size:** 10 users per page
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of users retrieved"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - Required ADMIN role")
    })
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<UserResponse> users = userService.getAllUsers(pageable);

        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - Required ADMIN role"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID", example = "1") @PathVariable @Positive(message = "Id must be positive") Long id) {

        UserResponse user = userService.getUserById(id);

        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Update user role", description = "Updates the role of a specific user. "
            + "All active refresh tokens for the user will be revoked to ensure the new role takes effect immediately on next login.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User role updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - Required ADMIN role"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @Parameter(description = "User ID", example = "1") @PathVariable @Positive(message = "Id must be positive") Long id,
            @Valid @RequestBody UpdateUserRoleRequest request) {

        UserResponse userResponse = userService.updateUserRole(id, request);

        return ResponseEntity.ok(userResponse);
    }

    @Operation(summary = "Update user status", description = """
            Activates or deactivates a user account.

            **When deactivating:**
            - All active refresh tokens are revoked
            - User cannot log in until reactivated
            - Existing access tokens remain valid until expiration

            **Note:** This is a soft delete - user data is preserved.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - Required ADMIN role"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<MessageResponse> updateUserStatus(
            @Parameter(description = "User ID", example = "1") @PathVariable @Positive(message = "Id must be positive") Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {

        MessageResponse messageResponse = userService.updateUserStatus(id, request);

        return ResponseEntity.ok(messageResponse);
    }
}
