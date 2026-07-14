package org.htmadvisory.platform.auth.dto;

/**
 * Response payload for {@code POST /api/auth/register}. Deliberately
 * minimal (no token — registration does not log the user in, since the
 * account is PENDING until an admin approves it). The frontend uses
 * {@code status} to render the "pending approval" confirmation screen.
 */
public class RegisterResponse {

    private String id;
    private String email;
    private String status;
    private String message;

    public RegisterResponse(String id, String email, String status, String message) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
