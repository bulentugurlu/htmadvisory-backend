package org.htmadvisory.platform.auth;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPersonId(String personId);

    List<User> findByStatus(UserStatus status);
}
