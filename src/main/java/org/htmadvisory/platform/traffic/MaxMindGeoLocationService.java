package org.htmadvisory.platform.traffic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * MaxMind GeoLite2 implementation of {@link GeoLocationService}.
 *
 * <p><strong>Scaffold — not yet fully wired up.</strong> To enable geolocation:
 * <ol>
 *   <li>Create a free MaxMind account at maxmind.com</li>
 *   <li>Download {@code GeoLite2-City.mmdb} (for city) and
 *       {@code GeoLite2-ISP.mmdb} (for serviceProvider) — two separate databases,
 *       same account</li>
 *   <li>Deploy both {@code .mmdb} files alongside the app (Cloud Run deployment
 *       concern — paths must be readable at runtime)</li>
 *   <li>Configure the file paths via application properties and wire them into
 *       the {@code DatabaseReader} instances below</li>
 * </ol>
 *
 * <p>Until the {@code .mmdb} files are configured, this service degrades
 * gracefully: all lookups return a result with null city and null
 * serviceProvider, so {@link Visit} records are still written without geo data.
 *
 * <p><strong>Data freshness note:</strong> GeoLite2 databases go stale over
 * months. A periodic refresh process (e.g. a scheduled job that re-downloads
 * on a monthly basis) will eventually be needed — not required this pass.
 *
 * <p><strong>Privacy note:</strong> raw IP addresses and ISP data are personal
 * data under GDPR. A privacy notice, retention policy, and legal review are
 * open items before this domain goes live in production (flagged in
 * ARCHITECTURE.md, repeated here so it is not lost in the implementation).
 */
@Service
public class MaxMindGeoLocationService implements GeoLocationService {

    private static final Logger log = LoggerFactory.getLogger(MaxMindGeoLocationService.class);

    public MaxMindGeoLocationService() {
        log.warn("MaxMindGeoLocationService: GeoLite2 .mmdb files are not yet configured — " +
                 "city and serviceProvider will be null on all lookups. " +
                 "See class Javadoc for setup instructions.");
    }

    @Override
    public GeoLocationResult lookup(String ipAddress) {
        // TODO: instantiate MaxMind DatabaseReader instances pointing at
        // GeoLite2-City.mmdb and GeoLite2-ISP.mmdb (inject paths from config),
        // perform the lookup, and return a populated GeoLocationResult.
        // Until the databases are configured, degrade gracefully.
        return new GeoLocationResult(null, null);
    }
}
