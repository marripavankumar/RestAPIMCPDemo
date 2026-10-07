package com.restmcp.demo.employee.dto;

import com.restmcp.demo.employee.EmployeeStatus;

/** All fields optional; null fields are ignored. */
public record EmployeeSearchCriteria(String name, Long departmentId, EmployeeStatus status) {
}
