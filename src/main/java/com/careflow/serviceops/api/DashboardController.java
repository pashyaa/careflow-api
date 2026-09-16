package com.careflow.serviceops.api;

import com.careflow.serviceops.api.dto.DashboardSummaryResponse;
import com.careflow.serviceops.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return service.summary();
    }
}