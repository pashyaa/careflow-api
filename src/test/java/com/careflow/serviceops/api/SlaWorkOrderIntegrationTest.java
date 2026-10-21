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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
class SlaWorkOrderIntegrationTest {

    private static final String BLR_SITE = "10000000-0000-0000-0000-000000000001";
    private static final String HYD_SITE = "10000000-0000-0000-0000-000000000002";
    private static final String BLR_HVAC_ASSET = "20000000-0000-0000-0000-000000000001";

    @Autowired WebApplicationContext context;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private MockMvc mvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"planner.pat@careflow.local\",\"password\":\"Planner#2026\"}")).andReturn();
        token = json.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode create(String siteId, String assetId, String priority) throws Exception {
        String asset = assetId == null ? "" : ",\"assetId\":\"" + assetId + "\"";
        String body = "{\"title\":\"SLA test\",\"description\":\"Policy selection\",\"priority\":\"" + priority
                + "\",\"siteId\":\"" + siteId + "\"" + asset + "}";
        MvcResult result = mvc.perform(post("/api/v1/work-orders")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json").content(body)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return json.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void priorityOnlyPolicyIsSnapshottedAndTheDeadlineIsCalculated() throws Exception {
        JsonNode created = create(BLR_SITE, null, "HIGH");
        assertThat(created.at("/site/timeZone").asText()).isEqualTo("Asia/Kolkata");
        assertThat(created.at("/sla/policyScope").asText()).isEqualTo("PRIORITY");
        assertThat(created.at("/sla/responseMinutes").asInt()).isEqualTo(120);
        assertThat(created.at("/sla/timeZone").asText()).isEqualTo("Asia/Kolkata");
        assertThat(created.at("/sla/overallState").asText()).isEqualTo("ON_TRACK");
        assertThat(created.get("targetResolutionAt").asText()).isEqualTo(created.at("/sla/resolutionDueAt").asText());
    }

    @Test
    void sitePolicyBeatsPriorityPolicy() throws Exception {
        JsonNode created = create(HYD_SITE, null, "CRITICAL");
        assertThat(created.at("/sla/policyScope").asText()).isEqualTo("SITE+PRIORITY");
        assertThat(created.at("/sla/responseMinutes").asInt()).isEqualTo(15);
    }

    @Test
    void categoryPolicyAppliesThroughTheAsset() throws Exception {
        JsonNode created = create(BLR_SITE, BLR_HVAC_ASSET, "CRITICAL");
        assertThat(created.at("/sla/policyScope").asText()).isEqualTo("CATEGORY+PRIORITY");
        assertThat(created.at("/sla/responseMinutes").asInt()).isEqualTo(20);
    }

    @Test
    void dashboardExposesAtRiskAndBreachedCounts() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/dashboard/summary")
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode summary = json.readTree(result.getResponse().getContentAsString());
        assertThat(summary.has("atRiskWorkOrders")).isTrue();
        assertThat(summary.has("breachedWorkOrders")).isTrue();
        // seeded data includes open work orders past their target, so at least one breach exists
        assertThat(summary.get("breachedWorkOrders").asLong()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void previewShowsTheDeadlinesWithoutCreatingAnything() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/sla-policies/preview")
                .param("siteId", HYD_SITE).param("priority", "CRITICAL")
                .header("Authorization", "Bearer " + token)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode preview = json.readTree(result.getResponse().getContentAsString());
        assertThat(preview.get("responseMinutes").asInt()).isEqualTo(15);
    }
}