package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.OptionResponse;
import com.careflow.serviceops.domain.AssetStatus;
import com.careflow.serviceops.repository.AssetRepository;
import com.careflow.serviceops.repository.ServiceSiteRepository;
import com.careflow.serviceops.repository.TechnicianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final ServiceSiteRepository siteRepository;
    private final AssetRepository assetRepository;
    private final TechnicianRepository technicianRepository;

    public ReferenceDataService(ServiceSiteRepository siteRepository, AssetRepository assetRepository,
                                TechnicianRepository technicianRepository) {
        this.siteRepository = siteRepository;
        this.assetRepository = assetRepository;
        this.technicianRepository = technicianRepository;
    }

    public List<OptionResponse> sites() {
        return siteRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(site -> new OptionResponse(site.getId(), site.getSiteCode(), site.getName(), site.getCustomerName()))
                .toList();
    }

    public List<OptionResponse> assets(UUID siteId) {
        return assetRepository.findBySiteIdAndStatusNotInOrderByNameAsc(siteId, List.of(AssetStatus.RETIRED)).stream()
                .map(asset -> new OptionResponse(asset.getId(), asset.getAssetTag(), asset.getName(), asset.getCategory()))
                .toList();
    }

    public List<OptionResponse> technicians() {
        return technicianRepository.findByActiveTrueOrderByFullNameAsc().stream()
                .map(technician -> new OptionResponse(
                        technician.getId(), technician.getEmployeeCode(), technician.getFullName(), technician.getPrimarySkill()
                ))
                .toList();
    }
}

