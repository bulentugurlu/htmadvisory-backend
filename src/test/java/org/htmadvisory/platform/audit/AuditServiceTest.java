package org.htmadvisory.platform.audit;

import org.htmadvisory.platform.audit.model.Audit;
import org.htmadvisory.platform.audit.model.AuditStatus;
import org.htmadvisory.platform.audit.repository.AuditRepository;
import org.htmadvisory.platform.people.Person;
import org.htmadvisory.platform.people.PersonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private PersonService personService;

    @Mock
    private AuditExecutor auditExecutor;

    @InjectMocks
    private AuditService auditService;

    @Test
    void startAudit_returnsRunningStatus() {
        Person person = new Person("bulent@htmadvisory.org", "Bulent");
        when(personService.findOrCreateByEmail(anyString(), any())).thenReturn(person);
        UUID generatedId = UUID.randomUUID();
        when(auditRepository.save(any())).thenAnswer(inv -> {
            Audit audit = inv.getArgument(0);
            ReflectionTestUtils.setField(audit, "id", generatedId);
            return audit;
        });

        AuditRequest request = new AuditRequest();
        request.setUrl("https://acmecorp.com");
        request.setCompanyName("Acme");
        request.setRequestedByEmail("bulent@htmadvisory.org");
        request.setAuditTypes(List.of("SEO"));

        AuditStartResponse response = auditService.startAudit(request);

        assertThat(response.status()).isEqualTo("RUNNING");
        assertThat(response.auditId()).isNotNull();
        verify(auditExecutor).execute(any(UUID.class));
    }

    @Test
    void startAudit_recordsEngagement_whenEmailProvided() {
        Person person = new Person("bulent@htmadvisory.org", "Bulent");
        when(personService.findOrCreateByEmail(anyString(), any())).thenReturn(person);
        when(auditRepository.save(any())).thenAnswer(inv -> {
            Audit audit = inv.getArgument(0);
            ReflectionTestUtils.setField(audit, "id", UUID.randomUUID());
            return audit;
        });

        AuditRequest request = new AuditRequest();
        request.setUrl("https://acmecorp.com");
        request.setRequestedByEmail("bulent@htmadvisory.org");
        request.setAuditTypes(List.of("SEO"));

        auditService.startAudit(request);

        verify(personService).recordEngagement(any(), eq("audit"), eq("audit_requested"), anyMap());
    }

    @Test
    void startAudit_skipsPersonResolution_whenNoEmail() {
        when(auditRepository.save(any())).thenAnswer(inv -> {
            Audit audit = inv.getArgument(0);
            ReflectionTestUtils.setField(audit, "id", UUID.randomUUID());
            return audit;
        });

        AuditRequest request = new AuditRequest();
        request.setUrl("https://acmecorp.com");
        request.setAuditTypes(List.of("SEO"));

        auditService.startAudit(request);

        verifyNoInteractions(personService);
    }
}
