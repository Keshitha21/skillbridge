package com.skillbridge.backend.security;

import com.skillbridge.backend.entity.AuthToken;
import com.skillbridge.backend.entity.PasswordReset;
import com.skillbridge.backend.entity.User;
import com.skillbridge.backend.repository.AuthTokenRepository;
import com.skillbridge.backend.repository.PasswordResetRepository;
import com.skillbridge.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Simple auth token management (F1).
 * A login mints an opaque random token; only its SHA-256 hash is stored in MySQL.
 * Frontend sends it in the Authorization header: "Bearer <token>".
 */
@Service
public class TokenService {

    public static final long TOKEN_DAYS_VALID = 30;

    private final SecureRandom random = new SecureRandom();
    private final AuthTokenRepository tokens;
    private final UserRepository users;
    private final PasswordResetRepository resets;

    public TokenService(AuthTokenRepository tokens, UserRepository users, PasswordResetRepository resets) {
        this.tokens = tokens;
        this.users = users;
        this.resets = resets;
    }

    public static final long RESET_MINUTES_VALID = 60;

    /** F1 password recovery: mint a one-hour reset token, returning only the hash to storage. */
    @Transactional
    public String issueResetToken(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        PasswordReset entity = PasswordReset.builder()
                .user(user)
                .tokenHash(sha256(token))
                .expiresAt(Instant.now().plus(RESET_MINUTES_VALID, ChronoUnit.MINUTES))
                .build();
        resets.save(entity);
        return token;
    }

    @Transactional
    public void revokeAllFor(Long userId) {
        tokens.deleteByUserId(userId);
    }

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes); // 64 hex characters
        AuthToken entity = AuthToken.builder()
                .user(user)
                .tokenHash(sha256(token))
                .expiresAt(Instant.now().plus(TOKEN_DAYS_VALID, ChronoUnit.DAYS))
                .build();
        tokens.save(entity);
        tokens.deleteByExpiresAtBefore(Instant.now()); // lazy cleanup
        return token;
    }

    @Transactional
    public Optional<User> verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return tokens.findByTokenHash(sha256(rawToken))
                .flatMap(entity -> {
                    if (entity.getExpiresAt().isBefore(Instant.now())) {
                        tokens.delete(entity);
                        return Optional.empty();
                    }
                    User user = entity.getUser();
                    if (user.getStatus() != User.Status.ACTIVE) {
                        return Optional.empty();
                    }
                    return Optional.of(user);
                });
    }

    @Transactional
    public void revoke(String rawToken) {
        tokens.findByTokenHash(sha256(rawToken)).ifPresent(tokens::delete);
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
