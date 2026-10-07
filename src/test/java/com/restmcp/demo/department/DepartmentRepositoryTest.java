package com.restmcp.demo.department;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DepartmentRepositoryTest {

    @Autowired
    DepartmentRepository departments;

    @Test
    void findByCodeIgnoreCase_findsSeededDepartmentWithManager() {
        var eng = departments.findByCodeIgnoreCase("eng").orElseThrow();

        assertThat(eng.getName()).isEqualTo("Engineering");
        assertThat(eng.getManager().getEmail()).isEqualTo("asha.rao@example.com");
    }

    @Test
    void existsChecks_areCaseInsensitive() {
        assertThat(departments.existsByCodeIgnoreCase("hr")).isTrue();
        assertThat(departments.existsByNameIgnoreCase("FINANCE")).isTrue();
        assertThat(departments.existsByCodeIgnoreCase("OPS")).isFalse();
    }

    @Test
    void statsFor_returnsHeadcountAndTotalSalary() {
        Long finId = departments.findByCodeIgnoreCase("FIN").orElseThrow().getId();

        var stats = departments.statsFor(finId);

        assertThat(stats.headcount()).isEqualTo(3);
        assertThat(stats.totalSalary()).isEqualByComparingTo(new BigDecimal("340000.00"));
    }

    @Test
    void statsFor_emptyDepartment_returnsZeroes() {
        var ops = departments.saveAndFlush(new Department("OPS", "Operations", "Chennai"));

        var stats = departments.statsFor(ops.getId());

        assertThat(stats.headcount()).isZero();
        assertThat(stats.totalSalary()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void headcounts_returnsOneRowPerNonEmptyDepartment() {
        assertThat(departments.headcounts()).hasSize(4)
                .allSatisfy(h -> assertThat(h.headcount()).isGreaterThan(0));
    }
}
