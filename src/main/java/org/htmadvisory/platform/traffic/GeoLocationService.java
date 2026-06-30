package org.htmadvisory.platform.traffic;

/**
 * Resolves IP addresses to approximate city and service-provider (ISP).
 *
 * Implementations must degrade gracefully: if the lookup fails for any reason
 * (private IP, missing database, network error), return a
 * {@link GeoLocationResult} with null fields rather than throwing — the Visit
 * write must never be blocked by a geolocation failure.
 */
public interface GeoLocationService {

    /**
     * Looks up city and service provider for the given IP address.
     *
     * @param ipAddress the raw IP address captured server-side
     * @return a result whose fields may be null if the lookup cannot resolve them
     */
    GeoLocationResult lookup(String ipAddress);
}
