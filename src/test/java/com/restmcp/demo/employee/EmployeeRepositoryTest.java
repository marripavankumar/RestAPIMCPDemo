package com.restmcp.demo.employee;

import com.restmcp.demo.department.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EmployeeRepositoryTest {

    @Autowired
    EmployeeRepository employees;

    @Autowired
    DepartmentRepository departments;

    @Test
    void findByDepartmentId_returnsMembersWithDepartmentLoaded() {
        Long engId = departments.findByCodeIgnoreCase("ENG").orElseThrow().getId();

        var result = employees.findByDepartmentId(engId);

        assertThat(result).hasSize(4);
        assertThat(result).allSatisfy(e -> assertThat(e.getDepartment().getCode()).isEqualTo("ENG"));
    }

    @Test
    void existsByEmailIgnoreCase_matchesRegardlessOfCase() {
        assertThat(employees.existsByEmailIgnoreCase("ASHA.RAO@EXAMPLE.COM")).isTrue();
        assertThat(employees.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void countByDepartmentId_countsMembers() {
        Long finId = departments.findByCodeIgnoreCase("FIN").orElseThrow().getId();

        assertThat(employees.countByDepartmentId(finId)).isEqualTo(3);
    }

    @Test
    void search_byPartialNameCaseInsensitive() {
        var page = employees.findAll(EmployeeSpecifications.matching("RAO", null, null), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Employee::getEmail).containsExactly("asha.rao@example.com");
    }

    @Test
    void search_byFullName() {
        var page = employees.findAll(EmployeeSpecifications.matching("asha rao", null, null), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_byDepartmentAndStatus() {
        Long engId = departments.findByCodeIgnoreCase("ENG").orElseThrow().getId();

        var page = employees.findAll(EmployeeSpecifications.matching(null, engId, EmployeeStatus.ACTIVE),
                PageRequest.of(0, 10, Sort.by("lastName")));

        assertThat(page.getContent()).extracting(Employee::getLastName).containsExactly("Iyer", "Rao", "Shah");
    }

    @Test
    void search_withNoCriteria_returnsEveryone() {
        assertThat(employees.findAll(EmployeeSpecifications.matching(null, null, null), PageRequest.of(0, 50))
                .getTotalElements()).isEqualTo(12);
    }
}
