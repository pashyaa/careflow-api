package com.careflow.serviceops.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the identity to attribute a mutation to, from the authenticated principal
 * on the current request. This is the ONLY supported source of "who did this" for
 * audit purposes, and (CF-102) the ONLY supported source of "which tenant this
 * request belongs to" — callers must never accept an actor identity or an
 * organization id from request bodies, query params, or headers, since those can be
 * spoofed by the client.
 */
@Component
public class CurrentActorResolver {

    public Actor resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            // Endpoints that call this are all annotated @PreAuthorize on an authenticated
            // rule, so Spring Security should already have rejected the request before
            // this is ever reached. This is a defensive backstop, not a path we expect
            // to exercise.
            throw new IllegalStateException("No authenticated tenant principal is available for this request.");
        }

        return new Actor(principal.getUserId(), principal.getDisplayName(), principal.getOrganizationId());
    }
}