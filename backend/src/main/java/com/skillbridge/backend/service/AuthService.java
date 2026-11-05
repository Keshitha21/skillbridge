package com.skillbridge.backend.service;

import com.skillbridge.backend.dto.AuthDtos;
import com.skillbridge.backend.entity.PasswordReset;
import com.skillbridge.backend.entity.StudentProfile;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.exception.BadRequestException;
import com.skillbridge.backend.repository.PasswordResetRepository;
import com.skillbridge.backend.repository.StudentProfileRepository;
import com.skillbridge.backend.repository.UserRepository;
import com.skillbridge.backend.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** U1 / F1: register, login, logout, password recovery and change. */
@Service
public class AuthService {

    public record ResetOutcome(String resetToken, String fullName) {}

    private final UserRepository users;
    private final StudentProfileRepository profiles;
    private final PasswordResetRepository resetRepo;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthService(UserRepository users, StudentProfileRepository profiles, PasswordResetRepository resetRepo,
                       PasswordEncoder encoder, TokenService tokens) {
        this.users = users;
        this.profiles = profiles;
        this.resetRepo = resetRepo;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthDtos.UserResponse register(AuthDtos.RegisterRequest request) {
        if (users.existsByEmail(request.email().toLowerCase().trim())) {
            throw new BadRequestException("An account with this email already exists.");
        }
        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(request.email().toLowerCase().trim())
                .passwordHash(encoder.encode(request.password()))
                .role(User.Role.STUDENT)
                .status(User.Status.ACTIVE)
                .build();
        user = users.save(user);
        profiles.save(StudentProfile.builder().user(user).build()); // optional details can be added later
        return AuthDtos.UserResponse.from(user);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request, String rawToken) {
        User user = users.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Email or password is incorrect."));
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Email or password is incorrect.");
        }
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new BadRequestException("Account is suspended. Contact an administrator.");
        }
        if (rawToken != null && !rawToken.isBlank()) {
            tokens.revoke(rawToken); // single active session per login
        }
        String token = tokens.issue(user);
        return new AuthDtos.AuthResponse(token, AuthDtos.UserResponse.from(user));
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken != null) {
            tokens.revoke(rawToken);
        }
    }

    @Transactional
    public void changePassword(User me, AuthDtos.ChangePasswordRequest request) {
        if (!encoder.matches(request.currentPassword(), me.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        if (encoder.matches(request.newPassword(), me.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current one.");
        }
        me.setPasswordHash(encoder.encode(request.newPassword()));
        users.save(me);
        tokens.revokeAllFor(me.getId());
    }

    @Transactional
    public ResetOutcome forgotPassword(String email) {
        User user = users.findByEmail(email.toLowerCase().trim()).orElse(null);
        if (user == null || user.getStatus() != User.Status.ACTIVE) {
            return null; // do not leak whether the account exists
        }
        String token = tokens.issueResetToken(user);
        return new ResetOutcome(token, user.getFullName());
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        PasswordReset reset = resetRepo.findByTokenHash(tokens.sha256(request.resetToken()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link."));
        if (reset.isUsed()) {
            throw new BadRequestException("This reset link was already used.");
        }
        if (reset.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This reset link has expired. Please request a new one.");
        }
        User user = reset.getUser();
        user.setPasswordHash(encoder.encode(request.newPassword()));
        reset.setUsed(true);
        resetRepo.save(reset);
        users.save(user);
        tokens.revokeAllFor(user.getId()); // force re-login with the new password
    }
}
