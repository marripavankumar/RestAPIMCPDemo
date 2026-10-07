package com.restmcp.demo.department;

import java.util.List;

import com.restmcp.demo.department.dto.DepartmentRequest;
import com.restmcp.demo.department.dto.DepartmentResponse;
import com.restmcp.demo.department.dto.DepartmentSummary;
import com.restmcp.demo.employee.dto.EmployeeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments", description = "Department CRUD, members, summary and manager assignment")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all departments with headcount")
    public List<DepartmentResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a department by id")
    public DepartmentResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create a department")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        var created = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a department")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an empty department")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/employees")
    @Operation(summary = "List the employees of a department")
    public List<EmployeeResponse> employees(@PathVariable Long id) {
        return service.employeesOf(id);
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Headcount, payroll and manager of a department")
    public DepartmentSummary summary(@PathVariable Long id) {
        return service.summary(id);
    }

    @PutMapping("/{id}/manager/{employeeId}")
    @Operation(summary = "Assign a department manager (must be a member)")
    public DepartmentResponse assignManager(@PathVariable Long id, @PathVariable Long employeeId) {
        return service.assignManager(id, employeeId);
    }
}
