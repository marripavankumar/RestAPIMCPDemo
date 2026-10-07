package com.restmcp.demo.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Requires a shared API key on REST and MCP endpoints, sent as {@code X-API-KEY: <key>} or
 * {@code Authorization: Bearer <key>}. Registered by {@link WebSecurityConfig} only when a key is configured.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-KEY";

    private final byte[] expectedKey;

    public ApiKeyAuthFilter(String apiKey) {
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String presented = presentedKey(request);
        if (presented != null && MessageDigest.isEqual(expectedKey, presented.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        response.getWriter().write("""
                {"title":"Unauthorized","status":401,"detail":"Missing or invalid API key. Send it in the \
                X-API-KEY header or as Authorization: Bearer <key>.","instance":"%s"}"""
                .formatted(request.getRequestURI()));
    }

    private static String presentedKey(HttpServletRequest request) {
        String key = request.getHeader(HEADER);
        if (key != null) {
            return key;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return null;
    }
}
