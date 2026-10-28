package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.AssignTechnicianRequest;
import com.careflow.serviceops.api.dto.CreateWorkOrderRequest;
import com.careflow.serviceops.api.dto.StatusTransitionRequest;
import com.careflow.serviceops.domain.*;
import com.careflow.serviceops.exception.ResourceNotFoundException;
import com.careflow.serviceops.repository.*;
import com.careflow.serviceops.security.Actor;
import com.careflow.serviceops.security.CurrentActorResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.careflow.serviceops.exception.FieldValidationException;
import java.util.Map;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock SlaService slaService;
    @Mock ReasonCodeRepository reasonCodeRepository;

    private final UUID organizationId = UUID.randomUUID();
    private WorkOrderService service;

    @BeforeEach
    void setUp() {
        service = new WorkOrderService(
                workOrderRepository, historyRepository, siteRepository, assetRepository, technicianRepository,
                new StatusTransitionPolicy(), new WorkOrderMapper(), currentActorResolver, slaService,
                reasonCodeRepository
        );
    }

    @Test
    void createsWorkOrderAndInitialHistory() {
        UUID siteId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ServiceSite site = mock(ServiceSite.class);
        when(site.getId()).thenReturn(siteId);
        when(site.isActive()).thenReturn(true);
        when(currentActorResolver.resolve()).thenReturn(new Actor(actorId, "Pat Iyer", organizationId));
        when(siteRepository.findByIdAndOrganizationId(siteId, organizationId)).thenReturn(Optional.of(site));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime resolutionDue = now.plusHours(8);
        when(slaService.plan(any(), any(), any(), any())).thenReturn(new SlaPlan(
                UUID.randomUUID(), "High - default", "PRIORITY", 120, 480, true, "Asia/Kolkata",
                now.plusHours(2), now.plusMinutes(90), resolutionDue, now.plusHours(6)));

        var response = service.create(new CreateWorkOrderRequest(
                "Cooling alarm", "Investigate repeated high temperature alarms", Priority.HIGH, siteId, null
        ));

        assertThat(response.status()).isEqualTo(WorkOrderStatus.NEW);
        assertThat(response.referenceNumber()).startsWith("WO-");
        // CF-106: the deadline comes from the policy, and the snapshot is on the work order
        assertThat(response.targetResolutionAt()).isEqualTo(resolutionDue);
        assertThat(response.sla().policyName()).isEqualTo("High - default");
        assertThat(response.sla().responseMinutes()).isEqualTo(120);

        ArgumentCaptor<WorkOrderStatusHistory> captor = ArgumentCaptor.forClass(WorkOrderStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedByUserId()).isEqualTo(actorId);
        assertThat(captor.getValue().getChangedByDisplayName()).isEqualTo("Pat Iyer");
    }

    @Test
    void creatingWorkOrderForAnotherTenantsSiteIsTreatedAsNotFound() {
        // CF-102: a siteId that belongs to a different tenant must 404, exactly like
        // a siteId that doesn't exist — findByIdAndOrganizationId is what enforces this.
        UUID siteId = UUID.randomUUID();
        when(currentActorResolver.resolve()).thenReturn(new Actor(UUID.randomUUID(), "Pat Iyer", organizationId));
        when(siteRepository.findByIdAndOrganizationId(siteId, organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateWorkOrderRequest(
                "Cooling alarm", "Investigate repeated high temperature alarms", Priority.HIGH,
                siteId, null
        ))).isInstanceOf(ResourceNotFoundException.class);
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
                organizationId, "WO-20260824-TEST0001", "Cooling alarm", "Investigate alarm", Priority.HIGH,
                site, null, OffsetDateTime.now().plusHours(4)
        );
        when(currentActorResolver.resolve()).thenReturn(new Actor(actorId, "Riley D'Souza", organizationId));
        when(workOrderRepository.findOneByIdAndOrganizationId(workOrderId, organizationId)).thenReturn(Optional.of(workOrder));
        when(technicianRepository.findByIdAndOrganizationId(technicianId, organizationId)).thenReturn(Optional.of(technician));

        var response = service.assign(workOrderId, new AssignTechnicianRequest(technicianId), 0L);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.ASSIGNED);
        assertThat(response.assignedTechnician().name()).isEqualTo("Ananya Rao");

        ArgumentCaptor<WorkOrderStatusHistory> captor = ArgumentCaptor.forClass(WorkOrderStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getChangedByUserId()).isEqualTo(actorId);
        assertThat(captor.getValue().getChangedByDisplayName()).isEqualTo("Riley D'Souza");
    }

    @Test
    void fetchingAnotherTenantsWorkOrderByIdIsTreatedAsNotFound() {
        // CF-102: this is the core acceptance check — a cross-tenant id 404s rather
        // than either leaking data or revealing existence via a 403.
        UUID workOrderId = UUID.randomUUID();
        when(currentActorResolver.resolve()).thenReturn(new Actor(UUID.randomUUID(), "Someone", organizationId));
        when(workOrderRepository.findOneByIdAndOrganizationId(workOrderId, organizationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(workOrderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void historyForAnotherTenantsWorkOrderIsTreatedAsNotFound() {
        UUID workOrderId = UUID.randomUUID();
        when(currentActorResolver.resolve()).thenReturn(new Actor(UUID.randomUUID(), "Someone", organizationId));
        when(workOrderRepository.existsByIdAndOrganizationId(workOrderId, organizationId)).thenReturn(false);

        assertThatThrownBy(() -> service.history(workOrderId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(historyRepository, never()).findByWorkOrderIdAndOrganizationIdOrderByChangedAtAsc(any(), any());
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
        // BUG-305: reasonCode is allowed; an actor identity (changedBy) still must not be.
        assertThat(componentNames).containsExactlyInAnyOrder("status", "note", "reasonCode");
        assertThat(componentNames).doesNotContain("changedBy");
    }

    @Test
    void createWorkOrderRequestHasNoWayToSupplyAnOrganizationId() {
        // CF-102: organizationId must only ever come from the authenticated actor,
        // never from the request body.
        var componentNames = java.util.Arrays.stream(CreateWorkOrderRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();
        assertThat(componentNames).doesNotContain("organizationId");
    }
    // ---- BUG-305: required resolution / cancellation evidence ----------------------------

    private WorkOrder newWorkOrderInStatusNew() {
        ServiceSite site = mock(ServiceSite.class);
        return new WorkOrder(organizationId, "WO-TEST-1", "Title", "Description",
                Priority.HIGH, site, null, OffsetDateTime.now().plusHours(4));
    }

    private UUID stubActorAndWorkOrder(WorkOrder workOrder) {
        UUID id = UUID.randomUUID();
        when(currentActorResolver.resolve()).thenReturn(new Actor(UUID.randomUUID(), "Pat Iyer", organizationId));
        when(workOrderRepository.findOneByIdAndOrganizationId(id, organizationId)).thenReturn(Optional.of(workOrder));
        return id;
    }

    @Test
    void cancellingWithoutReasonCodeOrNoteReturnsBothFieldErrors() {
        UUID id = stubActorAndWorkOrder(newWorkOrderInStatusNew());

        assertThatThrownBy(() -> service.transition(id,
                new StatusTransitionRequest(WorkOrderStatus.CANCELLED, null, null), 0))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getErrors()).containsKeys("reasonCode", "note"));
        verify(historyRepository, never()).save(any());
    }

    @Test
    void whitespaceOnlyNoteIsRejected() {
        UUID id = stubActorAndWorkOrder(newWorkOrderInStatusNew());
        when(reasonCodeRepository.findByCategoryAndCodeAndActiveTrue(ReasonCategory.CANCELLATION, "DUPLICATE"))
                .thenReturn(Optional.of(new ReasonCode(ReasonCategory.CANCELLATION, "DUPLICATE", "Duplicate work order", 10)));

        assertThatThrownBy(() -> service.transition(id,
                new StatusTransitionRequest(WorkOrderStatus.CANCELLED, "     ", "DUPLICATE"), 0))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getErrors()).containsOnlyKeys("note"));
    }

    @Test
    void unknownReasonCodeIsRejected() {
        UUID id = stubActorAndWorkOrder(newWorkOrderInStatusNew());
        when(reasonCodeRepository.findByCategoryAndCodeAndActiveTrue(ReasonCategory.CANCELLATION, "BOGUS"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transition(id,
                new StatusTransitionRequest(WorkOrderStatus.CANCELLED, "Raised against the wrong site", "bogus"), 0))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getErrors()).containsOnlyKeys("reasonCode"));
    }

    @Test
    void validCancellationStoresReasonCodeLabelAndNoteInHistory() {
        UUID id = stubActorAndWorkOrder(newWorkOrderInStatusNew());
        when(reasonCodeRepository.findByCategoryAndCodeAndActiveTrue(ReasonCategory.CANCELLATION, "DUPLICATE"))
                .thenReturn(Optional.of(new ReasonCode(ReasonCategory.CANCELLATION, "DUPLICATE", "Duplicate work order", 10)));

        var response = service.transition(id,
                new StatusTransitionRequest(WorkOrderStatus.CANCELLED, "  Duplicate of WO-20261001-ABCD1234  ", "duplicate"), 0);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.CANCELLED);
        ArgumentCaptor<WorkOrderStatusHistory> captor = ArgumentCaptor.forClass(WorkOrderStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getReasonCode()).isEqualTo("DUPLICATE");
        assertThat(captor.getValue().getReasonLabel()).isEqualTo("Duplicate work order");
        assertThat(captor.getValue().getNote()).isEqualTo("Duplicate of WO-20261001-ABCD1234");
    }

    @Test
    void reasonCodeIsRejectedOnNonTerminalTransitions() {
        UUID id = stubActorAndWorkOrder(newWorkOrderInStatusNew());
        // NEW -> ASSIGNED passes the state machine; the stray reason code must still be refused.
        assertThatThrownBy(() -> service.transition(id,
                new StatusTransitionRequest(WorkOrderStatus.ASSIGNED, null, "FIXED"), 0))
                .isInstanceOf(FieldValidationException.class);
    }
    // ---- BUG-305: resolution path + note length ------------------------------------------

    private WorkOrder newWorkOrderInProgress() {
        WorkOrder workOrder = newWorkOrderInStatusNew();
        workOrder.assignTo(mock(Technician.class));
        workOrder.changeStatus(WorkOrderStatus.ASSIGNED);
        workOrder.changeStatus(WorkOrderStatus.IN_PROGRESS);
        return workOrder;
    }

    @Test
    void resolvingWithoutReasonCodeOrNoteReturnsBothFieldErrors() {
        UUID id = stubActorAndWorkOrder(newWorkOrderInProgress());

        assertThatThrownBy(() -> service.transition(id,
                new StatusTransitionRequest(WorkOrderStatus.RESOLVED, "   ", null), 0))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getErrors()).containsKeys("reasonCode", "note"));
        verify(historyRepository, never()).save(any());
    }

    @Test
    void noteShorterThanMinimumLengthIsRejected() {
        UUID id = stubActorAndWorkOrder(newWorkOrderInProgress());
        when(reasonCodeRepository.findByCategoryAndCodeAndActiveTrue(ReasonCategory.RESOLUTION, "FIXED"))
                .thenReturn(Optional.of(new ReasonCode(ReasonCategory.RESOLUTION, "FIXED", "Fault repaired", 10)));

        assertThatThrownBy(() -> service.transition(id,
                new StatusTransitionRequest(WorkOrderStatus.RESOLVED, "done", "FIXED"), 0))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getErrors()).containsOnlyKeys("note"));
    }

    @Test
    void cancellationReasonCodeCannotBeUsedToResolve() {
        UUID id = stubActorAndWorkOrder(newWorkOrderInProgress());
        when(reasonCodeRepository.findByCategoryAndCodeAndActiveTrue(ReasonCategory.RESOLUTION, "DUPLICATE"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transition(id,
                new StatusTransitionRequest(WorkOrderStatus.RESOLVED, "Replaced the pressure sensor", "DUPLICATE"), 0))
                .isInstanceOfSatisfying(FieldValidationException.class, ex ->
                        assertThat(ex.getErrors()).containsOnlyKeys("reasonCode"));
    }

    @Test
    void validResolutionStoresReasonCodeLabelAndNoteInHistory() {
        UUID id = stubActorAndWorkOrder(newWorkOrderInProgress());
        when(reasonCodeRepository.findByCategoryAndCodeAndActiveTrue(ReasonCategory.RESOLUTION, "FIXED"))
                .thenReturn(Optional.of(new ReasonCode(ReasonCategory.RESOLUTION, "FIXED", "Fault repaired", 10)));

        var response = service.transition(id,
                new StatusTransitionRequest(WorkOrderStatus.RESOLVED, "Replaced the pressure sensor and re-tested.", "fixed"), 0);

        assertThat(response.status()).isEqualTo(WorkOrderStatus.RESOLVED);
        ArgumentCaptor<WorkOrderStatusHistory> captor = ArgumentCaptor.forClass(WorkOrderStatusHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getToStatus()).isEqualTo(WorkOrderStatus.RESOLVED);
        assertThat(captor.getValue().getReasonCode()).isEqualTo("FIXED");
        assertThat(captor.getValue().getReasonLabel()).isEqualTo("Fault repaired");
        assertThat(captor.getValue().getNote()).isEqualTo("Replaced the pressure sensor and re-tested.");
    }
}