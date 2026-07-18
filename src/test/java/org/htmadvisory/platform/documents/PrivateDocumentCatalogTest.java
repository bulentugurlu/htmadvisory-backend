package org.htmadvisory.platform.documents;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrivateDocumentCatalogTest {

    private final PrivateDocumentCatalog catalog = new PrivateDocumentCatalog();

    @Test
    void findAll_shouldReturnAllEightKnownDocuments() {
        assertThat(catalog.findAll()).hasSize(8);
    }

    @Test
    void findById_shouldReturnEmptyForAnUnknownId() {
        assertThat(catalog.findById("not-a-real-doc")).isEmpty();
    }

    @Test
    void findById_shouldResolveEachKnownIdToItsRealGcsObjectName() {
        assertThat(catalog.findById("arch-spec")).get()
                .satisfies(d -> assertThat(d.gcsObjectName()).isEqualTo("HTM_Advisory_Architecture_Specification.pdf"));
        assertThat(catalog.findById("backend-whitepaper")).get()
                .satisfies(d -> assertThat(d.gcsObjectName()).isEqualTo("modern-backend-engineering-whitepaper.pdf"));
        assertThat(catalog.findById("seo-geo-brief")).get()
                .satisfies(d -> assertThat(d.gcsObjectName()).isEqualTo("HTM_Advisory_SEO_GEO_Brief.pdf"));
    }

    @Test
    void allDocuments_shouldHaveNonBlankIdObjectNameTitleAndContentType() {
        catalog.findAll().forEach(doc -> {
            assertThat(doc.id()).isNotBlank();
            assertThat(doc.gcsObjectName()).isNotBlank();
            assertThat(doc.title()).isNotBlank();
            assertThat(doc.contentType()).isNotBlank();
        });
    }
}
