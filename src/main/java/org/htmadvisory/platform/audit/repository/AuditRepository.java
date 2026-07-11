package org.htmadvisory.platform.audit.repository;

import org.htmadvisory.platform.audit.model.Audit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditRepository extends JpaRepository<Audit, UUID> {
    List<Audit> findAllByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT a FROM Audit a LEFT JOIN FETCH a.dimensions WHERE a.id = :id")
    Optional<Audit> findByIdWithDimensions(@Param("id") UUID id);
}
