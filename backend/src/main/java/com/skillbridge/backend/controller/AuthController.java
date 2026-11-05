package com.skillbridge.backend.controller;

import com.skillbridge.backend.dto.AuthDtos;
import com.skillbridge.backend.dto.CommonDtos;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.service.AuthService;
import com.skillbridge.backend.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** U1 / F1: register · login · logout · me · password flows. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final TokenService tokens;

    public AuthController(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/register")
    public ResponseEntity<CommonDtos.OkResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        auth.register(request);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true,
                "Account created. You can log in and complete your profile now."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                                       @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String previous = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
        return ResponseEntity.ok(auth.login(request, previous));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonDtos.OkResponse> logout(@RequestHeader("Authorization") String authHeader) {
        auth.logout(authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Logged out."));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDtos.UserResponse> me(@AuthenticationPrincipal User me) {
        return ResponseEntity.ok(AuthDtos.UserResponse.from(me));
    }

    @PutMapping("/password")
    public ResponseEntity<CommonDtos.OkResponse> changePassword(@AuthenticationPrincipal User me,
                                                               @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        auth.changePassword(me, request);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true,
                "Password updated. Please log in again with your new password."));
    }

    /** Local mode: the reset token is returned so the UI can display it (no mail server needed). */
    @PostMapping("/forgot-password")
    public ResponseEntity<CommonDtos.OkResponse> forgot(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        AuthService.ResetOutcome outcome = auth.forgotPassword(request.email());
        if (outcome == null) {
            return ResponseEntity.ok(new CommonDtos.OkResponse(true,
                    "If an account exists for that email, a reset link has been created."));
        }
        return ResponseEntity.ok(new CommonDtos.OkResponse(true,
                "Reset requested for " + outcome.fullName() + ". Local reset token: " + outcome.resetToken()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<CommonDtos.OkResponse> reset(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        auth.resetPassword(request);
        return ResponseEntity.ok(new CommonDtos.OkResponse(true, "Password reset. You can log in now."));
    }
}
