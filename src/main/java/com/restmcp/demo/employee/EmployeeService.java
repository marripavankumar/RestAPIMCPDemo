package com.restmcp.demo.employee;

import com.restmcp.demo.common.api.PageResponse;
import com.restmcp.demo.common.exception.ConflictException;
import com.restmcp.demo.common.exception.NotFoundException;
import com.restmcp.demo.department.Department;
import com.restmcp.demo.department.DepartmentRepository;
import com.restmcp.demo.employee.dto.EmployeeRequest;
import com.restmcp.demo.employee.dto.EmployeeResponse;
import com.restmcp.demo.employee.dto.EmployeeSearchCriteria;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class EmployeeService {

    static final int MAX_PAGE_SIZE = 100;

    private final EmployeeRepository employees;
    private final DepartmentRepository departments;

    public EmployeeService(EmployeeRepository employees, DepartmentRepository departments) {
        this.employees = employees;
        this.departments = departments;
    }

    public PageResponse<EmployeeResponse> search(EmployeeSearchCriteria criteria, Pageable pageable) {
        Pageable capped = pageable.getPageSize() > MAX_PAGE_SIZE
                ? PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort())
                : pageable;
        var spec = EmployeeSpecifications.matching(criteria.name(), criteria.departmentId(), criteria.status());
        return PageResponse.from(employees.findAll(spec, capped).map(EmployeeResponse::from));
    }

    public EmployeeResponse findById(Long id) {
        return EmployeeResponse.from(load(id));
    }

    @Transactional
    public EmployeeResponse create(@Valid EmployeeRequest request) {
        if (employees.existsByEmailIgnoreCase(request.email())) {
            throw duplicateEmail(request.email());
        }
        var employee = new Employee(request.firstName(), request.lastName(), request.email(),
                request.jobTitle(), request.salary(), request.hireDate());
        if (request.status() != null) {
            employee.setStatus(request.status());
        }
        if (request.departmentId() != null) {
            employee.setDepartment(loadDepartment(request.departmentId()));
        }
        return EmployeeResponse.from(employees.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, @Valid EmployeeRequest request) {
        var employee = load(id);
        if (!employee.getEmail().equalsIgnoreCase(request.email())
                && employees.existsByEmailIgnoreCase(request.email())) {
            throw duplicateEmail(request.email());
        }
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setJobTitle(request.jobTitle());
        employee.setSalary(request.salary());
        employee.setHireDate(request.hireDate());
        if (request.status() != null) {
            employee.setStatus(request.status());
        }
        if (request.departmentId() != null) {
            employee.setDepartment(loadDepartment(request.departmentId()));
        }
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public void delete(Long id) {
        var employee = load(id);
        departments.findByManagerId(id).forEach(d -> d.setManager(null));
        employees.delete(employee);
    }

    @Transactional
    public EmployeeResponse transfer(Long employeeId, Long targetDepartmentId) {
        var employee = load(employeeId);
        employee.transferTo(loadDepartment(targetDepartmentId));
        return EmployeeResponse.from(employee);
    }

    private Employee load(Long id) {
        return employees.findById(id).orElseThrow(() -> new NotFoundException(
                "Employee %d not found. Use search_employees to find valid employee ids.".formatted(id)));
    }

    private Department loadDepartment(Long id) {
        return departments.findById(id).orElseThrow(() -> new NotFoundException(
                "Department %d not found. Use list_departments to find valid department ids.".formatted(id)));
    }

    private static ConflictException duplicateEmail(String email) {
        return new ConflictException("An employee with email '%s' already exists.".formatted(email));
    }
}
