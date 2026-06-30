package org.htmadvisory.platform.contact;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ContactRepository extends MongoRepository<ContactInquiry, String> {

    List<ContactInquiry> findByPersonId(String personId);

    List<ContactInquiry> findByEmail(String email);
}
