package org.htmadvisory.platform.audit.repository;

import org.htmadvisory.platform.audit.model.AuditFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditFindingRepository extends JpaRepository<AuditFinding, UUID> {
    List<AuditFinding> findByDimensionId(UUID dimensionId);
}
