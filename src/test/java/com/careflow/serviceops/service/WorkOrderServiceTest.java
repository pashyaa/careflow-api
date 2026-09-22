package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.AssignTechnicianRequest;
import com.careflow.serviceops.api.dto.CreateWorkOrderRequest;
import com.careflow.serviceops.api.dto.StatusTransitionRequest;
import com.careflow.serviceops.domain.*;
import com.careflow.serviceops.repository.*;
import com.careflow.serviceops.security.Actor;
import com.careflow.serviceops.security.CurrentActorResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock WorkOrderRepository workOrderRepository;
    @Mock WorkOrderStatusHistoryRepository historyRepository;
    @Mock ServiceSiteRepository siteRepository;
    @Mock AssetRepository assetRepository;
    @Mock TechnicianRepository technicianRepository;
    @Mock CurrentActorResolver currentActorResolver;

    private WorkOrderService service;

    @BeforeEach
    void setUp() {
        service = new WorkOrderService(
                workOrderRepository, historyRepository, siteRepository, assetRepository, technicianRepository,
                new StatusTransitionPolicy(), new WorkOrderMapper(), currentActorResolver
        );
    }

    @Test
    void createsWorkOrderAndInitialHistory() {
        UUID siteId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ServiceSite site = mock(ServiceSite.class);
        when(site.getId()).thenReturn(siteId);
        when(site.isActive()).thenReturn(true);
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentActorResolver.resolve()).thenReturn(new Actor(actorId, "Pat Iyer"));

        var response = service.create(new CreateWorkOrderRequest(
                "Cooling alarm", "Investigate repeated high temperature alarms", Priority.HIGH,
                siteId, null, OffsetDateTime.now().plusDays(1)
        ));

        assertThat(response.status()).isEqualTo(WorkOrderStatus.NEW);
        assertThat(response.referenceNumber()).startsWith("WO-");

        ArgumentCaptor<WorkOrderStatusHistory> captor = ArgumentCaptor.forClass(WorkOrderStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedByUserId()).isEqualTo(actorId);
        assertThat(captor.getValue().getChangedByDisplayName()).isEqualTo("Pat Iyer");
    }

    @Test
    void assigningNewWorkOrderMovesItToAssigned() {
        UUID workOrderId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ServiceSite site = mock(ServiceSite.class);
        Technician technician = mock(Technician.class);
        when(technician.isActive()).thenReturn(true);
        when(technician.getFullName()).thenReturn("Ananya Rao");
        WorkOrder workOrder = new WorkOrder(
                "WO-20260824-TEST0001", "Cooling alarm", "Investigate alarm", Priority.HIGH,
                site, null, OffsetDateTime.now().plusHours(4)
        );
        when(workOrderRepository.findOneById(workOrderId)).thenReturn(Optional.of(workOrder));
        when(technicianRepository.findById(technicianId)).thenReturn(Optional.of(technician));
        when(currentActorResolver.resolve()).thenReturn(new Actor(actorId, "Riley D'Souza"));

        var response = service.assign(workOrderId, new AssignTechnicianRequest(technicianId));

        assertThat(response.status()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(response.assignedTechnician().name()).isEqualTo("Ananya Rao");

        ArgumentCaptor<WorkOrderStatusHistory> captor = ArgumentCaptor.forClass(WorkOrderStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedByUserId()).isEqualTo(actorId);
        assertThat(captor.getValue().getChangedByDisplayName()).isEqualTo("Riley D'Souza");
    }

    @Test
    void assignmentRequestHasNoWayToSupplyAnActorIdentity() {
        // CF-112: the request DTO must not expose a changedBy-style field at all, so
        // there is no client-controlled path to the audit identity — the compiler
        // enforces this rather than a runtime check.
        assertThat(AssignTechnicianRequest.class.getRecordComponents()).hasSize(1);
        assertThat(AssignTechnicianRequest.class.getRecordComponents()[0].getName()).isEqualTo("technicianId");
    }

    @Test
    void statusTransitionRequestHasNoWayToSupplyAnActorIdentity() {
        var componentNames = java.util.Arrays.stream(StatusTransitionRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();
        assertThat(componentNames).containsExactlyInAnyOrder("status", "note");
    }
}