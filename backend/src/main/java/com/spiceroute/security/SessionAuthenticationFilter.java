package com.spiceroute.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

public class SessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "spice_session";
    private final AuthService auth;
    private final List<String> allowedOrigins;

    public SessionAuthenticationFilter(AuthService auth, List<String> allowedOrigins) {
        this.auth = auth;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        String cookieToken = cookieToken(request);
        boolean mutation = !List.of("GET", "HEAD", "OPTIONS").contains(request.getMethod());
        String origin = request.getHeader("Origin");
        if (mutation && ((origin != null && !allowedOrigins.contains(origin))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site"))
                || (cookieToken != null && authorization == null && origin == null))) {
            reject(response, 403, "Request origin is not allowed");
            return;
        }
        if (authorization == null && cookieToken != null) authorization = "Bearer " + cookieToken;
        // A stale session must not prevent signing in again or using a public feedback link.
        boolean publicRoute = request.getRequestURI().startsWith("/api/public/feedback/")
            || request.getRequestURI().equals("/api/auth/login")
            || request.getRequestURI().equals("/api/auth/register");
        if (authorization != null && !publicRoute) {
            try {
                AuthService.Actor actor = auth.require(authorization);
                var authentication = new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + actor.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ResponseStatusException exception) {
                reject(response, 401, "Session expired. Sign in again.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    public static String cookieToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
