package com.restmcp.demo.department.dto;

import java.math.BigDecimal;

public record DepartmentSummary(Long id, String code, String name, long headcount,
                                BigDecimal totalSalary, BigDecimal averageSalary, String managerName) {
}
