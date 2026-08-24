package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.WorkOrder;
import com.careflow.serviceops.domain.WorkOrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WorkOrderSpecifications {

    private WorkOrderSpecifications() {
    }

    public static Specification<WorkOrder> filteredBy(String query, WorkOrderStatus status, Priority priority,
                                                       UUID siteId, UUID technicianId) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("referenceNumber")), pattern),
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)
                ));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(builder.equal(root.get("priority"), priority));
            }
            if (siteId != null) {
                predicates.add(builder.equal(root.get("site").get("id"), siteId));
            }
            if (technicianId != null) {
                predicates.add(builder.equal(root.get("assignedTechnician").get("id"), technicianId));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}

