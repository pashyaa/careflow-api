package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.*;
import com.careflow.serviceops.domain.*;
import com.careflow.serviceops.exception.BusinessRuleException;
import com.careflow.serviceops.exception.ResourceNotFoundException;
import com.careflow.serviceops.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkOrderService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "targetResolutionAt", "priority", "status", "referenceNumber"
    );

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final ServiceSiteRepository siteRepository;
    private final AssetRepository assetRepository;
    private final TechnicianRepository technicianRepository;
    private final StatusTransitionPolicy transitionPolicy;
    private final WorkOrderMapper mapper;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            WorkOrderStatusHistoryRepository historyRepository,
                            ServiceSiteRepository siteRepository,
                            AssetRepository assetRepository,
                            TechnicianRepository technicianRepository,
                            StatusTransitionPolicy transitionPolicy,
                            WorkOrderMapper mapper) {
        this.workOrderRepository = workOrderRepository;
        this.historyRepository = historyRepository;
        this.siteRepository = siteRepository;
        this.assetRepository = assetRepository;
        this.technicianRepository = technicianRepository;
        this.transitionPolicy = transitionPolicy;
        this.mapper = mapper;
    }

    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest request) {
        ServiceSite site = siteRepository.findById(request.siteId())
                .filter(ServiceSite::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active service site not found: " + request.siteId()));

        Asset asset = request.assetId() == null ? null : assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + request.assetId()));
        if (asset != null && !asset.getSite().getId().equals(site.getId())) {
            throw new BusinessRuleException("Selected asset does not belong to the selected service site.");
        }
        if (asset != null && asset.getStatus() == AssetStatus.RETIRED) {
            throw new BusinessRuleException("A work order cannot be created for a retired asset.");
        }

        WorkOrder workOrder = new WorkOrder(
                newReferenceNumber(), request.title().trim(), request.description().trim(), request.priority(),
                site, asset, request.targetResolutionAt()
        );
        WorkOrder saved = workOrderRepository.save(workOrder);
        historyRepository.save(new WorkOrderStatusHistory(
                saved, null, WorkOrderStatus.NEW, "Work order created", "Operations Console"
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
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), safeSize, sort);
        Page<WorkOrder> result = workOrderRepository.findAll(
                WorkOrderSpecifications.filteredBy(query, status, priority, siteId, technicianId), pageRequest
        );
        return PageResponse.from(result, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public WorkOrderResponse findById(UUID id) {
        return mapper.toResponse(load(id));
    }

    @Transactional
    public WorkOrderResponse assign(UUID id, AssignTechnicianRequest request) {
        WorkOrder workOrder = load(id);
        Technician technician = technicianRepository.findById(request.technicianId())
                .filter(Technician::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active technician not found: " + request.technicianId()));

        WorkOrderStatus originalStatus = workOrder.getStatus();
        workOrder.assignTo(technician);
        if (originalStatus == WorkOrderStatus.NEW) {
            transitionPolicy.verify(originalStatus, WorkOrderStatus.ASSIGNED);
            workOrder.changeStatus(WorkOrderStatus.ASSIGNED);
            historyRepository.save(new WorkOrderStatusHistory(
                    workOrder, originalStatus, WorkOrderStatus.ASSIGNED,
                    "Assigned to " + technician.getFullName(), request.changedBy().trim()
            ));
        }
        return mapper.toResponse(workOrder);
    }

    @Transactional
    public WorkOrderResponse transition(UUID id, StatusTransitionRequest request) {
        WorkOrder workOrder = load(id);
        WorkOrderStatus originalStatus = workOrder.getStatus();
        transitionPolicy.verify(originalStatus, request.status());
        if (request.status() == WorkOrderStatus.IN_PROGRESS && workOrder.getAssignedTechnician() == null) {
            throw new BusinessRuleException("Assign a technician before starting work.");
        }

        workOrder.changeStatus(request.status());
        historyRepository.save(new WorkOrderStatusHistory(
                workOrder, originalStatus, request.status(), normalizedNote(request.note()), request.changedBy().trim()
        ));
        return mapper.toResponse(workOrder);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(UUID id) {
        if (!workOrderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Work order not found: " + id);
        }
        return historyRepository.findByWorkOrderIdOrderByChangedAtAsc(id).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    private WorkOrder load(UUID id) {
        return workOrderRepository.findOneById(id)
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

