package com.restmcp.demo;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.security.api-key=test-key")
class SecurityIntegrationTest {

    static final String INITIALIZE = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
             "capabilities":{},"clientInfo":{"name":"test","version":"1"}}}
            """;

    @LocalServerPort
    int port;

    RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.builder().baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { })
                .build();
    }

    HttpStatus statusOf(RestClient.RequestHeadersSpec<?> request) {
        return HttpStatus.valueOf(request.retrieve().toBodilessEntity().getStatusCode().value());
    }

    @Test
    void restWithoutKeyIs401ProblemDetail() {
        var response = http.get().uri("/api/v1/departments").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue();
        assertThat(response.getBody()).contains("X-API-KEY");
    }

    @Test
    void restWithWrongKeyIs401() {
        assertThat(statusOf(http.get().uri("/api/v1/departments").header("X-API-KEY", "nope")))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void restWithKeyOrBearerIs200() {
        assertThat(statusOf(http.get().uri("/api/v1/departments").header("X-API-KEY", "test-key")))
                .isEqualTo(HttpStatus.OK);
        assertThat(statusOf(http.get().uri("/api/v1/departments").header("Authorization", "Bearer test-key")))
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void mcpWithoutKeyIs401() {
        assertThat(statusOf(http.post().uri("/mcp").contentType(MediaType.APPLICATION_JSON)
                .header("Accept", "application/json, text/event-stream").body(INITIALIZE)))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mcpFromForeignOriginIs403EvenWithKey() {
        assertThat(statusOf(http.post().uri("/mcp").contentType(MediaType.APPLICATION_JSON)
                .header("Accept", "application/json, text/event-stream")
                .header("X-API-KEY", "test-key")
                .header("Origin", "http://evil.example").body(INITIALIZE)))
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void mcpFromLocalhostOriginIsAllowed() {
        assertThat(statusOf(http.post().uri("/mcp").contentType(MediaType.APPLICATION_JSON)
                .header("Accept", "application/json, text/event-stream")
                .header("X-API-KEY", "test-key")
                .header("Origin", "http://localhost:6274").body(INITIALIZE)))
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void mcpClientWithKeyCanCallTools() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/mcp")
                .requestBuilder(HttpRequest.newBuilder().header("X-API-KEY", "test-key"))
                .build();
        var client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build();
        try {
            client.initialize();
            var result = client.callTool(new McpSchema.CallToolRequest("list_departments", Map.of()));
            assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        }
        finally {
            client.closeGracefully();
        }
    }

    @Test
    void healthAndDocsArePublic() {
        assertThat(statusOf(http.get().uri("/actuator/health"))).isEqualTo(HttpStatus.OK);
        assertThat(statusOf(http.get().uri("/v3/api-docs"))).isEqualTo(HttpStatus.OK);
    }
}
