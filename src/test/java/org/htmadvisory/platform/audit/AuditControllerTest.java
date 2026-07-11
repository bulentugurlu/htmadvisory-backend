package org.htmadvisory.platform.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditService auditService;

    @Test
    void runAudit_returns202_withAuditId() throws Exception {
        UUID auditId = UUID.randomUUID();
        when(auditService.startAudit(any())).thenReturn(
                new AuditStartResponse(auditId, "RUNNING",
                        "Audit started — check /api/audits/" + auditId + " for results"));

        Map<String, Object> request = Map.of(
                "url", "https://acmecorp.com",
                "companyName", "Acme",
                "requestedByEmail", "test@example.com",
                "auditTypes", List.of("SEO"));

        mockMvc.perform(post("/api/audits/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.auditId").value(auditId.toString()))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void runAudit_returns400_whenUrlMissing() throws Exception {
        Map<String, Object> request = Map.of(
                "companyName", "Acme",
                "auditTypes", List.of("SEO"));

        mockMvc.perform(post("/api/audits/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAudit_returns404_whenNotFound() throws Exception {
        when(auditService.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/audits/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listAudits_returnsEmptyList() throws Exception {
        when(auditService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
