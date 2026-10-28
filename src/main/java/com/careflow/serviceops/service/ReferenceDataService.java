package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.OptionResponse;
import com.careflow.serviceops.domain.AssetStatus;
import com.careflow.serviceops.exception.ResourceNotFoundException;
import com.careflow.serviceops.repository.AssetRepository;
import com.careflow.serviceops.repository.ServiceSiteRepository;
import com.careflow.serviceops.repository.TechnicianRepository;
import com.careflow.serviceops.security.CurrentActorResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.careflow.serviceops.domain.ReasonCategory;
import com.careflow.serviceops.domain.WorkOrderStatus;
import com.careflow.serviceops.repository.ReasonCodeRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final ServiceSiteRepository siteRepository;
    private final AssetRepository assetRepository;
    private final TechnicianRepository technicianRepository;
    private final CurrentActorResolver currentActorResolver;
    private final ReasonCodeRepository reasonCodeRepository;

    public ReferenceDataService(ServiceSiteRepository siteRepository, AssetRepository assetRepository,
                                TechnicianRepository technicianRepository,
                                CurrentActorResolver currentActorResolver,
                                ReasonCodeRepository reasonCodeRepository) {
        this.siteRepository = siteRepository;
        this.assetRepository = assetRepository;
        this.technicianRepository = technicianRepository;
        this.currentActorResolver = currentActorResolver;
        this.reasonCodeRepository = reasonCodeRepository;
    }

    public List<OptionResponse> sites() {
        UUID organizationId = currentActorResolver.resolve().organizationId();
        return siteRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(organizationId).stream()
                .map(site -> new OptionResponse(site.getId(), site.getSiteCode(), site.getName(), site.getCustomerName()))
                .toList();
    }

    public List<OptionResponse> assets(UUID siteId) {
        UUID organizationId = currentActorResolver.resolve().organizationId();
        // CF-102: confirms the requested site is actually in this tenant before
        // listing anything for it — a cross-tenant siteId returns 404, not an empty
        // (and revealing) list, and never another tenant's assets.
        siteRepository.findByIdAndOrganizationId(siteId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Active service site not found: " + siteId));
        return assetRepository.findBySiteIdAndOrganizationIdAndStatusNotInOrderByNameAsc(
                        siteId, organizationId, List.of(AssetStatus.RETIRED)).stream()
                .map(asset -> new OptionResponse(asset.getId(), asset.getAssetTag(), asset.getName(), asset.getCategory()))
                .toList();
    }

    public List<OptionResponse> technicians() {
        UUID organizationId = currentActorResolver.resolve().organizationId();
        return technicianRepository.findByOrganizationIdAndActiveTrueOrderByFullNameAsc(organizationId).stream()
                .map(technician -> new OptionResponse(
                        technician.getId(), technician.getEmployeeCode(), technician.getFullName(), technician.getPrimarySkill()
                ))
                .toList();
    }
    /** BUG-305: the reason codes the UI should offer for a terminal target status. */
    public List<OptionResponse> reasonCodes(WorkOrderStatus status) {
        ReasonCategory category = ReasonCategory.forStatus(status);
        if (category == null) {
            throw new IllegalArgumentException("Reason codes only apply to RESOLVED or CANCELLED.");
        }
        return reasonCodeRepository.findByCategoryAndActiveTrueOrderBySortOrderAsc(category).stream()
                .map(reason -> new OptionResponse(reason.getId(), reason.getCode(), reason.getLabel(), null))
                .toList();
    }
}