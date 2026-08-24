package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.WorkOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkOrderStatusHistoryRepository extends JpaRepository<WorkOrderStatusHistory, UUID> {
    List<WorkOrderStatusHistory> findByWorkOrderIdOrderByChangedAtAsc(UUID workOrderId);
}

