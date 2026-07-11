package org.htmadvisory.platform.audit;

import jakarta.validation.Valid;
import org.htmadvisory.platform.audit.model.Audit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audits")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AuditStartResponse runAudit(@RequestBody @Valid AuditRequest request) {
        return auditService.startAudit(request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditResponse> getAudit(@PathVariable UUID id) {
        return auditService.findById(id)
                .map(audit -> ResponseEntity.ok(AuditResponse.from(audit)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<AuditResponse> listAudits() {
        return auditService.findAll().stream()
                .map(AuditResponse::summary)
                .toList();
    }
}
