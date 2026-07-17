package org.htmadvisory.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound payload for {@code POST /api/auth/register}.
 *
 * <p>{@code company} and {@code title} are optional — same relaxed
 * convention as {@code ContactInquiryRequest.company} — but strongly
 * encouraged since this is a CEO-facing member portal and admins use them
 * to decide who to approve.
 *
 * <p>{@code consentMarketing} is the ONLY consent captured here that's a
 * real choice — communications consent itself isn't a separate field
 * because it's implied by completing registration at all (the frontend
 * makes that checkbox mandatory to submit). Both get recorded as separate
 * {@code ConsentRecord} entries in {@code UserService.register()} — see
 * that method for why consent lives in its own domain rather than as
 * fields on User.
 */
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    private String company;

    private String title;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private boolean consentMarketing;

    public RegisterRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isConsentMarketing() {
        return consentMarketing;
    }

    public void setConsentMarketing(boolean consentMarketing) {
        this.consentMarketing = consentMarketing;
    }
}
