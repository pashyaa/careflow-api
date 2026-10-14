package com.careflow.serviceops.service;

import com.careflow.serviceops.domain.*;
import com.careflow.serviceops.exception.BusinessRuleException;
import com.careflow.serviceops.repository.AssetRepository;
import com.careflow.serviceops.repository.ServiceSiteRepository;
import com.careflow.serviceops.repository.SlaHolidayRepository;
import com.careflow.serviceops.repository.SlaPolicyRepository;
import com.careflow.serviceops.security.CurrentActorResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SlaServiceTest {

    @Mock SlaPolicyRepository policyRepository;
    @Mock SlaHolidayRepository holidayRepository;
    @Mock ServiceSiteRepository siteRepository;
    @Mock AssetRepository assetRepository;
    @Mock CurrentActorResolver currentActorResolver;

    private final UUID orgId = UUID.randomUUID();
    private final UUID siteId = UUID.randomUUID();
    private SlaService service;
    private ServiceSite site;

    @BeforeEach
    void setUp() {
        service = new SlaService(policyRepository, holidayRepository, siteRepository, assetRepository, currentActorResolver);
        site = mock(ServiceSite.class);
        lenient().when(site.getId()).thenReturn(siteId);
        lenient().when(site.getOrganizationId()).thenReturn(orgId);
        lenient().when(site.getSiteCode()).thenReturn("BLR-DC-01");
        lenient().when(site.getTimeZone()).thenReturn("Asia/Kolkata");
        lenient().when(site.getBusinessStart()).thenReturn(LocalTime.of(9, 0));
        lenient().when(site.getBusinessEnd()).thenReturn(LocalTime.of(18, 0));
        lenient().when(site.getBusinessDays()).thenReturn("MON,TUE,WED,THU,FRI");
        lenient().when(holidayRepository.findHolidayDates(any(), any(), any(), any())).thenReturn(List.of());
    }

    private SlaPolicy policy(UUID siteScope, String category, Priority priority, int response, int resolution, boolean businessOnly) {
        SlaPolicy policy = new SlaPolicy(orgId, "policy", siteScope, category, priority, response, resolution, businessOnly, true);
        ReflectionTestUtils.setField(policy, "id", UUID.randomUUID());
        return policy;
    }

    @Test
    void missingPolicyIsRejectedWithAClearMessage() {
        lenient().when(policyRepository.findByOrganizationIdAndActiveTrue(orgId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.plan(site, null, Priority.HIGH, OffsetDateTime.parse("2026-10-09T10:00+05:30")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No active SLA policy");
    }

    @Test
    void anInactiveOnlyPolicySetCountsAsMissing() {
        var inactive = new SlaPolicy(orgId, "old", null, null, null, 30, 60, false, false);
        lenient().when(policyRepository.findByOrganizationIdAndActiveTrue(orgId)).thenReturn(List.of(inactive));

        assertThatThrownBy(() -> service.plan(site, null, Priority.LOW, OffsetDateTime.now()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void mostSpecificPolicyIsSelectedAndSnapshotted() {
        SlaPolicy fallback = policy(null, null, null, 480, 4320, true);
        SlaPolicy hvacCritical = policy(null, "HVAC", Priority.CRITICAL, 20, 180, false);
        lenient().when(policyRepository.findByOrganizationIdAndActiveTrue(orgId)).thenReturn(List.of(fallback, hvacCritical));
        Asset asset = mock(Asset.class);
        lenient().when(asset.getCategory()).thenReturn("HVAC");

        SlaPlan plan = service.plan(site, asset, Priority.CRITICAL, OffsetDateTime.parse("2026-10-09T10:00+05:30"));

        assertThat(plan.policyScope()).isEqualTo("CATEGORY+PRIORITY");
        assertThat(plan.responseMinutes()).isEqualTo(20);
        assertThat(plan.responseDueAt().toInstant()).isEqualTo(OffsetDateTime.parse("2026-10-09T10:20+05:30").toInstant());
        assertThat(plan.resolutionDueAt().toInstant()).isEqualTo(OffsetDateTime.parse("2026-10-09T13:00+05:30").toInstant());
        assertThat(plan.timeZone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void businessHoursPolicySkipsTheWeekendAndHolidaysInTheSiteTimeZone() {
        SlaPolicy high = policy(null, null, Priority.HIGH, 120, 480, true);
        lenient().when(policyRepository.findByOrganizationIdAndActiveTrue(orgId)).thenReturn(List.of(high));
        // Monday 12 Oct is a holiday
        lenient().when(holidayRepository.findHolidayDates(any(), any(), any(), any()))
                .thenReturn(List.of(LocalDate.of(2026, 10, 12)));

        // Friday 17:00 IST, expressed in UTC to prove the site zone (not UTC) drives the maths
        SlaPlan plan = service.plan(site, null, Priority.HIGH, OffsetDateTime.parse("2026-10-09T11:30Z"));

        // response 120 min: 60 Friday, then Mon holiday -> Tue 10:00
        assertThat(plan.responseDueAt().toInstant()).isEqualTo(OffsetDateTime.parse("2026-10-13T10:00+05:30").toInstant());
        // resolution 480 min: 60 Friday + 420 Tuesday -> Tue 16:00
        assertThat(plan.resolutionDueAt().toInstant()).isEqualTo(OffsetDateTime.parse("2026-10-13T16:00+05:30").toInstant());
        // at-risk = 75% of the window, measured in business time: 360 min -> Tue 14:00
        assertThat(plan.resolutionAtRiskAt().toInstant()).isEqualTo(OffsetDateTime.parse("2026-10-13T14:00+05:30").toInstant());
    }

    @Test
    void editingAPolicyAfterCreationNeverMovesAnExistingWorkOrder() {
        SlaPolicy policy = policy(null, null, Priority.HIGH, 60, 240, false);
        lenient().when(policyRepository.findByOrganizationIdAndActiveTrue(orgId)).thenReturn(List.of(policy));
        OffsetDateTime start = OffsetDateTime.parse("2026-10-09T10:00+05:30");

        SlaPlan plan = service.plan(site, null, Priority.HIGH, start);
        WorkOrder workOrder = new WorkOrder(orgId, "WO-1", "t", "d", Priority.HIGH, site, null, plan.resolutionDueAt());
        workOrder.applySla(plan);
        OffsetDateTime originalResponseDue = workOrder.getResponseDueAt();
        OffsetDateTime originalResolutionDue = workOrder.getTargetResolutionAt();

        // an admin tightens the policy to 5 / 10 minutes, and renames it
        policy.update("Tightened", null, null, Priority.HIGH, 5, 10, false, true);

        assertThat(workOrder.getSlaResponseMinutes()).isEqualTo(60);
        assertThat(workOrder.getSlaResolutionMinutes()).isEqualTo(240);
        assertThat(workOrder.getSlaPolicyName()).isEqualTo("policy");
        assertThat(workOrder.getResponseDueAt()).isEqualTo(originalResponseDue);
        assertThat(workOrder.getTargetResolutionAt()).isEqualTo(originalResolutionDue);

        // ...while a NEW work order picks up the new policy
        SlaPlan newer = service.plan(site, null, Priority.HIGH, start);
        assertThat(newer.responseMinutes()).isEqualTo(5);
    }

    @Test
    void invalidSiteTimeZoneIsReportedAsABusinessRuleError() {
        lenient().when(site.getTimeZone()).thenReturn("Mars/Olympus_Mons");
        lenient().when(policyRepository.findByOrganizationIdAndActiveTrue(orgId))
                .thenReturn(List.of(policy(null, null, null, 30, 60, false)));

        assertThatThrownBy(() -> service.plan(site, null, Priority.LOW, OffsetDateTime.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BLR-DC-01");
    }

    @Test
    void businessDaysAreParsedFromTheSiteCsv() {
        assertThat(SlaService.parseBusinessDays("MON, tue ,WED")).isEqualTo(
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY));
        assertThat(SlaService.parseBusinessDays("SATURDAY,SUN")).isEqualTo(
                EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        assertThatThrownBy(() -> SlaService.parseBusinessDays("MON,FUNDAY")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SlaService.parseBusinessDays("T")).isInstanceOf(IllegalArgumentException.class);
    }
}