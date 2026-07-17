package org.htmadvisory.platform.documents;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The fixed set of member-only documents, and what they map to in GCS.
 *
 * <p>A static in-memory catalog rather than a MongoDB-backed domain — this
 * is 8 known, rarely-changing documents, not user-generated content. If
 * that changes (documents added/removed often, non-engineers need to
 * manage the list, etc.), move this to its own collection following the
 * same pattern as every other domain here. Until then, a database round
 * trip for 8 static entries would be pure overhead.
 *
 * <p>{@code id} values here MUST match the {@code PRIVATE_DOCS} array in
 * the frontend's {@code Whitepapers.jsx} — that's what the frontend sends
 * when requesting a download link.
 */
@Component
public class PrivateDocumentCatalog {

    private static final Map<String, PrivateDocument> DOCUMENTS = buildCatalog();

    private static Map<String, PrivateDocument> buildCatalog() {
        Map<String, PrivateDocument> docs = new LinkedHashMap<>();

        String docx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String pptx = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        String pdf = "application/pdf";

        docs.put("arch-spec", new PrivateDocument(
                "arch-spec", "HTM_Advisory_Architecture_Specification.docx",
                "Multi-Environment Architecture Specification", docx));
        docs.put("arch-deck", new PrivateDocument(
                "arch-deck", "HTM_Advisory_Architecture_Presentation.pptx",
                "Multi-Environment Architecture Deck", pptx));
        docs.put("gcp-training", new PrivateDocument(
                "gcp-training", "HTM_Advisory_GCP_Deployment_Training.pptx",
                "GCP Deployment Training", pptx));
        docs.put("training-deck", new PrivateDocument(
                "training-deck", "HTM_Advisory_Training_Presentation.pptx",
                "AI Engineering Training Presentation", pptx));
        docs.put("backend-whitepaper", new PrivateDocument(
                "backend-whitepaper", "modern-backend-engineering-whitepaper.docx",
                "Modern Backend Engineering: CI/CD, Containerization & Cloud", docx));
        docs.put("backend-deck", new PrivateDocument(
                "backend-deck", "modern-backend-engineering.pptx",
                "Modern Backend Engineering Deck", pptx));
        docs.put("seo-geo-brief", new PrivateDocument(
                "seo-geo-brief", "HTM_Advisory_SEO_GEO_Brief.pdf",
                "Is Your Company Invisible to AI? SEO & GEO for CEOs", pdf));
        docs.put("seo-geo-deck", new PrivateDocument(
                "seo-geo-deck", "HTM_Advisory_SEO_GEO_Overview.pptx",
                "SEO & GEO Visibility — CEO Consulting Overview", pptx));

        return Collections.unmodifiableMap(docs);
    }

    public Optional<PrivateDocument> findById(String id) {
        return Optional.ofNullable(DOCUMENTS.get(id));
    }

    public Collection<PrivateDocument> findAll() {
        return DOCUMENTS.values();
    }
}
