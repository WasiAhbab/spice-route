package com.spiceroute.security;

import jakarta.servlet.http.HttpServletRequest;
import com.spiceroute.common.RateLimiter;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final boolean secureCookies;
    private final RateLimiter rateLimiter;

    public AuthController(AuthService auth, RateLimiter rateLimiter, @Value("${app.secure-cookies}") boolean secureCookies) {
        this.auth = auth;
        this.rateLimiter = rateLimiter;
        this.secureCookies = secureCookies;
    }

    @PostMapping("/register")
    ResponseEntity<?> register(@RequestBody AuthService.Credentials credentials,
                               @RequestHeader(value = "X-Session-Mode", required = false) String mode, HttpServletRequest request) {
        rateLimiter.enforce("register-network", request.getRemoteAddr(), 30, Duration.ofHours(1));
        return respond(auth.register(credentials), mode);
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@RequestBody AuthService.Credentials credentials,
                            @RequestHeader(value = "X-Session-Mode", required = false) String mode, HttpServletRequest request) {
        rateLimiter.enforce("login-network", request.getRemoteAddr(), 100, Duration.ofMinutes(15));
        if (credentials.email() != null) {
            rateLimiter.enforce("login-account", credentials.email().trim().toLowerCase(java.util.Locale.ROOT), 10, Duration.ofMinutes(15));
        }
        return respond(auth.login(credentials), mode);
    }

    @GetMapping("/me")
    Object me(@AuthenticationPrincipal AuthService.Actor actor) {
        return Map.of("name", actor.name(), "role", actor.role());
    }

    @PostMapping("/logout")
    ResponseEntity<?> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) header = "Bearer " + SessionAuthenticationFilter.cookieToken(request);
        auth.logout(header);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
            .body(Map.of("ok", true));
    }

    public record ProfileUpdate(String name) {}
    public record PasswordUpdate(String currentPassword, String newPassword) {}

    @org.springframework.web.bind.annotation.PatchMapping("/profile")
    Object profile(@AuthenticationPrincipal AuthService.Actor actor, @RequestBody ProfileUpdate entry) {
        auth.updateProfile(actor, entry.name());
        return Map.of("name", entry.name().strip(), "role", actor.role());
    }

    @PostMapping("/password")
    ResponseEntity<?> password(@AuthenticationPrincipal AuthService.Actor actor,
                               @RequestBody PasswordUpdate entry) {
        rateLimiter.enforce("password-user", actor.id().toString(), 5, Duration.ofMinutes(15));
        auth.changePassword(actor, entry.currentPassword(), entry.newPassword());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
            .body(Map.of("ok", true));
    }

    private ResponseEntity<?> respond(AuthService.Session session, String mode) {
        if ("bearer".equals(mode)) return ResponseEntity.ok(session);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie(session.token(), Duration.ofHours(12)).toString())
            .body(Map.of("name", session.name(), "role", session.role()));
    }

    private ResponseCookie cookie(String token, Duration age) {
        return ResponseCookie.from(SessionAuthenticationFilter.COOKIE_NAME, token)
            .httpOnly(true).secure(secureCookies).sameSite("Lax").path("/").maxAge(age).build();
    }
}
