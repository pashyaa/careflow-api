package com.careflow.serviceops.api;

import com.careflow.serviceops.api.dto.OptionResponse;
import com.careflow.serviceops.service.ReferenceDataService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceDataController {

    private final ReferenceDataService service;

    public ReferenceDataController(ReferenceDataService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('PLANNER', 'TECHNICIAN', 'ADMIN')")
    @GetMapping("/sites")
    public List<OptionResponse> sites() { return service.sites(); }

    @PreAuthorize("hasAnyRole('PLANNER', 'TECHNICIAN', 'ADMIN')")
    @GetMapping("/assets")
    public List<OptionResponse> assets(@RequestParam UUID siteId) { return service.assets(siteId); }

    @PreAuthorize("hasAnyRole('PLANNER', 'TECHNICIAN', 'ADMIN')")
    @GetMapping("/technicians")
    public List<OptionResponse> technicians() { return service.technicians(); }
}