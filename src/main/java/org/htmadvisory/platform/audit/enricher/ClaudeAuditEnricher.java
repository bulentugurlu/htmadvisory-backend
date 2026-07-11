package org.htmadvisory.platform.audit.enricher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.htmadvisory.platform.audit.auditors.DimensionResult;
import org.htmadvisory.platform.audit.auditors.FindingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ClaudeAuditEnricher {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAuditEnricher.class);
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-6";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ANTHROPIC_API_KEY:}")
    private String apiKey;

    public ClaudeAuditEnricher(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public EnrichmentResult enrich(String url, String companyName, List<DimensionResult> dimensions) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ANTHROPIC_API_KEY not set — skipping Claude enrichment");
            return EnrichmentResult.empty();
        }

        try {
            String findingsSummary = buildFindingsSummary(dimensions);
            String prompt = buildPrompt(url, companyName, findingsSummary);

            Map<String, Object> requestBody = Map.of(
                    "model", MODEL,
                    "max_tokens", 2000,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    CLAUDE_API_URL, HttpMethod.POST, entity, String.class);

            return parseResponse(response.getBody());
        } catch (Exception e) {
            log.error("Claude enrichment failed: {}", e.getMessage());
            return EnrichmentResult.empty();
        }
    }

    private String buildFindingsSummary(List<DimensionResult> dimensions) {
        StringBuilder sb = new StringBuilder();
        for (DimensionResult dim : dimensions) {
            sb.append(dim.auditType()).append(" - ").append(dim.dimensionName())
              .append(": ").append(dim.score()).append("/100\n");
            for (FindingResult f : dim.findings()) {
                sb.append("  [").append(f.severity()).append("] ").append(f.finding()).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildPrompt(String url, String companyName, String findingsSummary) {
        return """
                You are an expert digital consultant advising a CEO.

                We audited the website: %s
                Company: %s

                Here are the raw technical findings:
                %s

                Your task:
                1. Write a 3-4 sentence executive summary a CEO can understand,
                   explaining what the overall findings mean for their business.
                   Focus on business impact, not technical jargon.

                2. For each finding marked HIGH or MEDIUM severity, write a
                   1-2 sentence recommendation in plain English that a CEO can
                   hand to their CTO or CIO. Start each with an action verb.
                   Format as bullet points.

                3. End with a 2-sentence closing on why fixing these issues matters
                   for competitive positioning and AI-era visibility.

                Respond in this exact JSON format:
                {
                  "executiveSummary": "...",
                  "recommendations": [
                    { "finding": "exact finding text", "recommendation": "..." }
                  ],
                  "closingStatement": "..."
                }
                """.formatted(url, companyName != null ? companyName : "Unknown", findingsSummary);
    }

    private EnrichmentResult parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        String content = root.path("content").get(0).path("text").asText();

        // Extract JSON from the response (Claude may wrap it in markdown code blocks)
        String json = content.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("```json\\n?", "").replaceFirst("\\n?```$", "").trim();
        }

        JsonNode parsed = objectMapper.readTree(json);
        String summary = parsed.path("executiveSummary").asText("");
        String closing = parsed.path("closingStatement").asText("");

        List<Map<String, String>> recs = new ArrayList<>();
        JsonNode recsNode = parsed.path("recommendations");
        if (recsNode.isArray()) {
            for (JsonNode rec : recsNode) {
                recs.add(Map.of(
                        "finding", rec.path("finding").asText(""),
                        "recommendation", rec.path("recommendation").asText("")
                ));
            }
        }

        return new EnrichmentResult(summary, recs, closing);
    }
}
