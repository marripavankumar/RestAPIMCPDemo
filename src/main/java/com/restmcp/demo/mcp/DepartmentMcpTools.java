package com.restmcp.demo.mcp;

import java.util.List;

import com.restmcp.demo.department.DepartmentService;
import com.restmcp.demo.department.dto.DepartmentRequest;
import com.restmcp.demo.department.dto.DepartmentResponse;
import com.restmcp.demo.department.dto.DepartmentSummary;
import com.restmcp.demo.employee.dto.EmployeeResponse;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/** MCP tool adapter over {@link DepartmentService}. No business logic lives here. */
@Component
public class DepartmentMcpTools {

    private final DepartmentService departmentService;

    public DepartmentMcpTools(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @McpTool(name = "list_departments",
            description = "List every department with its id, code, name, location, manager and headcount. "
                    + "Use this to find a department id or code before calling other department tools.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public List<DepartmentResponse> listDepartments() {
        return departmentService.findAll();
    }

    @McpTool(name = "get_department",
            description = "Get one department by its numeric id, including manager and headcount.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public DepartmentResponse getDepartment(
            @McpToolParam(description = "Numeric department id (see list_departments)") Long departmentId) {
        return departmentService.findById(departmentId);
    }

    @McpTool(name = "get_department_employees",
            description = "List all employees who belong to a department. Returns full employee records.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public List<EmployeeResponse> getDepartmentEmployees(
            @McpToolParam(description = "Numeric department id (see list_departments)") Long departmentId) {
        return departmentService.employeesOf(departmentId);
    }

    @McpTool(name = "get_department_summary",
            description = "Summarise a department: headcount, total and average salary, and manager name. "
                    + "Use this for payroll or size questions instead of adding up employees yourself.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public DepartmentSummary getDepartmentSummary(
            @McpToolParam(description = "Numeric department id (see list_departments)") Long departmentId) {
        return departmentService.summary(departmentId);
    }

    @McpTool(name = "create_department",
            description = "Create a new department. The code is stored in upper case and must be unique, "
                    + "as must the name. Returns the created department with its new id.",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false,
                    openWorldHint = false))
    public DepartmentResponse createDepartment(
            @McpToolParam(description = "Short unique code, letters/digits/underscore, e.g. OPS") String code,
            @McpToolParam(description = "Unique display name, e.g. Operations") String name,
            @McpToolParam(description = "City or office location", required = false) String location) {
        return departmentService.create(new DepartmentRequest(code, name, location));
    }

    @McpTool(name = "update_department",
            description = "Change a department's code, name or location. Only the fields you pass are changed. "
                    + "Returns the updated department.",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public DepartmentResponse updateDepartment(
            @McpToolParam(description = "Numeric department id (see list_departments)") Long departmentId,
            @McpToolParam(description = "New unique code", required = false) String code,
            @McpToolParam(description = "New unique name", required = false) String name,
            @McpToolParam(description = "New location", required = false) String location) {
        var current = departmentService.findById(departmentId);
        return departmentService.update(departmentId, new DepartmentRequest(
                code != null ? code : current.code(),
                name != null ? name : current.name(),
                location != null ? location : current.location()));
    }

    @McpTool(name = "delete_department",
            description = "Permanently delete a department. Only works when the department has no employees; "
                    + "transfer them first with transfer_employee. Confirm with the user before calling.",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true,
                    openWorldHint = false))
    public String deleteDepartment(
            @McpToolParam(description = "Numeric department id (see list_departments)") Long departmentId) {
        departmentService.delete(departmentId);
        return "Department %d deleted.".formatted(departmentId);
    }

    @McpTool(name = "assign_department_manager",
            description = "Make an employee the manager of a department. The employee must already belong "
                    + "to that department. Returns the updated department.",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public DepartmentResponse assignDepartmentManager(
            @McpToolParam(description = "Numeric department id (see list_departments)") Long departmentId,
            @McpToolParam(description = "Numeric id of the employee who becomes manager") Long employeeId) {
        return departmentService.assignManager(departmentId, employeeId);
    }
}
