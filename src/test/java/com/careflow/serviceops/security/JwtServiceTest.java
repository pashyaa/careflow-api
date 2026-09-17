package com.careflow.serviceops.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-only-secret-value-needs-32-chars-min";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // JwtService's secret/expiration come from @Value on application.yml properties,
        // which aren't resolved outside a Spring context — set them directly for a fast,
        // dependency-free unit test.
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 900_000L); // 15 minutes
    }

    private UserDetails testUser(String email, String role) {
        return User.withUsername(email)
                .password("irrelevant-for-this-test")
                .authorities(role)
                .build();
    }

    @Test
    void generatedTokenRoundTripsToTheSameUsername() {
        UserDetails user = testUser("planner.pat@careflow.local", "ROLE_PLANNER");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("planner.pat@careflow.local");
    }

    @Test
    void isTokenValidReturnsTrueForAFreshTokenMatchingTheSameUser() {
        UserDetails user = testUser("tech.jamie@careflow.local", "ROLE_TECHNICIAN");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValidReturnsFalseWhenTheTokenBelongsToADifferentUser() {
        UserDetails owner = testUser("planner.pat@careflow.local", "ROLE_PLANNER");
        UserDetails someoneElse = testUser("viewer.morgan@careflow.local", "ROLE_CUSTOMER_VIEWER");

        String token = jwtService.generateToken(owner);

        assertThat(jwtService.isTokenValid(token, someoneElse)).isFalse();
    }

    @Test
    void expiredTokenThrowsWhenParsed() {
        // This is exactly the situation JwtAuthenticationFilter's try/catch has to handle
        // gracefully — an expired token must never reach the controller layer.
        ReflectionTestUtils.setField(jwtService, "expiration", -5_000L); // already expired
        UserDetails user = testUser("admin.riley@careflow.local", "ROLE_ADMIN");

        String expiredToken = jwtService.generateToken(user);

        assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedTokenSignatureIsRejected() {
        UserDetails user = testUser("planner.pat@careflow.local", "ROLE_PLANNER");
        String token = jwtService.generateToken(user);

        // Flip a character in the signature (the part after the last '.') to simulate
        // a forged/corrupted token.
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(JwtException.class);
    }
}