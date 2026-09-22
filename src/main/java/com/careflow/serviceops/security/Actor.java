package com.careflow.serviceops.security;

import java.util.UUID;

/**
 * The authenticated identity attributed to a mutation, resolved server-side from the
 * current security context — never accepted as client input. See CurrentActorResolver.
 */
public record Actor(UUID userId, String displayName) {
}