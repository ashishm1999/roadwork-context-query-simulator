package org.coaas.roadwork.config;

import java.util.List;

/**
 * The four Melbourne roadwork sites used across the ACOCA-P evaluation.
 * Coordinates match the physical camera locations described in the thesis.
 */
public final class Sites {

    public static final class Site {
        public final String siteId;
        public final String suburb;
        public final double lat;
        public final double lng;
        public final double weight;

        public Site(String siteId, String suburb, double lat, double lng, double weight) {
            this.siteId = siteId;
            this.suburb = suburb;
            this.lat = lat;
            this.lng = lng;
            this.weight = weight;
        }
    }

    public static final List<Site> ALL = List.of(
        new Site("hawthorn-glenferrie-rd",   "Hawthorn",     -37.8218, 145.0421, 0.30),
        new Site("burwood-highway-warrigal", "Burwood",      -37.8517, 145.1178, 0.25),
        new Site("malvern-dandenong-rd",     "Malvern",      -37.8697, 145.0479, 0.25),
        new Site("cbd-swanston-collins",     "Melbourne CBD", -37.8154, 144.9660, 0.20)
    );

    public static Site bySiteId(String siteId) {
        for (Site s : ALL) if (s.siteId.equals(siteId)) return s;
        throw new IllegalArgumentException("unknown site: " + siteId);
    }

    private Sites() {}
}
