package com.spiceroute.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    private List<String> allowedOrigins(String origins) {
        return Arrays.stream(origins.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, AuthService auth,
                              @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
                              @Value("${app.allowed-origins}") String origins) throws Exception {
        return http
            // Cookie mutations are protected by explicit origin checks in the session filter.
            .csrf(config -> config.disable())
            .cors(config -> config.configurationSource(cors))
            .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(config -> config.disable())
            .formLogin(config -> config.disable())
            .httpBasic(config -> config.disable())
            .logout(config -> config.disable())
            .exceptionHandling(config -> config
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Sign in to continue\"}");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Access denied\"}");
                }))
            .authorizeHttpRequests(rules -> rules
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/public/feedback/**", "/actuator/health", "/error").permitAll()
                .requestMatchers("/api/manage/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manage/**", "/api/analytics/**", "/api/qr/**").hasAnyRole("OWNER", "MANAGER")
                .requestMatchers("/api/auth/me", "/api/auth/logout", "/api/auth/profile", "/api/auth/password").authenticated()
                .anyRequest().denyAll())
            .addFilterBefore(new SessionAuthenticationFilter(auth, allowedOrigins(origins)), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.allowed-origins}") String origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins(origins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Session-Mode"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
