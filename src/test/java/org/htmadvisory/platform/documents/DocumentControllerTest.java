package org.htmadvisory.platform.documents;

import com.google.cloud.storage.Blob;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.htmadvisory.platform.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrivateDocumentCatalog catalog;

    @MockBean
    private DocumentDownloadTokenService tokenService;

    @MockBean
    private PrivateDocumentStorageService storageService;

    // Required so JwtAuthInterceptor (which guards request-download) can be
    // constructed within this web slice — see JwtAuthInterceptor's Javadoc
    // and the identical note in AuditControllerTest.
    @MockBean
    private JwtService jwtService;

    private static final PrivateDocument ARCH_SPEC = new PrivateDocument(
            "arch-spec", "HTM_Advisory_Architecture_Specification.docx",
            "Multi-Environment Architecture Specification",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final PrivateDocument SEO_GEO_BRIEF = new PrivateDocument(
            "seo-geo-brief", "HTM_Advisory_SEO_GEO_Brief.pdf",
            "Is Your Company Invisible to AI? SEO & GEO for CEOs",
            "application/pdf");

    // ── request-download (behind JwtAuthInterceptor) ───────────────────────

    @Test
    void requestDownload_shouldReturn401WithNoAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/documents/private/arch-spec/request-download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestDownload_shouldReturnDownloadUrlWhenAuthenticated() throws Exception {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.parseToken("valid-session-token")).thenReturn(claims);
        when(jwtService.extractUserId(claims)).thenReturn("user-1");
        when(jwtService.extractRole(claims)).thenReturn("MEMBER");

        when(catalog.findById("arch-spec")).thenReturn(Optional.of(ARCH_SPEC));
        when(tokenService.generateToken("user-1", "arch-spec")).thenReturn("minted-download-token");

        mockMvc.perform(post("/api/documents/private/arch-spec/request-download")
                        .header("Authorization", "Bearer valid-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value(
                        org.hamcrest.Matchers.containsString("token=minted-download-token")))
                .andExpect(jsonPath("$.title").value("Multi-Environment Architecture Specification"));
    }

    @Test
    void requestDownload_shouldReturn404ForUnknownDocId() throws Exception {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.parseToken("valid-session-token")).thenReturn(claims);
        when(jwtService.extractUserId(claims)).thenReturn("user-1");
        when(jwtService.extractRole(claims)).thenReturn("MEMBER");
        when(catalog.findById("not-real")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/documents/private/not-real/request-download")
                        .header("Authorization", "Bearer valid-session-token"))
                .andExpect(status().isNotFound());
    }

    // ── download (public — token itself is the credential) ─────────────────

    @Test
    void download_shouldStreamTheFileWhenTokenIsValid() throws Exception {
        when(tokenService.validateAndExtractDocId("good-token")).thenReturn("arch-spec");
        when(catalog.findById("arch-spec")).thenReturn(Optional.of(ARCH_SPEC));

        Blob blob = org.mockito.Mockito.mock(Blob.class);
        byte[] fakeFileBytes = "fake docx bytes".getBytes();
        doAnswer(invocation -> {
            java.io.OutputStream out = invocation.getArgument(0);
            out.write(fakeFileBytes);
            return null;
        }).when(blob).downloadTo(any(java.io.OutputStream.class));
        when(storageService.fetch("HTM_Advisory_Architecture_Specification.docx")).thenReturn(blob);

        mockMvc.perform(get("/api/documents/private/download").param("token", "good-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("HTM_Advisory_Architecture_Specification.docx")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(content().bytes(fakeFileBytes));
    }

    @Test
    void download_shouldUseInlineDispositionForPdfs() throws Exception {
        when(tokenService.validateAndExtractDocId("pdf-token")).thenReturn("seo-geo-brief");
        when(catalog.findById("seo-geo-brief")).thenReturn(Optional.of(SEO_GEO_BRIEF));

        Blob blob = org.mockito.Mockito.mock(Blob.class);
        byte[] fakePdfBytes = "fake pdf bytes".getBytes();
        doAnswer(invocation -> {
            java.io.OutputStream out = invocation.getArgument(0);
            out.write(fakePdfBytes);
            return null;
        }).when(blob).downloadTo(any(java.io.OutputStream.class));
        when(storageService.fetch("HTM_Advisory_SEO_GEO_Brief.pdf")).thenReturn(blob);

        mockMvc.perform(get("/api/documents/private/download").param("token", "pdf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.startsWith("inline")))
                .andExpect(content().bytes(fakePdfBytes));
    }

    @Test
    void download_shouldReturn401ForAnInvalidOrExpiredToken() throws Exception {
        when(tokenService.validateAndExtractDocId(anyString()))
                .thenThrow(new JwtException("expired"));

        mockMvc.perform(get("/api/documents/private/download").param("token", "bad-token"))
                .andExpect(status().isUnauthorized());
    }
}
