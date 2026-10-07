package com.restmcp.demo.employee;

import java.util.List;
import java.util.Optional;

import com.restmcp.demo.common.exception.BusinessRuleException;
import com.restmcp.demo.common.exception.ConflictException;
import com.restmcp.demo.common.exception.NotFoundException;
import com.restmcp.demo.department.DepartmentRepository;
import com.restmcp.demo.employee.dto.EmployeeSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static com.restmcp.demo.support.TestData.department;
import static com.restmcp.demo.support.TestData.employee;
import static com.restmcp.demo.support.TestData.request;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository employees;

    @Mock
    DepartmentRepository departments;

    @InjectMocks
    EmployeeService service;

    @Test
    void findById_unknownId_throwsNotFound() {
        when(employees.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Employee 99 not found");
    }

    @Test
    void create_duplicateEmail_throwsConflict() {
        when(employees.existsByEmailIgnoreCase("asha.rao@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("asha.rao@example.com", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("asha.rao@example.com");
        verify(employees, never()).save(any());
    }

    @Test
    void create_unknownDepartment_throwsNotFound() {
        when(departments.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("new@example.com", 7L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Department 7 not found");
    }

    @Test
    void create_valid_defaultsStatusActive_andSaves() {
        var eng = department(1L, "ENG", "Engineering");
        when(departments.findById(1L)).thenReturn(Optional.of(eng));
        when(employees.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request("new@example.com", 1L));

        assertThat(response.status()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(response.department().code()).isEqualTo("ENG");
        assertThat(response.email()).isEqualTo("new@example.com");
    }

    @Test
    void update_changingEmailToExisting_throwsConflict() {
        when(employees.findById(5L)).thenReturn(Optional.of(employee(5L, "Meera", "Iyer", null)));
        when(employees.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request("taken@example.com", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_keepingOwnEmail_isAllowed() {
        when(employees.findById(5L)).thenReturn(Optional.of(employee(5L, "Meera", "Iyer", null)));

        var response = service.update(5L, request("MEERA.IYER@example.com", null, EmployeeStatus.ON_LEAVE));

        assertThat(response.status()).isEqualTo(EmployeeStatus.ON_LEAVE);
        verify(employees, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void transfer_toSameDepartment_throwsBusinessRule() {
        var eng = department(1L, "ENG", "Engineering");
        when(employees.findById(5L)).thenReturn(Optional.of(employee(5L, "Meera", "Iyer", eng)));
        when(departments.findById(1L)).thenReturn(Optional.of(eng));

        assertThatThrownBy(() -> service.transfer(5L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already in department ENG");
    }

    @Test
    void transfer_terminated_throwsBusinessRule() {
        var e = employee(5L, "Meera", "Iyer", department(1L, "ENG", "Engineering"));
        e.setStatus(EmployeeStatus.TERMINATED);
        when(employees.findById(5L)).thenReturn(Optional.of(e));
        when(departments.findById(3L)).thenReturn(Optional.of(department(3L, "FIN", "Finance")));

        assertThatThrownBy(() -> service.transfer(5L, 3L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("TERMINATED");
    }

    @Test
    void transfer_valid_updatesDepartment() {
        when(employees.findById(5L))
                .thenReturn(Optional.of(employee(5L, "Meera", "Iyer", department(1L, "ENG", "Engineering"))));
        when(departments.findById(3L)).thenReturn(Optional.of(department(3L, "FIN", "Finance")));

        var response = service.transfer(5L, 3L);

        assertThat(response.department().code()).isEqualTo("FIN");
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_capsPageSizeAt100() {
        when(employees.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        service.search(new EmployeeSearchCriteria(null, null, null), PageRequest.of(0, 500));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(employees).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @SuppressWarnings("unchecked")
    void search_mapsResultsToPageResponse() {
        var eng = department(1L, "ENG", "Engineering");
        when(employees.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee(1L, "Asha", "Rao", eng)), PageRequest.of(0, 20), 1));

        var page = service.search(new EmployeeSearchCriteria("rao", null, null), PageRequest.of(0, 20));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().getFirst().fullName()).isEqualTo("Asha Rao");
    }

    @Test
    void delete_managerOfDepartment_clearsManagerFirst() {
        var eng = department(1L, "ENG", "Engineering");
        var manager = employee(1L, "Asha", "Rao", eng);
        eng.setManager(manager);
        when(employees.findById(1L)).thenReturn(Optional.of(manager));
        when(departments.findByManagerId(1L)).thenReturn(List.of(eng));

        service.delete(1L);

        assertThat(eng.getManager()).isNull();
        verify(employees).delete(manager);
    }

    @Test
    void delete_unknown_throwsNotFound() {
        when(employees.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(NotFoundException.class);
    }
}
