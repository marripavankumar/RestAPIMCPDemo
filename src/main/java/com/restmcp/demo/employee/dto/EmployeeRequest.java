package com.restmcp.demo.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.restmcp.demo.employee.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload. A null {@code status} means ACTIVE on create and "unchanged" on update;
 * a null {@code departmentId} means "no department" on create and "unchanged" on update.
 */
public record EmployeeRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 80) String jobTitle,
        @NotNull @Positive BigDecimal salary,
        @NotNull @PastOrPresent LocalDate hireDate,
        EmployeeStatus status,
        Long departmentId) {
}
