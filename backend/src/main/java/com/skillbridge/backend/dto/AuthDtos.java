package com.skillbridge.backend.dto;

import com.skillbridge.backend.entity.User;
import jakarta.validation.constraints.*;

/** Auth endpoints F1: register, login, password flows. */
public final class AuthDtos {

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be at least 8 characters") String password) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 72) String newPassword) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(@NotBlank String resetToken,
                                       @NotBlank @Size(min = 8, max = 72) String newPassword) {}

    public record AuthResponse(String token, UserResponse user) {}

    public record UserResponse(Long id, String fullName, String email, User.Role role, User.Status status) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getStatus());
        }
    }
}
