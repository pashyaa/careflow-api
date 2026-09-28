package com.careflow.serviceops.security;

import java.util.UUID;

/**
 * The authenticated identity attributed to a mutation, resolved server-side from the
 * current security context — never accepted as client input. See CurrentActorResolver.
 * CF-102: also carries the actor's tenant (organizationId), since every mutation this
 * actor performs must be scoped to their own organization.
 */
public record Actor(UUID userId, String displayName, UUID organizationId) {
}