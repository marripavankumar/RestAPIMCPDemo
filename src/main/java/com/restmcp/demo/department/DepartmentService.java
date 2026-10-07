package com.restmcp.demo.department;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.restmcp.demo.common.exception.BusinessRuleException;
import com.restmcp.demo.common.exception.ConflictException;
import com.restmcp.demo.common.exception.NotFoundException;
import com.restmcp.demo.department.dto.DepartmentRequest;
import com.restmcp.demo.department.dto.DepartmentResponse;
import com.restmcp.demo.department.dto.DepartmentSummary;
import com.restmcp.demo.employee.Employee;
import com.restmcp.demo.employee.EmployeeRepository;
import com.restmcp.demo.employee.dto.EmployeeResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departments;
    private final EmployeeRepository employees;

    public DepartmentService(DepartmentRepository departments, EmployeeRepository employees) {
        this.departments = departments;
        this.employees = employees;
    }

    public List<DepartmentResponse> findAll() {
        Map<Long, Long> headcounts = departments.headcounts().stream()
                .collect(Collectors.toMap(DepartmentHeadcount::departmentId, DepartmentHeadcount::headcount));
        return departments.findAll(Sort.by("code")).stream()
                .map(d -> DepartmentResponse.from(d, headcounts.getOrDefault(d.getId(), 0L)))
                .toList();
    }

    public DepartmentResponse findById(Long id) {
        var department = load(id);
        return DepartmentResponse.from(department, employees.countByDepartmentId(id));
    }

    @Transactional
    public DepartmentResponse create(@Valid DepartmentRequest request) {
        String code = normalizeCode(request.code());
        if (departments.existsByCodeIgnoreCase(code)) {
            throw duplicateCode(code);
        }
        if (departments.existsByNameIgnoreCase(request.name())) {
            throw duplicateName(request.name());
        }
        var saved = departments.save(new Department(code, request.name(), request.location()));
        return DepartmentResponse.from(saved, 0);
    }

    @Transactional
    public DepartmentResponse update(Long id, @Valid DepartmentRequest request) {
        var department = load(id);
        String code = normalizeCode(request.code());
        if (!department.getCode().equalsIgnoreCase(code) && departments.existsByCodeIgnoreCase(code)) {
            throw duplicateCode(code);
        }
        if (!department.getName().equalsIgnoreCase(request.name())
                && departments.existsByNameIgnoreCase(request.name())) {
            throw duplicateName(request.name());
        }
        department.setCode(code);
        department.setName(request.name());
        department.setLocation(request.location());
        return DepartmentResponse.from(department, employees.countByDepartmentId(id));
    }

    @Transactional
    public void delete(Long id) {
        var department = load(id);
        long headcount = employees.countByDepartmentId(id);
        if (headcount > 0) {
            throw new ConflictException("Department %s still has %d employees. Transfer them before deleting."
                    .formatted(department.getCode(), headcount));
        }
        departments.delete(department);
    }

    public List<EmployeeResponse> employeesOf(Long id) {
        if (!departments.existsById(id)) {
            throw notFound(id);
        }
        return employees.findByDepartmentId(id).stream().map(EmployeeResponse::from).toList();
    }

    public List<EmployeeResponse> rosterByCode(String code) {
        var department = departments.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException(
                "Department with code '%s' not found. Use list_departments to see valid codes.".formatted(code)));
        return employees.findByDepartmentId(department.getId()).stream().map(EmployeeResponse::from).toList();
    }

    public DepartmentSummary summary(Long id) {
        var department = load(id);
        var stats = departments.statsFor(id);
        BigDecimal average = stats.headcount() == 0
                ? BigDecimal.ZERO
                : stats.totalSalary().divide(BigDecimal.valueOf(stats.headcount()), 2, RoundingMode.HALF_UP);
        Employee manager = department.getManager();
        return new DepartmentSummary(department.getId(), department.getCode(), department.getName(),
                stats.headcount(), stats.totalSalary(), average, manager == null ? null : manager.fullName());
    }

    @Transactional
    public DepartmentResponse assignManager(Long departmentId, Long employeeId) {
        var department = load(departmentId);
        var employee = employees.findById(employeeId).orElseThrow(() -> new NotFoundException(
                "Employee %d not found. Use search_employees to find valid employee ids.".formatted(employeeId)));
        if (employee.getDepartment() == null || !employee.getDepartment().getId().equals(departmentId)) {
            throw new BusinessRuleException(
                    "Employee %d is not a member of department %s. Transfer them to it first."
                            .formatted(employeeId, department.getCode()));
        }
        department.setManager(employee);
        return DepartmentResponse.from(department, employees.countByDepartmentId(departmentId));
    }

    private Department load(Long id) {
        return departments.findById(id).orElseThrow(() -> notFound(id));
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException(
                "Department %d not found. Use list_departments to find valid department ids.".formatted(id));
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static ConflictException duplicateCode(String code) {
        return new ConflictException("A department with code '%s' already exists.".formatted(code));
    }

    private static ConflictException duplicateName(String name) {
        return new ConflictException("A department named '%s' already exists.".formatted(name));
    }
}
