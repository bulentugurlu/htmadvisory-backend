package org.htmadvisory.platform.auth;

/**
 * Where a User account sits in the manual-approval workflow.
 *
 * <p>Every account is created as {@code PENDING} by {@code POST
 * /api/auth/register}. An admin must explicitly move it to {@code APPROVED}
 * (via {@code POST /api/admin/users/{id}/approve}) before the account can
 * log in. {@code REJECTED} is available for admins who want to record a
 * declined application rather than leaving it PENDING indefinitely, but no
 * endpoint sets it yet — see CLAUDE.md build order for when that's added.
 */
public enum UserStatus {
    PENDING,
    APPROVED,
    REJECTED
}
