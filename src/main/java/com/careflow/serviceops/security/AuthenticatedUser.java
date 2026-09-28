package com.careflow.serviceops.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * The authenticated principal placed on the SecurityContext by JwtAuthenticationFilter.
 * Extends Spring Security's own UserDetails.User (for password/authorities plumbing)
 * with the fields CareFlow needs on every request: the user's id, display name, and —
 * critically for CF-102 — the tenant (organization) they belong to.
 *
 * CustomUserDetailsService is the ONLY place this is constructed, always from the
 * User entity loaded from the database. There is no client-controlled path to set
 * organizationId: it can never come from a request body, header, or the JWT payload.
 */
public class AuthenticatedUser extends User {

    private final UUID userId;
    private final String displayName;
    private final UUID organizationId;

    public AuthenticatedUser(String email, String password, boolean enabled,
                             Collection<? extends GrantedAuthority> authorities,
                             UUID userId, String displayName, UUID organizationId) {
        super(email, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.displayName = displayName;
        this.organizationId = organizationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }
}