package org.coaas.roadwork.jobs;

import java.util.Objects;

/**
 * One scheduled context query — the analogue of Shakthi's ContextQuery
 * adapted for the roadwork scenario. Carries the CDQL body, an auth
 * token, and the calendar coordinates the scheduler uses.
 */
public final class RoadworkContextQuery {

    public final String day;
    public final int hour;
    public final int minute;
    public final int second;

    private final String query;
    private final String queryId;
    private final String authToken;
    private final String targetSite;
    private final Integer slaMs;

    public RoadworkContextQuery(String day, int hour, int minute, int second,
                                String query, String queryId, String authToken,
                                String targetSite, Integer slaMs) {
        this.day = Objects.requireNonNull(day);
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.query = Objects.requireNonNull(query);
        this.queryId = Objects.requireNonNull(queryId);
        this.authToken = authToken;
        this.targetSite = targetSite;
        this.slaMs = slaMs;
    }

    public String getQuery() { return query; }
    public String getQueryId() { return queryId; }
    public String getToken() { return authToken; }
    public String getTargetSite() { return targetSite; }
    public Integer getSlaMs() { return slaMs; }

    /** True if this query should have already fired at wall-clock offset {@code offsetSec}. */
    public boolean fires(int offsetSec) {
        int firesAt = second + 60 * minute + 3600 * hour;
        return firesAt <= offsetSec;
    }

    @Override
    public String toString() {
        return "RoadworkContextQuery{" +
               "id=" + queryId +
               ", when=" + day + " " + hour + ":" + minute + ":" + second +
               ", site=" + targetSite +
               '}';
    }
}
