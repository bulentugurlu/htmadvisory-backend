package org.htmadvisory.platform.traffic;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface VisitRepository extends MongoRepository<Visit, String> {

    List<Visit> findBySessionId(String sessionId);

    List<Visit> findByPersonId(String personId);

    List<Visit> findByStartedAtBetween(Instant start, Instant end);
}
