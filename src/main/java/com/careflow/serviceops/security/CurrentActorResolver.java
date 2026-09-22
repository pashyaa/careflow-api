package com.careflow.serviceops.security;

import com.careflow.serviceops.domain.User;
import com.careflow.serviceops.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the identity to attribute a mutation to, from the authenticated principal
 * on the current request. This is the ONLY supported source of "who did this" for
 * audit purposes — callers must never accept an actor identity from request bodies,
 * query params, or headers, since those can be spoofed by the client.
 */
@Component
public class CurrentActorResolver {

    private final UserRepository userRepository;

    public CurrentActorResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Actor resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            // Endpoints that record audit history are all annotated @PreAuthorize on an
            // authenticated rule, so Spring Security should already have rejected the
            // request before this is ever reached. This is a defensive backstop, not a
            // path we expect to exercise.
            throw new IllegalStateException("No authenticated principal is available to attribute this change to.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal '" + email + "' does not correspond to a known user."));

        return new Actor(user.getId(), user.getDisplayName());
    }
}