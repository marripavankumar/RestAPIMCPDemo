package com.restmcp.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI employeeDepartmentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Employee & Department API")
                .version("v1")
                .description("REST API for employees and departments. The same operations are exposed "
                        + "as MCP tools at /mcp."));
    }
}
