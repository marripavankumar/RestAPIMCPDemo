package com.restmcp.demo.mcp;

import java.util.List;

import com.restmcp.demo.department.DepartmentService;
import com.restmcp.demo.department.dto.DepartmentResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/** MCP tool adapter over {@link DepartmentService}. No business logic lives here. */
@Component
public class DepartmentMcpTools {

    private final DepartmentService departmentService;

    public DepartmentMcpTools(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Tool(name = "list_departments",
            description = "List every department with its id, code, name, location, manager and headcount. "
                    + "Use this to find a department id or code before calling other department tools.")
    public List<DepartmentResponse> listDepartments() {
        return departmentService.findAll();
    }
}
