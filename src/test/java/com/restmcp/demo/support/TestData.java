package com.restmcp.demo.support;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.restmcp.demo.department.Department;
import com.restmcp.demo.employee.Employee;
import com.restmcp.demo.employee.EmployeeStatus;
import com.restmcp.demo.employee.dto.EmployeeRequest;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestData {

    private TestData() {
    }

    public static Department department(long id, String code, String name) {
        var d = new Department(code, name, "Bengaluru");
        ReflectionTestUtils.setField(d, "id", id);
        return d;
    }

    public static Employee employee(long id, String first, String last, Department department) {
        var e = new Employee(first, last, (first + "." + last + "@example.com").toLowerCase(), "Engineer",
                new BigDecimal("100000.00"), LocalDate.of(2024, 1, 15));
        ReflectionTestUtils.setField(e, "id", id);
        e.setDepartment(department);
        return e;
    }

    public static EmployeeRequest request(String email, Long departmentId) {
        return request(email, departmentId, null);
    }

    public static EmployeeRequest request(String email, Long departmentId, EmployeeStatus status) {
        return new EmployeeRequest("Asha", "Rao", email, "Engineer", new BigDecimal("95000.00"),
                LocalDate.of(2025, 9, 1), status, departmentId);
    }
}
