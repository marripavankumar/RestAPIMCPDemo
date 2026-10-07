package com.restmcp.demo.department;

import java.math.BigDecimal;

public record DepartmentStats(Long headcount, BigDecimal totalSalary) {

    public DepartmentStats {
        headcount = headcount == null ? 0L : headcount;
        totalSalary = totalSalary == null ? BigDecimal.ZERO : totalSalary;
    }
}
