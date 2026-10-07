package com.restmcp.demo.mcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Launches the packaged JAR exactly as a desktop MCP host would (a child process speaking JSON-RPC
 * over stdin/stdout). Runs in the Failsafe integration-test phase, after the JAR is built.
 */
class StdioTransportIT {

    @Test
    void packagedJarServesToolsOverStdio() {
        Path jar = Path.of(System.getProperty("app.jar", "target/rest-api-mcp-demo-0.0.1-SNAPSHOT.jar"));
        assertThat(jar).as("run 'mvnw verify' so the JAR exists").satisfies(p -> assertThat(Files.exists(p)).isTrue());
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();

        var params = ServerParameters.builder(java)
                .args("-jar", jar.toAbsolutePath().toString(), "--spring.profiles.active=stdio")
                .build();
        var client = McpClient.sync(new StdioClientTransport(params, McpJsonDefaults.getMapper()))
                .initializationTimeout(Duration.ofSeconds(60))
                .requestTimeout(Duration.ofSeconds(20))
                .build();
        try {
            client.initialize();

            assertThat(client.getServerInfo().name()).isEqualTo("employee-department-mcp");
            assertThat(client.listTools().tools()).hasSize(14);

            var result = client.callTool(new McpSchema.CallToolRequest("list_departments", Map.of()));
            assertThat(((McpSchema.TextContent) result.content().getFirst()).text()).contains("Engineering");
        }
        finally {
            client.closeGracefully();
        }
    }
}
