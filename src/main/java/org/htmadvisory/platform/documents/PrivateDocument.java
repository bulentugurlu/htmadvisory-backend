package org.htmadvisory.platform.documents;

/**
 * A member-only document, as known to the backend.
 *
 * <p>{@code gcsObjectName} is the exact object name in the private bucket
 * (see {@code htm.documents.private-bucket}) — no folder prefix, since that
 * bucket holds nothing else. This is the ONLY place that name is allowed to
 * come from; {@link DocumentController} never accepts a path or filename
 * from the caller, precisely so a request can't be crafted to fetch an
 * arbitrary object from the bucket.
 */
public record PrivateDocument(String id, String gcsObjectName, String title, String contentType) {
}
