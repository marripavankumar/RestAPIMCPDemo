package com.restmcp.demo.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.restmcp.demo.department.Department;
import com.restmcp.demo.employee.Employee;
import com.restmcp.demo.employee.EmployeeStatus;

public record EmployeeResponse(Long id, String firstName, String lastName, String fullName, String email,
                               String jobTitle, BigDecimal salary, LocalDate hireDate, EmployeeStatus status,
                               DepartmentRef department) {

    public record DepartmentRef(Long id, String code, String name) {
    }

    public static EmployeeResponse from(Employee e) {
        Department d = e.getDepartment();
        DepartmentRef ref = d == null ? null : new DepartmentRef(d.getId(), d.getCode(), d.getName());
        return new EmployeeResponse(e.getId(), e.getFirstName(), e.getLastName(), e.fullName(), e.getEmail(),
                e.getJobTitle(), e.getSalary(), e.getHireDate(), e.getStatus(), ref);
    }
}
