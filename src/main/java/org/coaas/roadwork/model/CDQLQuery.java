package org.coaas.roadwork.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One executable CDQL query, ready to be dispatched to CoaaS.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CDQLQuery {
    @JsonProperty("query_id")     public String queryId;
    @JsonProperty("submit_at_ms") public long submitAtMs;
    @JsonProperty("sla_ms")       public int slaMs;
    @JsonProperty("cdql")         public String cdql;
    @JsonProperty("meta")         public QueryTemplate meta;

    public CDQLQuery() {}

    public CDQLQuery(String queryId, long submitAtMs, int slaMs, String cdql, QueryTemplate meta) {
        this.queryId = queryId;
        this.submitAtMs = submitAtMs;
        this.slaMs = slaMs;
        this.cdql = cdql;
        this.meta = meta;
    }
}
