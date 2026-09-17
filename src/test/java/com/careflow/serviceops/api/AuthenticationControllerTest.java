package com.careflow.serviceops.api;

import com.careflow.serviceops.domain.Role;
import com.careflow.serviceops.domain.User;
import com.careflow.serviceops.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthenticationControllerTest {

    private static final String KNOWN_HASH = "$2b$12$NELmcxdqqAZUr1GCGF8GLuTxNnayiN2GtWLUcfJXwSsG9TGtb6mHq";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private User activeUser(String role) {
        User user = new User();
        user.setEmail("planner.pat@careflow.local");
        user.setPassword(KNOWN_HASH);
        user.setEnabled(true);
        user.setRoles(Set.of(new Role(role)));
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    @Test
    void validCredentialsReturnAJwtAndTheUsersRoles() throws Exception {
        when(userRepository.findByEmail("planner.pat@careflow.local"))
                .thenReturn(Optional.of(activeUser("ROLE_PLANNER")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"planner.pat@careflow.local\",\"password\":\"Correct#123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("planner.pat@careflow.local"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_PLANNER"));
    }

    @Test
    void wrongPasswordIsRejectedWithAGenericMessage() throws Exception {
        when(userRepository.findByEmail("planner.pat@careflow.local"))
                .thenReturn(Optional.of(activeUser("ROLE_PLANNER")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"planner.pat@careflow.local\",\"password\":\"totally-wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource."));
    }

    @Test
    void unknownEmailGetsTheSameGenericMessageAsWrongPassword() throws Exception {
        when(userRepository.findByEmail("nobody@careflow.local")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"nobody@careflow.local\",\"password\":\"anything\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource."));
    }

    @Test
    void disabledAccountIsRejectedTheSameWayAsBadCredentials() throws Exception {
        User disabled = activeUser("ROLE_PLANNER");
        disabled.setEnabled(false);
        when(userRepository.findByEmail("planner.pat@careflow.local")).thenReturn(Optional.of(disabled));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"planner.pat@careflow.local\",\"password\":\"Correct#123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource."));
    }
}