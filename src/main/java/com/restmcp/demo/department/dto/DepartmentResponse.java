package com.restmcp.demo.department.dto;

import com.restmcp.demo.department.Department;
import com.restmcp.demo.employee.Employee;

public record DepartmentResponse(Long id, String code, String name, String location,
                                 Long managerId, String managerName, long headcount) {

    public static DepartmentResponse from(Department d, long headcount) {
        Employee m = d.getManager();
        return new DepartmentResponse(d.getId(), d.getCode(), d.getName(), d.getLocation(),
                m == null ? null : m.getId(), m == null ? null : m.fullName(), headcount);
    }
}
