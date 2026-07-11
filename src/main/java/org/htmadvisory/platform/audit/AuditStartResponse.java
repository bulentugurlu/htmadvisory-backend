package org.htmadvisory.platform.audit;

import java.util.UUID;

public record AuditStartResponse(UUID auditId, String status, String message) {}
