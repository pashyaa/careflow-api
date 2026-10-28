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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** BUG-305: HTTP-level checks for required resolution / cancellation evidence. */
@SpringBootTest
class ReasonCodeIntegrationTest {

    private static final String SITE_ID = "10000000-0000-0000-0000-000000000001";
    private static final String TECH_1 = "30000000-0000-0000-0000-000000000001";

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

    // ---- helpers -------------------------------------------------------------------------

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private long versionOf(String id) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/work-orders/" + id)
                .header("Authorization", "Bearer " + token)).andReturn();
        return body(result).get("version").asLong();
    }

    private String statusOf(String id) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/work-orders/" + id)
                .header("Authorization", "Bearer " + token)).andReturn();
        return body(result).get("status").asText();
    }

    /** Creates a work order and assigns a technician, leaving it in ASSIGNED. */
    private String assignedWorkOrder() throws Exception {
        String create = """
                {"title":"Reason code test","description":"BUG-305 evidence check","priority":"HIGH","siteId":"%s"}"""
                .formatted(SITE_ID);
        MvcResult created = mvc.perform(post("/api/v1/work-orders")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json").content(create)).andReturn();
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        String id = body(created).get("id").asText();

        MvcResult assigned = mvc.perform(patch("/api/v1/work-orders/" + id + "/assignment")
                .header("Authorization", "Bearer " + token)
                .header("If-Match", "\"0\"")
                .contentType("application/json")
                .content("{\"technicianId\":\"" + TECH_1 + "\"}")).andReturn();
        assertThat(assigned.getResponse().getStatus()).isEqualTo(200);
        return id;
    }

    private MvcResult changeStatus(String id, String requestBody) throws Exception {
        return mvc.perform(patch("/api/v1/work-orders/" + id + "/status")
                .header("Authorization", "Bearer " + token)
                .header("If-Match", "\"" + versionOf(id) + "\"")
                .contentType("application/json")
                .content(requestBody)).andReturn();
    }

    private JsonNode lastHistoryEntry(String id) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/work-orders/" + id + "/history")
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode history = body(result);
        return history.get(history.size() - 1);
    }

    // ---- cancelling ----------------------------------------------------------------------

    @Test
    void cancellingWithBlankEvidenceReturns400WithFieldErrorsAndLeavesStatusUnchanged() throws Exception {
        String id = assignedWorkOrder();

        MvcResult result = changeStatus(id, "{\"status\":\"CANCELLED\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode errors = body(result).get("errors");
        assertThat(errors.has("reasonCode")).isTrue();
        assertThat(errors.has("note")).isTrue();
        assertThat(statusOf(id)).isEqualTo("ASSIGNED");
    }

    @Test
    void cancellingWithWhitespaceOnlyNoteIsRejected() throws Exception {
        String id = assignedWorkOrder();

        MvcResult result = changeStatus(id,
                "{\"status\":\"CANCELLED\",\"note\":\"      \",\"reasonCode\":\"DUPLICATE\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode errors = body(result).get("errors");
        assertThat(errors.has("note")).isTrue();
        assertThat(errors.has("reasonCode")).isFalse();
        assertThat(statusOf(id)).isEqualTo("ASSIGNED");
    }

    @Test
    void cancellingWithUnknownReasonCodeIsRejected() throws Exception {
        String id = assignedWorkOrder();

        MvcResult result = changeStatus(id,
                "{\"status\":\"CANCELLED\",\"note\":\"Raised against the wrong site\",\"reasonCode\":\"NOT_A_CODE\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(result).get("errors").has("reasonCode")).isTrue();
    }

    @Test
    void validCancellationSucceedsAndHistoryStoresReasonAndNote() throws Exception {
        String id = assignedWorkOrder();

        MvcResult result = changeStatus(id,
                "{\"status\":\"CANCELLED\",\"note\":\"Duplicate of WO-20261001-ABCD1234\",\"reasonCode\":\"DUPLICATE\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(result).get("status").asText()).isEqualTo("CANCELLED");

        JsonNode entry = lastHistoryEntry(id);
        assertThat(entry.get("toStatus").asText()).isEqualTo("CANCELLED");
        assertThat(entry.get("reasonCode").asText()).isEqualTo("DUPLICATE");
        assertThat(entry.get("reasonLabel").asText()).isEqualTo("Duplicate work order");
        assertThat(entry.get("note").asText()).isEqualTo("Duplicate of WO-20261001-ABCD1234");
    }

    // ---- resolving -----------------------------------------------------------------------

    @Test
    void resolvingRequiresEvidenceAndStoresItWhenValid() throws Exception {
        String id = assignedWorkOrder();
        assertThat(changeStatus(id, "{\"status\":\"IN_PROGRESS\"}").getResponse().getStatus()).isEqualTo(200);

        // blank evidence
        MvcResult blank = changeStatus(id, "{\"status\":\"RESOLVED\"}");
        assertThat(blank.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(blank).get("errors").has("reasonCode")).isTrue();
        assertThat(body(blank).get("errors").has("note")).isTrue();

        // whitespace-only note
        MvcResult whitespace = changeStatus(id,
                "{\"status\":\"RESOLVED\",\"note\":\"     \",\"reasonCode\":\"FIXED\"}");
        assertThat(whitespace.getResponse().getStatus()).isEqualTo(400);

        // a cancellation code is not a valid resolution code
        MvcResult wrongCategory = changeStatus(id,
                "{\"status\":\"RESOLVED\",\"note\":\"Replaced the pressure sensor\",\"reasonCode\":\"DUPLICATE\"}");
        assertThat(wrongCategory.getResponse().getStatus()).isEqualTo(400);
        assertThat(statusOf(id)).isEqualTo("IN_PROGRESS");

        // valid evidence
        MvcResult valid = changeStatus(id,
                "{\"status\":\"RESOLVED\",\"note\":\"Replaced the pressure sensor and re-tested.\",\"reasonCode\":\"FIXED\"}");
        assertThat(valid.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(valid).get("status").asText()).isEqualTo("RESOLVED");

        JsonNode entry = lastHistoryEntry(id);
        assertThat(entry.get("reasonCode").asText()).isEqualTo("FIXED");
        assertThat(entry.get("reasonLabel").asText()).isEqualTo("Fault repaired");
    }

    // ---- non-terminal transitions --------------------------------------------------------

    @Test
    void reasonCodeIsRejectedOnNonTerminalTransitionsButOptionalNoteStillWorks() throws Exception {
        String id = assignedWorkOrder();

        MvcResult withCode = changeStatus(id, "{\"status\":\"IN_PROGRESS\",\"reasonCode\":\"FIXED\"}");
        assertThat(withCode.getResponse().getStatus()).isEqualTo(400);

        MvcResult withoutCode = changeStatus(id, "{\"status\":\"IN_PROGRESS\"}");
        assertThat(withoutCode.getResponse().getStatus()).isEqualTo(200);
    }

    // ---- reference endpoint --------------------------------------------------------------

    @Test
    void reasonCodesEndpointReturnsOnlyCodesForTheRequestedStatus() throws Exception {
        MvcResult resolved = mvc.perform(get("/api/v1/reference/reason-codes").param("status", "RESOLVED")
                .header("Authorization", "Bearer " + token)).andReturn();
        assertThat(resolved.getResponse().getStatus()).isEqualTo(200);
        assertThat(resolved.getResponse().getContentAsString()).contains("\"FIXED\"").doesNotContain("\"DUPLICATE\"");

        MvcResult cancelled = mvc.perform(get("/api/v1/reference/reason-codes").param("status", "CANCELLED")
                .header("Authorization", "Bearer " + token)).andReturn();
        assertThat(cancelled.getResponse().getStatus()).isEqualTo(200);
        assertThat(cancelled.getResponse().getContentAsString()).contains("\"DUPLICATE\"").doesNotContain("\"FIXED\"");
    }

    @Test
    void reasonCodesEndpointRejectsNonTerminalStatuses() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/reference/reason-codes").param("status", "ASSIGNED")
                .header("Authorization", "Bearer " + token)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }
}