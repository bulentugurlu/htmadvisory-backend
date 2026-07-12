package org.htmadvisory.platform.audit;

import org.htmadvisory.platform.audit.model.Audit;
import org.htmadvisory.platform.audit.model.AuditStatus;
import org.htmadvisory.platform.audit.repository.AuditRepository;
import org.htmadvisory.platform.people.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;
    private final PersonService personService;
    private final AuditExecutor auditExecutor;

    public AuditService(AuditRepository auditRepository,
                        PersonService personService,
                        AuditExecutor auditExecutor) {
        this.auditRepository = auditRepository;
        this.personService = personService;
        this.auditExecutor = auditExecutor;
    }

    public AuditStartResponse startAudit(AuditRequest request) {
        // Resolve or create person identity
        String personId = null;
        if (request.getRequestedByEmail() != null && !request.getRequestedByEmail().isBlank()) {
            var person = personService.findOrCreateByEmail(request.getRequestedByEmail(), null);
            personId = person.getId();
            personService.recordEngagement(personId, "audit", "audit_requested",
                    Map.of("url", request.getUrl()));
        }

        String[] auditTypes = request.getAuditTypes() != null
                ? request.getAuditTypes().toArray(new String[0])
                : new String[]{"SEO", "ACCESSIBILITY"};

        Audit audit = new Audit(
                request.getUrl(), request.getCompanyName(),
                request.getRequestedByEmail(), personId, auditTypes);
        auditRepository.save(audit);

        // Fire async execution (separate bean — avoids @Async self-invocation proxy bypass)
        log.info("startAudit: saved audit={} id={}, calling auditExecutor.execute()",
                audit.getUrl(), audit.getId());
        auditExecutor.execute(audit.getId());
        log.info("startAudit: auditExecutor.execute() returned (should be immediate if @Async is working)");

        return new AuditStartResponse(
                audit.getId(),
                AuditStatus.RUNNING.name(),
                "Audit started — check /api/audits/" + audit.getId() + " for results");
    }

    @Transactional(readOnly = true)
    public Optional<Audit> findById(UUID id) {
        return auditRepository.findByIdWithDimensions(id)
                .map(audit -> {
                    // Force-initialize findings for each dimension while the transaction is open,
                    // preventing LazyInitializationException when the controller serializes the response.
                    audit.getDimensions().forEach(d -> d.getFindings().size());
                    return audit;
                });
    }

    @Transactional(readOnly = true)
    public List<Audit> findAll() {
        return auditRepository.findAllByOrderByCreatedAtDesc();
    }
}
