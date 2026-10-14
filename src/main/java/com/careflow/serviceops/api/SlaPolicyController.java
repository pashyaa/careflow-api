package com.careflow.serviceops.api;

import com.careflow.serviceops.api.dto.SlaPolicyRequest;
import com.careflow.serviceops.api.dto.SlaPolicyResponse;
import com.careflow.serviceops.api.dto.SlaPreviewResponse;
import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.service.SlaPolicyService;
import com.careflow.serviceops.service.SlaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sla-policies")
public class SlaPolicyController {

    private final SlaPolicyService policyService;
    private final SlaService slaService;

    public SlaPolicyController(SlaPolicyService policyService, SlaService slaService) {
        this.policyService = policyService;
        this.slaService = slaService;
    }

    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @GetMapping
    public List<SlaPolicyResponse> list() {
        return policyService.list();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<SlaPolicyResponse> create(@Valid @RequestBody SlaPolicyRequest request) {
        SlaPolicyResponse created = policyService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/sla-policies/" + created.id())).body(created);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public SlaPolicyResponse update(@PathVariable UUID id, @Valid @RequestBody SlaPolicyRequest request) {
        return policyService.update(id, request);
    }

    /** Which policy and deadlines WOULD apply: used by the create form. Saves nothing. */
    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @GetMapping("/preview")
    public SlaPreviewResponse preview(@RequestParam UUID siteId,
                                      @RequestParam(required = false) UUID assetId,
                                      @RequestParam Priority priority) {
        return SlaPreviewResponse.from(slaService.preview(siteId, assetId, priority));
    }
}