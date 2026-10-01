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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID>, JpaSpecificationExecutor<WorkOrder> {

    // CF-102: the only single-record lookup used anywhere in the service layer.
    // Deliberately named so it cannot be mistaken for the inherited, tenant-unaware
    // findById(UUID) — a cross-tenant id simply yields Optional.empty(), which the
    // service layer turns into a 404, never a 403 (so an attacker cannot distinguish
    // "not yours" from "doesn't exist").
    @EntityGraph(attributePaths = {"site", "asset", "assignedTechnician"})
    Optional<WorkOrder> findOneByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByIdAndOrganizationId(UUID id, UUID organizationId);

    @Override
    @EntityGraph(attributePaths = {"site", "asset", "assignedTechnician"})
    Page<WorkOrder> findAll(Specification<WorkOrder> specification, Pageable pageable);

    // Every dashboard aggregate is scoped by organizationId — without this, the
    // "open work orders" tile on tenant A's dashboard would silently include tenant
    // B's work orders too.
    long countByOrganizationIdAndStatusIn(UUID organizationId, Collection<WorkOrderStatus> statuses);
    long countByOrganizationIdAndStatusInAndAssignedTechnicianIsNull(UUID organizationId, Collection<WorkOrderStatus> statuses);
    long countByOrganizationIdAndPriorityAndStatusIn(UUID organizationId, Priority priority, Collection<WorkOrderStatus> statuses);
    long countByOrganizationIdAndTargetResolutionAtBeforeAndStatusIn(UUID organizationId, OffsetDateTime timestamp, Collection<WorkOrderStatus> statuses);
    long countByOrganizationIdAndStatus(UUID organizationId, WorkOrderStatus status);
    @Query("select w.version from WorkOrder w where w.id = :id and w.organizationId = :organizationId")
    Optional<Long> findVersionByIdAndOrganizationId(@Param("id") UUID id,
                                                    @Param("organizationId") UUID organizationId);
}