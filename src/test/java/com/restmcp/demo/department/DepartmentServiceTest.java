package com.restmcp.demo.department;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.restmcp.demo.common.exception.BusinessRuleException;
import com.restmcp.demo.common.exception.ConflictException;
import com.restmcp.demo.common.exception.NotFoundException;
import com.restmcp.demo.department.dto.DepartmentRequest;
import com.restmcp.demo.employee.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import static com.restmcp.demo.support.TestData.department;
import static com.restmcp.demo.support.TestData.employee;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    DepartmentRepository departments;

    @Mock
    EmployeeRepository employees;

    @InjectMocks
    DepartmentService service;

    @Test
    void create_duplicateCode_throwsConflict() {
        when(departments.existsByCodeIgnoreCase("ENG")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new DepartmentRequest("eng", "Engineering 2", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ENG");
        verify(departments, never()).save(any());
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(departments.existsByNameIgnoreCase("Finance")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new DepartmentRequest("FIN2", "Finance", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Finance");
    }

    @Test
    void create_valid_uppercasesCode() {
        when(departments.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(new DepartmentRequest("ops", "Operations", "Chennai"));

        assertThat(response.code()).isEqualTo("OPS");
        assertThat(response.headcount()).isZero();
    }

    @Test
    void update_changingCodeToExisting_throwsConflict() {
        when(departments.findById(9L)).thenReturn(Optional.of(department(9L, "OPS", "Operations")));
        when(departments.existsByCodeIgnoreCase("ENG")).thenReturn(true);

        assertThatThrownBy(() -> service.update(9L, new DepartmentRequest("ENG", "Operations", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_withEmployees_throwsConflict() {
        when(departments.findById(1L)).thenReturn(Optional.of(department(1L, "ENG", "Engineering")));
        when(employees.countByDepartmentId(1L)).thenReturn(5L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Department ENG still has 5 employees. Transfer them before deleting.");
    }

    @Test
    void delete_empty_deletes() {
        var ops = department(9L, "OPS", "Operations");
        when(departments.findById(9L)).thenReturn(Optional.of(ops));
        when(employees.countByDepartmentId(9L)).thenReturn(0L);

        service.delete(9L);

        verify(departments).delete(ops);
    }

    @Test
    void employeesOf_unknown_throwsNotFound() {
        when(departments.existsById(77L)).thenReturn(false);

        assertThatThrownBy(() -> service.employeesOf(77L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Department 77 not found");
    }

    @Test
    void summary_returnsStatsAndManager() {
        var fin = department(3L, "FIN", "Finance");
        fin.setManager(employee(7L, "Divya", "Menon", fin));
        when(departments.findById(3L)).thenReturn(Optional.of(fin));
        when(departments.statsFor(3L)).thenReturn(new DepartmentStats(3L, new BigDecimal("340000.00")));

        var summary = service.summary(3L);

        assertThat(summary.headcount()).isEqualTo(3);
        assertThat(summary.totalSalary()).isEqualByComparingTo("340000.00");
        assertThat(summary.averageSalary()).isEqualByComparingTo("113333.33");
        assertThat(summary.managerName()).isEqualTo("Divya Menon");
    }

    @Test
    void summary_emptyDepartment_hasZeroAverage() {
        when(departments.findById(9L)).thenReturn(Optional.of(department(9L, "OPS", "Operations")));
        when(departments.statsFor(9L)).thenReturn(new DepartmentStats(0L, null));

        assertThat(service.summary(9L).averageSalary()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void assignManager_employeeInOtherDept_throwsBusinessRule() {
        var eng = department(1L, "ENG", "Engineering");
        var fin = department(3L, "FIN", "Finance");
        when(departments.findById(1L)).thenReturn(Optional.of(eng));
        when(employees.findById(7L)).thenReturn(Optional.of(employee(7L, "Divya", "Menon", fin)));

        assertThatThrownBy(() -> service.assignManager(1L, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not a member of department ENG");
    }

    @Test
    void assignManager_member_setsManager() {
        var eng = department(1L, "ENG", "Engineering");
        when(departments.findById(1L)).thenReturn(Optional.of(eng));
        when(employees.findById(2L)).thenReturn(Optional.of(employee(2L, "Vikram", "Shah", eng)));
        when(employees.countByDepartmentId(1L)).thenReturn(4L);

        var response = service.assignManager(1L, 2L);

        assertThat(response.managerName()).isEqualTo("Vikram Shah");
    }

    @Test
    void findAll_includesHeadcount() {
        when(departments.findAll(any(Sort.class))).thenReturn(
                List.of(department(1L, "ENG", "Engineering"), department(9L, "OPS", "Operations")));
        when(departments.headcounts()).thenReturn(List.of(new DepartmentHeadcount(1L, 4L)));

        var all = service.findAll();

        assertThat(all).extracting(r -> r.code() + ":" + r.headcount()).containsExactly("ENG:4", "OPS:0");
    }

    @Test
    void rosterByCode_unknown_throwsNotFound() {
        when(departments.findByCodeIgnoreCase("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rosterByCode("XYZ"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("XYZ");
    }
}
