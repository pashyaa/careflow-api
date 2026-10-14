package com.careflow.serviceops.service;

import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.SlaPolicy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** Picks the single most specific matching policy. See SlaPolicy for the precedence scores. */
public final class SlaPolicyResolver {

    private SlaPolicyResolver() {
    }

    public static Optional<SlaPolicy> select(Collection<SlaPolicy> candidates, UUID siteId,
                                             String assetCategory, Priority priority) {
        return candidates.stream()
                .filter(policy -> policy.matches(siteId, assetCategory, priority))
                .max(Comparator.comparingInt(SlaPolicy::specificity));
    }
}