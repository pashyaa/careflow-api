package com.careflow.serviceops.service;

import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.SlaPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SlaPolicyResolverTest {

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID SITE_A = UUID.randomUUID();
    private static final UUID SITE_B = UUID.randomUUID();

    private static SlaPolicy policy(String name, UUID siteId, String category, Priority priority) {
        return new SlaPolicy(ORG, name, siteId, category, priority, 30, 240, false, true);
    }

    private static String pick(List<SlaPolicy> policies, UUID site, String category, Priority priority) {
        return SlaPolicyResolver.select(policies, site, category, priority).map(SlaPolicy::getName).orElse(null);
    }

    @Test
    void tenantFallbackAppliesWhenNothingMoreSpecificMatches() {
        var policies = List.of(policy("fallback", null, null, null), policy("critical", null, null, Priority.CRITICAL));
        assertThat(pick(policies, SITE_A, null, Priority.LOW)).isEqualTo("fallback");
    }

    @Test
    void priorityBeatsTenantFallback() {
        var policies = List.of(policy("fallback", null, null, null), policy("critical", null, null, Priority.CRITICAL));
        assertThat(pick(policies, SITE_A, null, Priority.CRITICAL)).isEqualTo("critical");
    }

    @Test
    void categoryBeatsPriorityOnly() {
        var policies = List.of(policy("critical", null, null, Priority.CRITICAL), policy("hvac", null, "HVAC", null));
        assertThat(pick(policies, SITE_A, "HVAC", Priority.CRITICAL)).isEqualTo("hvac");
    }

    @Test
    void siteBeatsCategory() {
        var policies = List.of(policy("hvac", null, "HVAC", null), policy("site", SITE_A, null, null));
        assertThat(pick(policies, SITE_A, "HVAC", Priority.HIGH)).isEqualTo("site");
    }

    @Test
    void sitePlusPriorityBeatsCategoryPlusPriority() {
        var policies = List.of(
                policy("category+priority", null, "HVAC", Priority.CRITICAL),
                policy("site+priority", SITE_A, null, Priority.CRITICAL));
        assertThat(pick(policies, SITE_A, "HVAC", Priority.CRITICAL)).isEqualTo("site+priority");
    }

    @Test
    void fullyScopedPolicyBeatsEverything() {
        var policies = List.of(
                policy("site+priority", SITE_A, null, Priority.CRITICAL),
                policy("everything", SITE_A, "HVAC", Priority.CRITICAL),
                policy("fallback", null, null, null));
        assertThat(pick(policies, SITE_A, "HVAC", Priority.CRITICAL)).isEqualTo("everything");
    }

    @Test
    void policyForAnotherSiteIsIgnored() {
        var policies = List.of(policy("site B", SITE_B, null, Priority.CRITICAL), policy("fallback", null, null, null));
        assertThat(pick(policies, SITE_A, null, Priority.CRITICAL)).isEqualTo("fallback");
    }

    @Test
    void categoryPoliciesNeverMatchWorkWithoutAnAsset() {
        var policies = List.of(policy("hvac", null, "HVAC", null), policy("fallback", null, null, null));
        assertThat(pick(policies, SITE_A, null, Priority.HIGH)).isEqualTo("fallback");
    }

    @Test
    void categoryMatchingIgnoresCase() {
        assertThat(pick(List.of(policy("hvac", null, "hvac", null)), SITE_A, "HVAC", Priority.LOW)).isEqualTo("hvac");
    }

    @Test
    void inactivePoliciesAreIgnored() {
        var inactive = new SlaPolicy(ORG, "old", null, null, Priority.CRITICAL, 5, 10, false, false);
        assertThat(pick(List.of(inactive), SITE_A, null, Priority.CRITICAL)).isNull();
    }

    @Test
    void noMatchingPolicyYieldsEmpty() {
        assertThat(SlaPolicyResolver.select(List.of(), SITE_A, null, Priority.LOW)).isEmpty();
    }

    @Test
    void scopeLabelsDescribeThePinnedDimensions() {
        assertThat(policy("a", null, null, null).scopeLabel()).isEqualTo("TENANT_DEFAULT");
        assertThat(policy("b", SITE_A, null, Priority.HIGH).scopeLabel()).isEqualTo("SITE+PRIORITY");
        assertThat(policy("c", SITE_A, "HVAC", Priority.HIGH).scopeLabel()).isEqualTo("SITE+CATEGORY+PRIORITY");
    }
}