package com.restmcp.demo.config;

import java.util.List;

import com.restmcp.demo.mcp.McpOriginValidationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * HTTP protection for the REST API and the MCP endpoint. Not active in the STDIO profile, where there is no
 * web server and the host process owns the connection.
 */
@Configuration
@ConditionalOnWebApplication
public class WebSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

    private static final String[] PROTECTED_PATHS = {"/api/*", "/mcp", "/mcp/*", "/sse"};

    @Bean
    FilterRegistrationBean<McpOriginValidationFilter> mcpOriginValidationFilter(
            @Value("${app.mcp.allowed-origin-hosts:localhost,127.0.0.1,[::1]}") List<String> allowedHosts) {
        var registration = new FilterRegistrationBean<>(new McpOriginValidationFilter(allowedHosts));
        registration.addUrlPatterns("/mcp", "/mcp/*", "/sse");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter(@Value("${app.security.api-key:}") String apiKey) {
        var registration = new FilterRegistrationBean<>(new ApiKeyAuthFilter(apiKey));
        registration.addUrlPatterns(PROTECTED_PATHS);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        if (!StringUtils.hasText(apiKey)) {
            registration.setEnabled(false);
            log.warn("No API key configured (APP_API_KEY): /api and /mcp are UNAUTHENTICATED. "
                    + "Fine for local demos only; the server binds to 127.0.0.1 by default.");
        }
        return registration;
    }
}
