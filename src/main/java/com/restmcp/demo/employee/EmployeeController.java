package com.restmcp.demo.employee;

import com.restmcp.demo.common.api.PageResponse;
import com.restmcp.demo.employee.dto.EmployeeRequest;
import com.restmcp.demo.employee.dto.EmployeeResponse;
import com.restmcp.demo.employee.dto.EmployeeSearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employees", description = "Employee CRUD, search and department transfer")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Search employees by name, department and status (paged)")
    public PageResponse<EmployeeResponse> search(@ModelAttribute EmployeeSearchCriteria criteria,
                                                 @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return service.search(criteria, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an employee by id")
    public EmployeeResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create an employee")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        var created = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an employee")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an employee")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/department/{departmentId}")
    @Operation(summary = "Transfer an employee to another department")
    public EmployeeResponse transfer(@PathVariable Long id, @PathVariable Long departmentId) {
        return service.transfer(id, departmentId);
    }
}
