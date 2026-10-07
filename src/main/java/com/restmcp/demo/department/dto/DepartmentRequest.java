package com.restmcp.demo.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9_]+",
                message = "must contain only letters, digits or underscore") String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String location) {
}
