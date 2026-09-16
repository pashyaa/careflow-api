package com.careflow.serviceops.api;

import com.careflow.serviceops.api.dto.*;
import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.WorkOrderStatus;
import com.careflow.serviceops.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-orders")
public class WorkOrderController {

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody CreateWorkOrderRequest request) {
        WorkOrderResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/work-orders/" + created.id())).body(created);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponse<WorkOrderResponse> findAll(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID technicianId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        return service.findAll(query, status, priority, siteId, technicianId, page, size, sortBy, direction);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public WorkOrderResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @PatchMapping("/{id}/assignment")
    public WorkOrderResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignTechnicianRequest request) {
        return service.assign(id, request);
    }

    @PreAuthorize("hasAnyRole('PLANNER', 'TECHNICIAN', 'ADMIN')")
    @PatchMapping("/{id}/status")
    public WorkOrderResponse transition(@PathVariable UUID id, @Valid @RequestBody StatusTransitionRequest request) {
        return service.transition(id, request);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@PathVariable UUID id) {
        return service.history(id);
    }
}