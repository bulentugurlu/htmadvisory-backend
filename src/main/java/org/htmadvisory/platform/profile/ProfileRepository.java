package org.htmadvisory.platform.profile;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProfileRepository extends MongoRepository<PersonProfile, String> {

    Optional<PersonProfile> findByPersonId(String personId);

}
