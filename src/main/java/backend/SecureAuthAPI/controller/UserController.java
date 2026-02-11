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
import backend.secureauthapi.dto.request.ChangePasswordRequest;
import backend.secureauthapi.dto.request.UpdateProfileRequest;
import backend.secureauthapi.dto.response.MessageResponse;
import backend.secureauthapi.dto.response.UserResponse;
import backend.secureauthapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "User",
    description = "User management endpoints for authenticated users"
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {

        UserResponse userResponse = userService.getCurrentUser();

        return ResponseEntity.ok(userResponse);
    }

    @Operation(summary = "Update current user profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse userResponse = userService.updateProfile(request);

        return ResponseEntity.ok(userResponse);
    }

    @Operation(
            summary = "Change current user password",
            description = "Requires the current password for verification. " +
                    "All active sessions will be terminated after password change.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
            @ApiResponse(responseCode = "401", description = "Wrong current password")
    })
    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        MessageResponse messageResponse = userService.changePassword(request);

        return ResponseEntity.ok(messageResponse);
    }

    @Operation(summary = "Deactivate current user account",
            description = "This is a soft delete - the account is marked as inactive but not removed. "
                    +
                    "All active sessions will be terminated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deactivated"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deactivateAccount() {

        MessageResponse messageResponse = userService.deactivateAccount();

        return ResponseEntity.ok(messageResponse);
    }
}