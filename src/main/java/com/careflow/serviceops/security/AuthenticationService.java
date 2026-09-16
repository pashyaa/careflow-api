package com.careflow.serviceops.security;

import com.careflow.serviceops.api.dto.LoginRequest;
import com.careflow.serviceops.api.dto.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails user =
                userDetailsService.loadUserByUsername(request.getEmail());

        String token = jwtService.generateToken(user);

        // No /auth/me endpoint exists, so this login response is the one and only place
        // roles are ever handed to the frontend — it persists them locally alongside the
        // token to drive role-aware navigation and route guards.
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new LoginResponse(token, user.getUsername(), roles);
    }
}