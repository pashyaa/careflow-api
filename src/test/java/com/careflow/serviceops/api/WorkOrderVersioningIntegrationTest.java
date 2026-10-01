package com.careflow.serviceops.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class WorkOrderVersioningIntegrationTest {

    private static final String SITE_ID = "10000000-0000-0000-0000-000000000001";
    private static final String TECH_1 = "30000000-0000-0000-0000-000000000001";
    private static final String TECH_2 = "30000000-0000-0000-0000-000000000002";

    @Autowired WebApplicationContext context;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private MockMvc mvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"planner.pat@careflow.local\",\"password\":\"Planner#2026\"}"))
                .andReturn();
        token = json.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }

    private String createWorkOrder() throws Exception {
        String body = """
                {"title":"Version test","description":"Concurrency check","priority":"HIGH",
                 "siteId":"%s","targetResolutionAt":"2030-01-01T10:00:00Z"}""".formatted(SITE_ID);
        MvcResult result = mvc.perform(post("/api/v1/work-orders")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json").content(body)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private MvcResult assign(String id, String technicianId, String ifMatch) throws Exception {
        var req = patch("/api/v1/work-orders/" + id + "/assignment")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"technicianId\":\"" + technicianId + "\"}");
        if (ifMatch != null) req.header("If-Match", ifMatch);
        return mvc.perform(req).andReturn();
    }

    private JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    @Test
    void missingIfMatchIsRejectedWith428() throws Exception {
        String id = createWorkOrder();
        assertThat(assign(id, TECH_1, null).getResponse().getStatus()).isEqualTo(428);
    }

    @Test
    void secondSessionUsingTheSameVersionGets409WithCurrentVersion() throws Exception {
        String id = createWorkOrder();                       // version 0

        MvcResult first = assign(id, TECH_1, "\"0\"");       // session A
        assertThat(first.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(first).get("version").asLong()).isEqualTo(1);
        assertThat(first.getResponse().getHeader("ETag")).isEqualTo("\"1\"");

        MvcResult second = assign(id, TECH_2, "\"0\"");      // session B, stale
        assertThat(second.getResponse().getStatus()).isEqualTo(409);
        JsonNode problem = body(second);
        assertThat(problem.get("code").asText()).isEqualTo("VERSION_CONFLICT");
        assertThat(problem.get("currentVersion").asLong()).isEqualTo(1);

        // Retrying with the current version succeeds
        assertThat(assign(id, TECH_2, "\"1\"").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void staleStatusTransitionAlsoReturns409() throws Exception {
        String id = createWorkOrder();
        assign(id, TECH_1, "\"0\"");                         // now version 1, ASSIGNED
        MvcResult stale = mvc.perform(patch("/api/v1/work-orders/" + id + "/status")
                .header("Authorization", "Bearer " + token)
                .header("If-Match", "\"0\"")
                .contentType("application/json")
                .content("{\"status\":\"CANCELLED\"}")).andReturn();
        assertThat(stale.getResponse().getStatus()).isEqualTo(409);
        assertThat(body(stale).get("currentVersion").asLong()).isEqualTo(1);
    }

    @Test
    void truelyConcurrentMutationsFromTheSameVersionYieldOneWinnerAndOneConflict() throws Exception {
        String id = createWorkOrder();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Future<MvcResult>> futures = new ArrayList<>();
        for (String tech : List.of(TECH_1, TECH_2)) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return assign(id, tech, "\"0\"");
            }));
        }
        ready.await();
        go.countDown();

        List<Integer> statuses = new ArrayList<>();
        for (Future<MvcResult> f : futures) {
            MvcResult r = f.get(15, TimeUnit.SECONDS);
            statuses.add(r.getResponse().getStatus());
            if (r.getResponse().getStatus() == 409) {
                assertThat(body(r).get("currentVersion").asLong()).isEqualTo(1);
            }
        }
        pool.shutdown();

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
    }
}