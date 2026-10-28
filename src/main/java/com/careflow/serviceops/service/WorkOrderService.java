package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.*;
import com.careflow.serviceops.domain.*;
import com.careflow.serviceops.exception.BusinessRuleException;
import com.careflow.serviceops.exception.FieldValidationException;
import com.careflow.serviceops.exception.ResourceNotFoundException;
import com.careflow.serviceops.exception.StaleVersionException;
import com.careflow.serviceops.repository.*;
import com.careflow.serviceops.security.Actor;
import com.careflow.serviceops.security.CurrentActorResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkOrderService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "targetResolutionAt", "priority", "status", "referenceNumber"
    );
    static final int MIN_EVIDENCE_NOTE_LENGTH = 10;

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final ServiceSiteRepository siteRepository;
    private final AssetRepository assetRepository;
    private final TechnicianRepository technicianRepository;
    private final StatusTransitionPolicy transitionPolicy;
    private final WorkOrderMapper mapper;
    private final CurrentActorResolver currentActorResolver;
    private final SlaService slaService;
    private final ReasonCodeRepository reasonCodeRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            WorkOrderStatusHistoryRepository historyRepository,
                            ServiceSiteRepository siteRepository,
                            AssetRepository assetRepository,
                            TechnicianRepository technicianRepository,
                            StatusTransitionPolicy transitionPolicy,
                            WorkOrderMapper mapper,
                            CurrentActorResolver currentActorResolver,
                            SlaService slaService,
                            ReasonCodeRepository reasonCodeRepository) {
        this.workOrderRepository = workOrderRepository;
        this.historyRepository = historyRepository;
        this.siteRepository = siteRepository;
        this.assetRepository = assetRepository;
        this.technicianRepository = technicianRepository;
        this.transitionPolicy = transitionPolicy;
        this.mapper = mapper;
        this.currentActorResolver = currentActorResolver;
        this.slaService = slaService;
        this.reasonCodeRepository = reasonCodeRepository;
    }

    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest request) {
        Actor actor = currentActorResolver.resolve();
        UUID organizationId = actor.organizationId();

        // CF-102: findByIdAndOrganizationId means a siteId from another tenant is
        // indistinguishable from a siteId that doesn't exist at all — both 404.
        ServiceSite site = siteRepository.findByIdAndOrganizationId(request.siteId(), organizationId)
                .filter(ServiceSite::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active service site not found: " + request.siteId()));

        Asset asset = request.assetId() == null ? null : assetRepository.findByIdAndOrganizationId(request.assetId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + request.assetId()));
        if (asset != null && !asset.getSite().getId().equals(site.getId())) {
            throw new BusinessRuleException("Selected asset does not belong to the selected service site.");
        }
        if (asset != null && asset.getStatus() == AssetStatus.RETIRED) {
            throw new BusinessRuleException("A work order cannot be created for a retired asset.");
        }

        // CF-106: the deadline is no longer typed in by the planner. Pick the most specific
        // policy, compute deadlines in the site's time zone, and snapshot it all onto the work order.
        SlaPlan sla = slaService.plan(site, asset, request.priority(), OffsetDateTime.now());
        WorkOrder workOrder = new WorkOrder(
                organizationId, newReferenceNumber(), request.title().trim(), request.description().trim(),
                request.priority(), site, asset, sla.resolutionDueAt()
        );
        workOrder.applySla(sla);
        WorkOrder saved = workOrderRepository.save(workOrder);
        historyRepository.save(new WorkOrderStatusHistory(
                saved, null, WorkOrderStatus.NEW, "Work order created", actor.userId(), actor.displayName()
        ));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> findAll(String query, WorkOrderStatus status, Priority priority,
                                                   UUID siteId, UUID technicianId, int page, int size,
                                                   String sortBy, String direction) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }
        UUID organizationId = currentActorResolver.resolve().organizationId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), safeSize, sort);
        Page<WorkOrder> result = workOrderRepository.findAll(
                WorkOrderSpecifications.filteredBy(organizationId, query, status, priority, siteId, technicianId),
                pageRequest
        );
        return PageResponse.from(result, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse findById(UUID id) {
        return mapper.toResponse(load(id, currentActorResolver.resolve().organizationId()));
    }

    @Transactional
    public WorkOrderResponse assign(UUID id, AssignTechnicianRequest request, long expectedVersion) {
        Actor actor = currentActorResolver.resolve();
        WorkOrder workOrder = load(id, actor.organizationId());
        verifyVersion(workOrder, expectedVersion);

        Technician technician = technicianRepository.findByIdAndOrganizationId(request.technicianId(), actor.organizationId())
                .filter(Technician::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active technician not found: " + request.technicianId()));

        WorkOrderStatus originalStatus = workOrder.getStatus();
        workOrder.assignTo(technician);
        if (originalStatus == WorkOrderStatus.NEW) {
            transitionPolicy.verify(originalStatus, WorkOrderStatus.ASSIGNED);
            workOrder.changeStatus(WorkOrderStatus.ASSIGNED);
            historyRepository.save(new WorkOrderStatusHistory(
                    workOrder, originalStatus, WorkOrderStatus.ASSIGNED,
                    "Assigned to " + technician.getFullName(), actor.userId(), actor.displayName()
            ));
        }
        workOrderRepository.flush();
        return mapper.toResponse(workOrder);
    }

    @Transactional
    public WorkOrderResponse transition(UUID id, StatusTransitionRequest request, long expectedVersion) {
        Actor actor = currentActorResolver.resolve();
        WorkOrder workOrder = load(id, actor.organizationId());
        verifyVersion(workOrder, expectedVersion);

        WorkOrderStatus originalStatus = workOrder.getStatus();
        transitionPolicy.verify(originalStatus, request.status());
        if (request.status() == WorkOrderStatus.IN_PROGRESS && workOrder.getAssignedTechnician() == null) {
            throw new BusinessRuleException("Assign a technician before starting work.");
        }

        // BUG-305: terminal transitions need a configured reason code + a meaningful note.
        ReasonCode reason = validateTerminalEvidence(request);

        workOrder.changeStatus(request.status());
        historyRepository.save(new WorkOrderStatusHistory(
                workOrder, originalStatus, request.status(), normalizedNote(request.note()),
                reason == null ? null : reason.getCode(),
                reason == null ? null : reason.getLabel(),
                actor.userId(), actor.displayName()
        ));
        workOrderRepository.flush();
        return mapper.toResponse(workOrder);
    }

    /**
     * Returns the validated reason code for RESOLVED / CANCELLED, or null for any other status.
     * Collects every problem so the UI can show all field errors at once.
     */
    private ReasonCode validateTerminalEvidence(StatusTransitionRequest request) {
        ReasonCategory category = ReasonCategory.forStatus(request.status());
        String code = request.reasonCode() == null ? "" : request.reasonCode().trim();

        if (category == null) {
            if (!code.isEmpty()) {
                throw new FieldValidationException(Map.of(
                        "reasonCode", "A reason code is only accepted when resolving or cancelling."));
            }
            return null;
        }

        Map<String, String> errors = new LinkedHashMap<>();
        String label = category == ReasonCategory.RESOLUTION ? "resolution" : "cancellation";

        ReasonCode reason = null;
        if (code.isEmpty()) {
            errors.put("reasonCode", "A " + label + " reason code is required.");
        } else {
            reason = reasonCodeRepository
                    .findByCategoryAndCodeAndActiveTrue(category, code.toUpperCase())
                    .orElse(null);
            if (reason == null) {
                errors.put("reasonCode", "'" + code + "' is not a valid " + label + " reason code.");
            }
        }

        String note = normalizedNote(request.note());   // trims; whitespace-only becomes null
        if (note == null) {
            errors.put("note", "A " + label + " note is required.");
        } else if (note.length() < MIN_EVIDENCE_NOTE_LENGTH) {
            errors.put("note", "The " + label + " note must be at least "
                    + MIN_EVIDENCE_NOTE_LENGTH + " characters.");
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        return reason;
    }

    private void verifyVersion(WorkOrder workOrder, long expectedVersion) {
        if (workOrder.getVersion() != expectedVersion) {
            throw new StaleVersionException(expectedVersion, workOrder.getVersion());
        }
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(UUID id) {
        UUID organizationId = currentActorResolver.resolve().organizationId();
        if (!workOrderRepository.existsByIdAndOrganizationId(id, organizationId)) {
            throw new ResourceNotFoundException("Work order not found: " + id);
        }
        return historyRepository.findByWorkOrderIdAndOrganizationIdOrderByChangedAtAsc(id, organizationId).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    private WorkOrder load(UUID id, UUID organizationId) {
        return workOrderRepository.findOneByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + id));
    }

    private String normalizedNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private String newReferenceNumber() {
        String date = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "WO-" + date + "-" + suffix;
    }
}