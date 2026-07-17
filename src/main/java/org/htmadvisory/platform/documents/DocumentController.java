package org.htmadvisory.platform.documents;

import com.google.cloud.storage.Blob;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

/**
 * Delivers member-only documents without ever exposing a permanent public
 * link. Two endpoints, two very different trust models:
 *
 * <ul>
 *   <li>{@code POST /api/documents/private/{docId}/request-download} — behind
 *       {@code JwtAuthInterceptor} (see {@code WebMvcConfig}), so this only
 *       runs for an authenticated, approved member. Mints a short-lived
 *       token and hands back a URL — never the file itself.</li>
 *   <li>{@code GET /api/documents/private/download} — deliberately public.
 *       This is what actually gets clicked, typically from an email, days
 *       or minutes later, with no Authorization header available at all.
 *       The token in the query string IS the credential here; {@link
 *       DocumentDownloadTokenService} validates it in place of a session
 *       check.</li>
 * </ul>
 *
 * <p>Neither endpoint accepts a path, filename, or bucket object name from
 * the caller — {@code docId} only ever resolves through {@link
 * PrivateDocumentCatalog}. That's deliberate: accepting a raw object name
 * here would let a request be crafted to fetch anything in the bucket.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final PrivateDocumentCatalog catalog;
    private final DocumentDownloadTokenService tokenService;
    private final PrivateDocumentStorageService storageService;
    private final String backendBaseUrl;

    public DocumentController(PrivateDocumentCatalog catalog,
                               DocumentDownloadTokenService tokenService,
                               PrivateDocumentStorageService storageService,
                               @Value("${htm.documents.backend-base-url}") String backendBaseUrl) {
        this.catalog = catalog;
        this.tokenService = tokenService;
        this.storageService = storageService;
        this.backendBaseUrl = backendBaseUrl;
    }

    @PostMapping("/private/{docId}/request-download")
    public Map<String, String> requestDownload(@PathVariable String docId, HttpServletRequest request) {
        PrivateDocument doc = catalog.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown document: " + docId));

        // Set by JwtAuthInterceptor after validating the caller's session
        // token — this endpoint never touches that token itself.
        String userId = (String) request.getAttribute("userId");

        String downloadToken = tokenService.generateToken(userId, doc.id());
        String downloadUrl = backendBaseUrl + "/api/documents/private/download?token=" + downloadToken;

        return Map.of("downloadUrl", downloadUrl, "title", doc.title());
    }

    @GetMapping("/private/download")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String token) {
        String docId;
        try {
            docId = tokenService.validateAndExtractDocId(token);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "This download link is invalid or has expired. Please request the document again.");
        }

        PrivateDocument doc = catalog.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown document"));

        Blob blob = storageService.fetch(doc.gcsObjectName());

        StreamingResponseBody body = outputStream -> blob.downloadTo(outputStream);

        // PDFs get a real in-tab preview. Everything else (.docx, .pptx)
        // still forces a download — browsers don't render Office formats
        // natively, so "inline" for those would either fail silently or
        // trigger a confusing "how do you want to open this" prompt
        // instead of the clean download experience "attachment" gives.
        String dispositionType = "application/pdf".equals(doc.contentType()) ? "inline" : "attachment";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + doc.gcsObjectName() + "\"")
                .body(body);
    }
}
