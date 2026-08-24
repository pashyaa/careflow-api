package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.WorkOrder;
import com.careflow.serviceops.domain.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID>, JpaSpecificationExecutor<WorkOrder> {

    @EntityGraph(attributePaths = {"site", "asset", "assignedTechnician"})
    Optional<WorkOrder> findOneById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"site", "asset", "assignedTechnician"})
    Page<WorkOrder> findAll(Specification<WorkOrder> specification, Pageable pageable);

    long countByStatusIn(Collection<WorkOrderStatus> statuses);
    long countByStatusInAndAssignedTechnicianIsNull(Collection<WorkOrderStatus> statuses);
    long countByPriorityAndStatusIn(Priority priority, Collection<WorkOrderStatus> statuses);
    long countByTargetResolutionAtBeforeAndStatusIn(OffsetDateTime timestamp, Collection<WorkOrderStatus> statuses);
    long countByStatus(WorkOrderStatus status);
}

