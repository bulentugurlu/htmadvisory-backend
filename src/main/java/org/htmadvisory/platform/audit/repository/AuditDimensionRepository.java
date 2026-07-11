package org.htmadvisory.platform.audit.repository;

import org.htmadvisory.platform.audit.model.AuditDimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditDimensionRepository extends JpaRepository<AuditDimension, UUID> {
    List<AuditDimension> findByAuditId(UUID auditId);
}
