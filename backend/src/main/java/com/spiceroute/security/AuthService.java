package com.spiceroute.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    public record Actor(UUID id, String role, String name) {}
    public record Credentials(String email, String password, String name) {}
    public record Session(String token, String role, String name) {}

    private final UserRepository users;
    private final SessionRepository sessions;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, SessionRepository sessions,
                       BCryptPasswordEncoder passwordEncoder) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Transactional
    public UUID createUser(Credentials credentials, String role) {
        validateCredentials(credentials);
        String email = normalizeEmail(credentials.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email exists");
        }
        UserAccount user = new UserAccount(
            UUID.randomUUID(), credentials.name().trim(), email,
            passwordEncoder.encode(credentials.password()), role
        );
        return users.saveAndFlush(user).getId();
    }

    private void validateCredentials(Credentials credentials) {
        if (credentials == null || credentials.name() == null || credentials.name().isBlank()
                || credentials.name().length() > 120 || credentials.email() == null
                || credentials.email().length() > 254
                || !normalizeEmail(credentials.email()).matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
                || !validPassword(credentials.password())) {
            throw new IllegalArgumentException("Enter a valid name, email and password (8–72 bytes)");
        }
    }

    private boolean validPassword(String password) {
        return password != null && password.length() >= 8
            && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public Session register(Credentials credentials) {
        createUser(credentials, "OWNER");
        return login(credentials);
    }

    @Transactional
    public Session login(Credentials credentials) {
        if (credentials == null || credentials.email() == null || !validPassword(credentials.password())) {
            throw invalidCredentials();
        }
        UserAccount user = users.findByEmailAndActiveTrue(normalizeEmail(credentials.email()))
            .orElseThrow(this::invalidCredentials);
        if (user.isOwnerSuspended() || !passwordEncoder.matches(credentials.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.save(new UserSession(hash(token), user, Instant.now().plus(12, ChronoUnit.HOURS)));
        return new Session(token, user.getRole(), user.getName());
    }

    @Transactional(readOnly = true)
    public Actor require(String header) {
        if (header == null || !header.startsWith("Bearer ") || header.length() > 200) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue");
        }
        UserAccount user = sessions.findActiveUser(hash(header.substring(7)), Instant.now())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired"));
        return new Actor(user.getId(), user.getRole(), user.getName());
    }

    @Transactional
    public void logout(String header) {
        require(header);
        sessions.deleteById(hash(header.substring(7)));
    }

    @Transactional
    public void revokeSessions(UUID userId) { sessions.revokeAll(userId); }

    @Transactional
    public void updateProfile(Actor actor, String name) {
        if (name == null || name.isBlank() || name.strip().length() > 120) {
            throw new IllegalArgumentException("Enter a name up to 120 characters");
        }
        users.findById(actor.id()).orElseThrow(this::invalidCredentials).rename(name.strip());
    }

    @Transactional
    public void changePassword(Actor actor, String current, String replacement) {
        UserAccount user = users.findById(actor.id()).orElseThrow(this::invalidCredentials);
        if (!validPassword(current) || !passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (!validPassword(replacement)) throw new IllegalArgumentException("Use a password of 8–72 bytes");
        user.setPasswordHash(passwordEncoder.encode(replacement));
        sessions.revokeAll(user.getId());
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
