package com.spiceroute.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final AuthService auth;
    private final UserRepository users;
    private final String email;
    private final String password;

    public AdminBootstrap(AuthService auth, UserRepository users,
                          @Value("${app.admin-email}") String email,
                          @Value("${app.admin-password}") String password) {
        this.auth = auth;
        this.users = users;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Both admin environment variables must be set");
        }
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (!users.existsByEmail(normalizedEmail)) {
            auth.createUser(new AuthService.Credentials(normalizedEmail, password, "Platform admin"), "ADMIN");
        }
    }
}
