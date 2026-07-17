package org.htmadvisory.platform.consent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.htmadvisory.platform.auth.JwtService;
import org.htmadvisory.platform.people.Person;
import org.htmadvisory.platform.people.PersonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsentController.class)
class ConsentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsentService consentService;

    @MockBean
    private PersonService personService;

    // Required so JwtAuthInterceptor can be constructed within this web
    // slice — see JwtAuthInterceptor's Javadoc and the identical note in
    // AuditControllerTest / DocumentControllerTest.
    @MockBean
    private JwtService jwtService;

    @Test
    void emailOptIn_shouldReturn201AndRecordBothConsentTypes() throws Exception {
        Person person = new Person("visitor@example.com", null, Instant.now(), Instant.now());
        person.setId("person-1");
        when(personService.findOrCreateByEmail(eq("visitor@example.com"), isNull())).thenReturn(person);

        String body = """
                {
                  "email": "visitor@example.com",
                  "consentCommunications": true,
                  "consentMarketing": true,
                  "source": "whitepaper-download"
                }
                """;

        mockMvc.perform(post("/api/consent/email-opt-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(consentService).recordConsent("person-1", "communications", true, "whitepaper-download");
        verify(consentService).recordConsent("person-1", "marketing", true, "whitepaper-download");
    }

    @Test
    void emailOptIn_shouldRecordMarketingAsFalseWhenNotOptedIn() throws Exception {
        Person person = new Person("visitor2@example.com", null, Instant.now(), Instant.now());
        person.setId("person-2");
        when(personService.findOrCreateByEmail(eq("visitor2@example.com"), isNull())).thenReturn(person);

        String body = """
                {
                  "email": "visitor2@example.com",
                  "consentCommunications": true,
                  "consentMarketing": false,
                  "source": "whitepaper-download"
                }
                """;

        mockMvc.perform(post("/api/consent/email-opt-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(consentService).recordConsent("person-2", "marketing", false, "whitepaper-download");
    }

    @Test
    void emailOptIn_shouldReturn400ForMissingEmail() throws Exception {
        String body = """
                {
                  "consentCommunications": true,
                  "consentMarketing": false,
                  "source": "whitepaper-download"
                }
                """;

        mockMvc.perform(post("/api/consent/email-opt-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailOptIn_shouldReturn400ForInvalidEmail() throws Exception {
        String body = """
                {
                  "email": "not-an-email",
                  "consentCommunications": true,
                  "consentMarketing": false,
                  "source": "whitepaper-download"
                }
                """;

        mockMvc.perform(post("/api/consent/email-opt-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
