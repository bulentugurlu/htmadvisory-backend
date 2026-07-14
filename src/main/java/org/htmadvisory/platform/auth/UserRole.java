package org.htmadvisory.platform.auth;

/**
 * A User's permission level within the member portal.
 *
 * <p>Not to be confused with {@code PersonProfile.role}, which stores the
 * person's job title at their company (e.g. "CEO") for firmographic
 * purposes. This enum is strictly about what the portal lets the account
 * do. Every account starts as {@code MEMBER}; {@code ADMIN} is granted
 * manually (there is no self-service upgrade path).
 */
public enum UserRole {
    MEMBER,
    ADMIN
}
