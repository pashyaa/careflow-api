package com.careflow.serviceops.api;

import com.careflow.serviceops.api.dto.CreateWorkOrderRequest;
import com.careflow.serviceops.api.dto.PageResponse;
import com.careflow.serviceops.api.dto.WorkOrderResponse;
import com.careflow.serviceops.domain.Priority;
import com.careflow.serviceops.domain.Role;
import com.careflow.serviceops.domain.User;
import com.careflow.serviceops.domain.WorkOrderStatus;
import com.careflow.serviceops.repository.UserRepository;
import com.careflow.serviceops.security.JwtService;
import com.careflow.serviceops.service.WorkOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class WorkOrderControllerSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private WorkOrderService workOrderService;

    @MockitoBean
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private CreateWorkOrderRequest validCreateRequest() {
        return new CreateWorkOrderRequest(
                "HVAC unit not cooling",
                "Unit reports a high discharge temperature alarm.",
                Priority.HIGH,
                UUID.randomUUID(),
                null,
                OffsetDateTime.now().plusDays(1));
    }

    // --- Scenario 1: anonymous --------------------------------------------------

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/work-orders"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(workOrderService);
    }

    // --- Scenario 2: wrong role --------------------------------------------------

    @Test
    @WithMockUser(roles = "CUSTOMER_VIEWER")
    void customerViewerCannotCreateWorkOrders() throws Exception {
        mockMvc.perform(post("/api/v1/work-orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(workOrderService);
    }

    // --- Scenario 3: valid role (via @WithMockUser) -------------------------------

    @Test
    @WithMockUser(roles = "PLANNER")
    void plannerCanCreateWorkOrders() throws Exception {
        var request = validCreateRequest();
        var created = new WorkOrderResponse(
                UUID.randomUUID(), "WO-20261001-AAAA1111", request.title(), request.description(),
                request.priority(), WorkOrderStatus.NEW, null, null, null,
                request.targetResolutionAt(), null, OffsetDateTime.now(), OffsetDateTime.now(), 0);
        when(workOrderService.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/work-orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // --- Scenario 4: expired token, through the REAL filter ----------------------

    @Test
    void expiredTokenIsRejectedWithoutCrashing() throws Exception {
        String secret = (String) ReflectionTestUtils.getField(jwtService, "secret");
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String expiredToken = Jwts.builder()
                .subject("planner.pat@careflow.local")
                .issuedAt(new Date(System.currentTimeMillis() - 3_600_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    // --- Bonus: valid role through the REAL filter end-to-end ---------------------

    @Test
    void validTokenThroughTheRealFilterReachesTheController() throws Exception {
        User planner = new User();
        planner.setEmail("planner.pat@careflow.local");
        planner.setPassword("irrelevant-not-checked-here");
        planner.setEnabled(true);
        planner.setRoles(Set.of(new Role("ROLE_PLANNER")));
        when(userRepository.findByEmail("planner.pat@careflow.local")).thenReturn(Optional.of(planner));

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("planner.pat@careflow.local")
                .password("irrelevant")
                .authorities("ROLE_PLANNER")
                .build();
        String realToken = jwtService.generateToken(userDetails);

        when(workOrderService.findAll(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0, true, true));

        mockMvc.perform(get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + realToken))
                .andExpect(status().isOk());
    }
}