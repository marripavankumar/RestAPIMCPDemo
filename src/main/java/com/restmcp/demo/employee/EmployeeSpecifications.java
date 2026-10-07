package com.restmcp.demo.employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    /**
     * Builds a filter where every non-null argument must match. {@code name} is a case-insensitive
     * substring match against "firstName lastName".
     */
    public static Specification<Employee> matching(String name, Long departmentId, EmployeeStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(name)) {
                var fullName = cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName")));
                predicates.add(cb.like(fullName, "%" + name.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
