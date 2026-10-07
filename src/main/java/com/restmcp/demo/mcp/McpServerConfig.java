package com.restmcp.demo.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    /** Registers every {@code @Tool} method; the MCP auto-configuration publishes them via tools/list. */
    @Bean
    ToolCallbackProvider employeeDepartmentTools(DepartmentMcpTools departmentTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(departmentTools)
                .build();
    }
}
