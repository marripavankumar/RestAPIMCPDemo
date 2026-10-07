package com.restmcp.demo.mcp;

import com.restmcp.demo.department.DepartmentService;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Read-only MCP resources that clients can attach as context. */
@Component
public class OrgResources {

    private final DepartmentService departmentService;
    private final JsonMapper jsonMapper;

    public OrgResources(DepartmentService departmentService, JsonMapper jsonMapper) {
        this.departmentService = departmentService;
        this.jsonMapper = jsonMapper;
    }

    @McpResource(uri = "org://departments", name = "department-directory", title = "Department directory",
            description = "All departments with code, location, manager and headcount",
            mimeType = "application/json")
    public String departmentDirectory() {
        return jsonMapper.writeValueAsString(departmentService.findAll());
    }

    @McpResource(uri = "org://departments/{code}/roster", name = "department-roster", title = "Department roster",
            description = "Employees of the department with the given code, e.g. org://departments/ENG/roster",
            mimeType = "application/json")
    public String departmentRoster(String code) {
        return jsonMapper.writeValueAsString(departmentService.rosterByCode(code));
    }
}
