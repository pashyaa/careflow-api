package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.SlaPolicyRequest;
import com.careflow.serviceops.api.dto.SlaPolicyResponse;
import com.careflow.serviceops.domain.SlaPolicy;
import com.careflow.serviceops.exception.BusinessRuleException;
import com.careflow.serviceops.exception.ResourceNotFoundException;
import com.careflow.serviceops.repository.ServiceSiteRepository;
import com.careflow.serviceops.repository.SlaPolicyRepository;
import com.careflow.serviceops.security.CurrentActorResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class SlaPolicyService {

    private final SlaPolicyRepository policyRepository;
    private final ServiceSiteRepository siteRepository;
    private final CurrentActorResolver currentActorResolver;

    public SlaPolicyService(SlaPolicyRepository policyRepository, ServiceSiteRepository siteRepository,
                            CurrentActorResolver currentActorResolver) {
        this.policyRepository = policyRepository;
        this.siteRepository = siteRepository;
        this.currentActorResolver = currentActorResolver;
    }

    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> list() {
        UUID organizationId = currentActorResolver.resolve().organizationId();
        return policyRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(SlaPolicyResponse::from)
                .toList();
    }

    @Transactional
    public SlaPolicyResponse create(SlaPolicyRequest request) {
        UUID organizationId = currentActorResolver.resolve().organizationId();
        validate(request, organizationId, null);
        SlaPolicy policy = new SlaPolicy(organizationId, request.name().trim(), request.siteId(),
                request.category(), request.priority(), request.responseMinutes(), request.resolutionMinutes(),
                request.businessHoursOnly(), isActive(request));
        return SlaPolicyResponse.from(policyRepository.saveAndFlush(policy));
    }

    @Transactional
    public SlaPolicyResponse update(UUID id, SlaPolicyRequest request) {
        UUID organizationId = currentActorResolver.resolve().organizationId();
        SlaPolicy policy = policyRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found: " + id));
        validate(request, organizationId, id);
        policy.update(request.name().trim(), request.siteId(), request.category(), request.priority(),
                request.responseMinutes(), request.resolutionMinutes(), request.businessHoursOnly(), isActive(request));
        return SlaPolicyResponse.from(policyRepository.saveAndFlush(policy));
    }

    private boolean isActive(SlaPolicyRequest request) {
        return request.active() == null || request.active();
    }

    private void validate(SlaPolicyRequest request, UUID organizationId, UUID selfId) {
        if (request.resolutionMinutes() < request.responseMinutes()) {
            throw new IllegalArgumentException("Resolution time must not be shorter than response time.");
        }
        if (request.siteId() != null) {
            // Cross-tenant site ids look exactly like missing ones (404).
            siteRepository.findByIdAndOrganizationId(request.siteId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service site not found: " + request.siteId()));
        }
        if (isActive(request)) {
            String category = lower(SlaPolicy.normalizeCategory(request.category()));
            boolean duplicate = policyRepository.findByOrganizationIdAndActiveTrue(organizationId).stream()
                    .filter(existing -> !existing.getId().equals(selfId))
                    .anyMatch(existing -> Objects.equals(existing.getSiteId(), request.siteId())
                            && Objects.equals(lower(existing.getCategory()), category)
                            && existing.getPriority() == request.priority());
            if (duplicate) {
                throw new BusinessRuleException(
                        "An active policy already exists for this site / category / priority combination.");
            }
        }
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}