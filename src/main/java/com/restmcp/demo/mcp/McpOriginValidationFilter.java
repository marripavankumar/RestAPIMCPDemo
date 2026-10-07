package com.restmcp.demo.mcp;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects browser requests to the MCP endpoint from untrusted origins. The MCP specification requires
 * HTTP servers to validate {@code Origin} to prevent DNS-rebinding attacks. Requests without an Origin
 * header (non-browser clients) are allowed. Ports are ignored when matching.
 */
public class McpOriginValidationFilter extends OncePerRequestFilter {

    private final List<String> allowedHosts;

    public McpOriginValidationFilter(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts.stream().map(h -> h.toLowerCase(Locale.ROOT)).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || isAllowed(origin)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        response.getWriter().write("""
                {"title":"Forbidden","status":403,"detail":"Origin not allowed for MCP requests."}""");
    }

    private boolean isAllowed(String origin) {
        try {
            String host = URI.create(origin).getHost();
            return host != null && allowedHosts.contains(host.toLowerCase(Locale.ROOT));
        }
        catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
