package com.restmcp.demo.department;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    @Query("""
           select new com.restmcp.demo.department.DepartmentStats(count(e), sum(e.salary))
           from Employee e
           where e.department.id = :departmentId
           """)
    DepartmentStats statsFor(Long departmentId);

    @Query("""
           select new com.restmcp.demo.department.DepartmentHeadcount(e.department.id, count(e))
           from Employee e
           where e.department is not null
           group by e.department.id
           """)
    List<DepartmentHeadcount> headcounts();
}
