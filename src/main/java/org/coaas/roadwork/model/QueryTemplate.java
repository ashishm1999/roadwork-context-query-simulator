package org.coaas.roadwork.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One materialised roadwork context query template.
 *
 * The generator samples every field from the distribution defined in
 * {@code dataset/Context Query Templates.csv}; the transformer then folds
 * this record into an executable CDQL query.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryTemplate {

    @JsonProperty("vehicle_id")     public String vehicleId;
    @JsonProperty("location_lat")   public double locationLat;
    @JsonProperty("location_lng")   public double locationLng;
    @JsonProperty("target_site")    public String targetSite;
    @JsonProperty("max_delay_min")  public Integer maxDelayMin;
    @JsonProperty("min_freshness")  public Double minFreshness;
    @JsonProperty("min_qoc")        public Double minQoc;
    @JsonProperty("max_distance_m") public Integer maxDistanceM;
    @JsonProperty("route_preference") public String routePreference;
    @JsonProperty("context_type")   public String contextType;
    @JsonProperty("day")            public String day;
    @JsonProperty("hour")           public int hour;
    @JsonProperty("minute")         public int minute;
    @JsonProperty("second")         public int second;
    @JsonProperty("sla_ms")         public Integer slaMs;

    /** Wall-clock offset from the workload start in milliseconds. */
    @JsonProperty("submit_at_ms")   public long submitAtMs;
}
