package com.restmcp.demo.mcp;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

/** Reusable prompt templates that users can pick in MCP hosts (often shown as slash commands). */
@Component
public class OrgPrompts {

    @McpPrompt(name = "department_report", title = "Department report",
            description = "Write a short report on one department: size, payroll, manager and observations")
    public GetPromptResult departmentReport(
            @McpArg(name = "departmentCode", description = "Department code, e.g. ENG", required = true)
            String departmentCode) {
        return userPrompt("Department report for " + departmentCode, """
                Write a short report on department %s.
                1. Call list_departments to find its id.
                2. Call get_department_summary and get_department_employees with that id.
                3. Report headcount, total and average salary, the manager (or say the role is vacant),
                   a breakdown by job title and status, and any notable observations.
                """.formatted(departmentCode));
    }

    @McpPrompt(name = "onboard_employee", title = "Onboard employee",
            description = "Guide the creation of a new hire and place them in a department")
    public GetPromptResult onboardEmployee(
            @McpArg(name = "firstName", description = "New hire's first name", required = true) String firstName,
            @McpArg(name = "lastName", description = "New hire's last name", required = true) String lastName,
            @McpArg(name = "departmentCode", description = "Department code, e.g. ENG", required = true)
            String departmentCode) {
        return userPrompt("Onboard " + firstName + " " + lastName, """
                Onboard a new employee named %s %s into department %s.
                1. Call list_departments to resolve the department id for code %s.
                2. Ask me for anything still missing: work email, job title, annual salary and hire date.
                3. Show me the details and wait for my confirmation, then call create_employee.
                4. Finish with a one-line confirmation including the new employee id.
                """.formatted(firstName, lastName, departmentCode, departmentCode));
    }

    private static GetPromptResult userPrompt(String description, String text) {
        return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, new TextContent(text))));
    }
}
