package org.htmadvisory.platform.people;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PersonRepository extends MongoRepository<Person, String> {

    Optional<Person> findByEmail(String email);

}
