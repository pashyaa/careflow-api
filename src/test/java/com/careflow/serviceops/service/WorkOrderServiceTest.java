package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.AssignTechnicianRequest;
import com.careflow.serviceops.api.dto.CreateWorkOrderRequest;
import com.careflow.serviceops.domain.*;
import com.careflow.serviceops.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private WorkOrderService service;

    @BeforeEach
    void setUp() {
        service = new WorkOrderService(
                workOrderRepository, historyRepository, siteRepository, assetRepository, technicianRepository,
                new StatusTransitionPolicy(), new WorkOrderMapper()
        );
    }

    @Test
    void createsWorkOrderAndInitialHistory() {
        UUID siteId = UUID.randomUUID();
        ServiceSite site = mock(ServiceSite.class);
        when(site.getId()).thenReturn(siteId);
        when(site.isActive()).thenReturn(true);
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new CreateWorkOrderRequest(
                "Cooling alarm", "Investigate repeated high temperature alarms", Priority.HIGH,
                siteId, null, OffsetDateTime.now().plusDays(1)
        ));

        assertThat(response.status()).isEqualTo(WorkOrderStatus.NEW);
        assertThat(response.referenceNumber()).startsWith("WO-");
        verify(historyRepository).save(any(WorkOrderStatusHistory.class));
    }

    @Test
    void assigningNewWorkOrderMovesItToAssigned() {
        UUID workOrderId = UUID.randomUUID();
        UUID technicianId = UUID.randomUUID();
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

        var response = service.assign(workOrderId, new AssignTechnicianRequest(technicianId, "Dispatcher"));

        assertThat(response.status()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(response.assignedTechnician().name()).isEqualTo("Ananya Rao");
        verify(historyRepository).save(any(WorkOrderStatusHistory.class));
    }
}

