package com.careflow.serviceops.service;

import com.careflow.serviceops.api.dto.HistoryResponse;
import com.careflow.serviceops.api.dto.WorkOrderResponse;
import com.careflow.serviceops.domain.Asset;
import com.careflow.serviceops.domain.ServiceSite;
import com.careflow.serviceops.domain.Technician;
import com.careflow.serviceops.domain.WorkOrder;
import com.careflow.serviceops.domain.WorkOrderStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class WorkOrderMapper {

    public WorkOrderResponse toResponse(WorkOrder workOrder) {
        return new WorkOrderResponse(
                workOrder.getId(),
                workOrder.getReferenceNumber(),
                workOrder.getTitle(),
                workOrder.getDescription(),
                workOrder.getPriority(),
                workOrder.getStatus(),
                toSite(workOrder.getSite()),
                toAsset(workOrder.getAsset()),
                toTechnician(workOrder.getAssignedTechnician()),
                workOrder.getTargetResolutionAt(),
                workOrder.getResolvedAt(),
                workOrder.getCreatedAt(),
                workOrder.getUpdatedAt(),
                workOrder.getVersion()
        );
    }

    public HistoryResponse toHistoryResponse(WorkOrderStatusHistory history) {
        return new HistoryResponse(
                history.getId(), history.getFromStatus(), history.getToStatus(), history.getNote(),
                history.getChangedBy(), history.getChangedAt()
        );
    }

    private WorkOrderResponse.SiteView toSite(ServiceSite site) {
        return new WorkOrderResponse.SiteView(
                site.getId(), site.getSiteCode(), site.getName(), site.getCustomerName(), site.getCity()
        );
    }

    private WorkOrderResponse.AssetView toAsset(Asset asset) {
        if (asset == null) return null;
        return new WorkOrderResponse.AssetView(
                asset.getId(), asset.getAssetTag(), asset.getName(), asset.getCategory(), asset.getStatus().name()
        );
    }

    private WorkOrderResponse.TechnicianView toTechnician(Technician technician) {
        if (technician == null) return null;
        return new WorkOrderResponse.TechnicianView(
                technician.getId(), technician.getEmployeeCode(), technician.getFullName(),
                technician.getEmail(), technician.getPrimarySkill()
        );
    }
}

