package com.careflow.serviceops.security;

import com.careflow.serviceops.domain.User;
import com.careflow.serviceops.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        java.util.List<org.springframework.security.core.GrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> (org.springframework.security.core.GrantedAuthority)
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName()))
                .toList();

        // CF-102: organizationId travels on the principal from here on — every
        // downstream authenticated call reads the tenant from this object, never
        // from client input.
        return new AuthenticatedUser(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                authorities,
                user.getId(),
                user.getDisplayName(),
                user.getOrganizationId()
        );
    }
}