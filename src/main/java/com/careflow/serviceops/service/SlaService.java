package com.careflow.serviceops.service;

import com.careflow.serviceops.domain.Asset;
import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.ServiceSite;
import com.careflow.serviceops.domain.SlaPlan;
import com.careflow.serviceops.domain.SlaPolicy;
import com.careflow.serviceops.exception.BusinessRuleException;
import com.careflow.serviceops.exception.ResourceNotFoundException;
import com.careflow.serviceops.repository.AssetRepository;
import com.careflow.serviceops.repository.ServiceSiteRepository;
import com.careflow.serviceops.repository.SlaHolidayRepository;
import com.careflow.serviceops.repository.SlaPolicyRepository;
import com.careflow.serviceops.security.CurrentActorResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SlaService {

    /** A work order is "at risk" once this share of its SLA window has been used. */
    static final int AT_RISK_PERCENT = 75;

    /** How far ahead holidays are loaded; comfortably beyond the longest allowed policy. */
    static final int HOLIDAY_LOOKAHEAD_DAYS = 800;

    private final SlaPolicyRepository policyRepository;
    private final SlaHolidayRepository holidayRepository;
    private final ServiceSiteRepository siteRepository;
    private final AssetRepository assetRepository;
    private final CurrentActorResolver currentActorResolver;

    public SlaService(SlaPolicyRepository policyRepository, SlaHolidayRepository holidayRepository,
                      ServiceSiteRepository siteRepository, AssetRepository assetRepository,
                      CurrentActorResolver currentActorResolver) {
        this.policyRepository = policyRepository;
        this.holidayRepository = holidayRepository;
        this.siteRepository = siteRepository;
        this.assetRepository = assetRepository;
        this.currentActorResolver = currentActorResolver;
    }

    /**
     * Selects the most specific active policy for this site / asset category / priority and
     * computes every deadline. Called when a work order is created; the result is copied onto
     * the work order so later policy edits cannot move its deadlines.
     */
    public SlaPlan plan(ServiceSite site, Asset asset, Priority priority, OffsetDateTime startedAt) {
        String category = asset == null ? null : asset.getCategory();

        SlaPolicy policy = SlaPolicyResolver
                .select(policyRepository.findByOrganizationIdAndActiveTrue(site.getOrganizationId()),
                        site.getId(), category, priority)
                .orElseThrow(() -> new BusinessRuleException(
                        "No active SLA policy matches this work order (priority " + priority
                                + (category == null ? "" : ", category " + category)
                                + "). Ask an administrator to configure one."));

        boolean businessHours = policy.isBusinessHoursOnly();
        SlaCalendar calendar = calendarFor(site, startedAt);

        int responseAtRisk = policy.getResponseMinutes() * AT_RISK_PERCENT / 100;
        int resolutionAtRisk = policy.getResolutionMinutes() * AT_RISK_PERCENT / 100;

        return new SlaPlan(
                policy.getId(), policy.getName(), policy.scopeLabel(),
                policy.getResponseMinutes(), policy.getResolutionMinutes(), businessHours,
                calendar.zone().getId(),
                SlaDeadlineCalculator.addMinutes(startedAt, policy.getResponseMinutes(), businessHours, calendar),
                SlaDeadlineCalculator.addMinutes(startedAt, responseAtRisk, businessHours, calendar),
                SlaDeadlineCalculator.addMinutes(startedAt, policy.getResolutionMinutes(), businessHours, calendar),
                SlaDeadlineCalculator.addMinutes(startedAt, resolutionAtRisk, businessHours, calendar)
        );
    }

    /** What-if for the create form: the same calculation, without saving anything. */
    public SlaPlan preview(UUID siteId, UUID assetId, Priority priority) {
        UUID organizationId = currentActorResolver.resolve().organizationId();
        ServiceSite site = siteRepository.findByIdAndOrganizationId(siteId, organizationId)
                .filter(ServiceSite::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active service site not found: " + siteId));
        Asset asset = assetId == null ? null : assetRepository.findByIdAndOrganizationId(assetId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
        if (asset != null && !asset.getSite().getId().equals(site.getId())) {
            throw new BusinessRuleException("Selected asset does not belong to the selected service site.");
        }
        return plan(site, asset, priority, OffsetDateTime.now());
    }

    private SlaCalendar calendarFor(ServiceSite site, OffsetDateTime startedAt) {
        try {
            ZoneId zone = ZoneId.of(site.getTimeZone());
            LocalDate from = startedAt.atZoneSameInstant(zone).toLocalDate();
            Set<LocalDate> holidays = new HashSet<>(holidayRepository.findHolidayDates(
                    site.getOrganizationId(), site.getId(), from, from.plusDays(HOLIDAY_LOOKAHEAD_DAYS)));
            return new SlaCalendar(zone, site.getBusinessStart(), site.getBusinessEnd(),
                    parseBusinessDays(site.getBusinessDays()), holidays);
        } catch (DateTimeException | IllegalArgumentException ex) {
            throw new BusinessRuleException("The working calendar of site " + site.getSiteCode()
                    + " is invalid: " + ex.getMessage());
        }
    }

    /** "MON,TUE,WED" -> {MONDAY, TUESDAY, WEDNESDAY}. At least the first three letters are required. */
    static Set<DayOfWeek> parseBusinessDays(String csv) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String token : csv.split(",")) {
            String key = token.trim().toUpperCase(Locale.ROOT);
            if (key.isEmpty()) continue;
            days.add(Arrays.stream(DayOfWeek.values())
                    .filter(day -> key.length() >= 3 && day.name().startsWith(key))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown business day '" + token.trim() + "'")));
        }
        return days;
    }
}