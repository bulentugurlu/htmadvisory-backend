package org.htmadvisory.platform.people;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EngagementRepository extends MongoRepository<Engagement, String> {

    List<Engagement> findByPersonId(String personId);

}
