package org.htmadvisory.platform.traffic;

/**
 * Result of a geolocation lookup for a given IP address.
 * Both fields are nullable — the lookup degrades gracefully for private/local
 * IPs, missing database files, or any lookup error.
 */
public class GeoLocationResult {

    private final String city;
    private final String serviceProvider;

    public GeoLocationResult(String city, String serviceProvider) {
        this.city = city;
        this.serviceProvider = serviceProvider;
    }

    public String getCity() {
        return city;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }
}
