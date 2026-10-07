package com.restmcp.demo.employee;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    @EntityGraph(attributePaths = "department")
    List<Employee> findByDepartmentId(Long departmentId);

    boolean existsByEmailIgnoreCase(String email);

    long countByDepartmentId(Long departmentId);

    @Override
    @EntityGraph(attributePaths = "department")
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);
}
