package com.restmcp.demo.mcp;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.restmcp.demo.common.api.PageResponse;
import com.restmcp.demo.employee.EmployeeService;
import com.restmcp.demo.employee.EmployeeStatus;
import com.restmcp.demo.employee.dto.EmployeeRequest;
import com.restmcp.demo.employee.dto.EmployeeResponse;
import com.restmcp.demo.employee.dto.EmployeeSearchCriteria;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** MCP tool adapter over {@link EmployeeService}. No business logic lives here. */
@Component
public class EmployeeMcpTools {

    private final EmployeeService employeeService;

    public EmployeeMcpTools(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @McpTool(name = "search_employees",
            description = "Find employees by part of their name, department and/or status. All filters are "
                    + "optional; with none you get everyone. Results are paged and sorted by last name.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public PageResponse<EmployeeResponse> searchEmployees(
            @McpToolParam(description = "Part of the first or last name, case-insensitive", required = false)
            String name,
            @McpToolParam(description = "Only employees of this department id", required = false)
            Long departmentId,
            @McpToolParam(description = "Only employees with this status", required = false)
            EmployeeStatus status,
            @McpToolParam(description = "Zero-based page number, default 0", required = false) Integer page,
            @McpToolParam(description = "Page size 1-100, default 20", required = false) Integer size) {
        var pageable = PageRequest.of(page == null ? 0 : Math.max(page, 0),
                size == null ? 20 : Math.clamp(size, 1, 100), Sort.by("lastName", "firstName"));
        return employeeService.search(new EmployeeSearchCriteria(name, departmentId, status), pageable);
    }

    @McpTool(name = "get_employee",
            description = "Get one employee by numeric id, including job, salary, status and department.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public EmployeeResponse getEmployee(
            @McpToolParam(description = "Numeric employee id (see search_employees)") Long employeeId) {
        return employeeService.findById(employeeId);
    }

    @McpTool(name = "create_employee",
            description = "Hire a new employee. The email must be unique. Status starts as ACTIVE. "
                    + "Optionally place them in a department (use list_departments for the id). "
                    + "Returns the created employee with its new id.",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false,
                    openWorldHint = false))
    public EmployeeResponse createEmployee(
            @McpToolParam(description = "First name") String firstName,
            @McpToolParam(description = "Last name") String lastName,
            @McpToolParam(description = "Unique work email address") String email,
            @McpToolParam(description = "Job title, e.g. Software Engineer") String jobTitle,
            @McpToolParam(description = "Annual salary, greater than 0") BigDecimal salary,
            @McpToolParam(description = "Hire date, ISO format yyyy-MM-dd, not in the future") LocalDate hireDate,
            @McpToolParam(description = "Department id to place the employee in", required = false)
            Long departmentId) {
        return employeeService.create(new EmployeeRequest(firstName, lastName, email, jobTitle, salary, hireDate,
                null, departmentId));
    }

    @McpTool(name = "update_employee",
            description = "Change an employee's details. Only the fields you pass are changed. To move someone "
                    + "to another department prefer transfer_employee. Returns the updated employee.",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true,
                    openWorldHint = false))
    public EmployeeResponse updateEmployee(
            @McpToolParam(description = "Numeric employee id (see search_employees)") Long employeeId,
            @McpToolParam(description = "New first name", required = false) String firstName,
            @McpToolParam(description = "New last name", required = false) String lastName,
            @McpToolParam(description = "New unique email", required = false) String email,
            @McpToolParam(description = "New job title", required = false) String jobTitle,
            @McpToolParam(description = "New annual salary, greater than 0", required = false) BigDecimal salary,
            @McpToolParam(description = "New hire date, yyyy-MM-dd", required = false) LocalDate hireDate,
            @McpToolParam(description = "New status", required = false) EmployeeStatus status) {
        var current = employeeService.findById(employeeId);
        return employeeService.update(employeeId, new EmployeeRequest(
                firstName != null ? firstName : current.firstName(),
                lastName != null ? lastName : current.lastName(),
                email != null ? email : current.email(),
                jobTitle != null ? jobTitle : current.jobTitle(),
                salary != null ? salary : current.salary(),
                hireDate != null ? hireDate : current.hireDate(),
                status,
                null));
    }

    @McpTool(name = "delete_employee",
            description = "Permanently delete an employee record. If they manage a department, that department "
                    + "is left without a manager. Confirm with the user before calling.",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = true,
                    openWorldHint = false))
    public String deleteEmployee(
            @McpToolParam(description = "Numeric employee id (see search_employees)") Long employeeId) {
        employeeService.delete(employeeId);
        return "Employee %d deleted.".formatted(employeeId);
    }

    @McpTool(name = "transfer_employee",
            description = "Move an employee to a different department. Find the employee id with "
                    + "search_employees and the department id with list_departments first. TERMINATED employees "
                    + "cannot be moved. Returns the updated employee.",
            annotations = @McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = false,
                    openWorldHint = false))
    public EmployeeResponse transferEmployee(
            @McpToolParam(description = "Numeric id of the employee to move") Long employeeId,
            @McpToolParam(description = "Numeric id of the destination department") Long targetDepartmentId) {
        return employeeService.transfer(employeeId, targetDepartmentId);
    }
}
