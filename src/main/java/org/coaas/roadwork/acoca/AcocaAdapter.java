package org.coaas.roadwork.acoca;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.coaas.roadwork.jobs.RoadworkContextQuery;
import org.coaas.roadwork.utils.Message;
import org.coaas.roadwork.utils.Operation;
import org.coaas.roadwork.utils.pubsub.Publisher;

/**
 * ACOCA-P adapter — the piece that wires the query simulator into the
 * four ACOCA-P microservices (CAPME, CFMS, DCMF, VACF).
 *
 * When enabled, every dispatched query fans out over four HTTP calls in
 * caching-lifecycle order:
 *
 *     CAPME (PoA) --> CFMS (CF) --> DCMF (decision) --> VACF (policy)
 *
 * Component failures do not block the query itself — the simulator
 * always continues, and the adapter publishes an ACOCA_ERROR message
 * on the topic bus.
 */
public final class AcocaAdapter {

    private static final Logger log = Logger.getLogger(AcocaAdapter.class.getName());
    private static final AcocaAdapter INSTANCE = new AcocaAdapter();

    private final CAPMEClient capme;
    private final CFMSClient cfms;
    private final DCMFClient dcmf;
    private final VACFClient vacf;
    private volatile boolean enabled;

    private AcocaAdapter() {
        this.capme = new CAPMEClient(env("CAPME_URL", "http://capme:8081"));
        this.cfms  = new CFMSClient(env("CFMS_URL",  "http://cfms:8082"));
        this.dcmf  = new DCMFClient(env("DCMF_URL",  "http://dcmf:8083"));
        this.vacf  = new VACFClient(env("VACF_URL",  "http://vacf:8084"));
        this.enabled = Boolean.parseBoolean(env("ACOCA_ADAPTER_ENABLED", "true"));
    }

    public static AcocaAdapter getInstance() {
        return INSTANCE;
    }

    public static boolean isEnabled() {
        return INSTANCE.enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    /**
     * Fire the four sequential calls asynchronously and publish the
     * combined decision on the topic bus.
     */
    public CompletableFuture<AcocaDecision> dispatch(RoadworkContextQuery q) {
        String itemId = q.getTargetSite() == null ? q.getQueryId() : q.getTargetSite();

        Map<String, Double> attributes = defaultAttributes();
        CompletableFuture<Double> poaFuture = capme.getPoAAsync(itemId, attributes)
            .exceptionally(err -> {
                log.fine("CAPME fallback: " + err.getMessage());
                return 0.5;
            });
        CompletableFuture<Double> cfFuture = cfms.getCfAsync(itemId)
            .exceptionally(err -> {
                log.fine("CFMS fallback: " + err.getMessage());
                return 0.0;
            });

        return poaFuture.thenCombine(cfFuture, (poa, cf) -> new double[]{poa, cf})
            .thenCompose(pair -> dcmf.decideAsync(itemId, pair[1], pair[0])
                .thenCombine(vacf.policyAsync(itemId, 1.0 - pair[1], pair[1], pair[0], q.getSlaMs() == null ? 250 : q.getSlaMs()),
                    (decision, policy) -> {
                        AcocaDecision d = new AcocaDecision(
                            itemId,
                            pair[0],
                            pair[1],
                            decision.cache,
                            decision.belief,
                            policy.ttlMs,
                            policy.replicas
                        );
                        Publisher.getInstance().publish("roadwork.acoca.decision",
                            new Message(Operation.DECISION_MADE, d.toJson()));
                        return d;
                    }))
            .exceptionally(err -> {
                Publisher.getInstance().publish("roadwork.acoca.error",
                    new Message(Operation.QUERY_ERROR, err.getMessage() == null ? "unknown" : err.getMessage()));
                return AcocaDecision.fallback(itemId);
            });
    }

    private static Map<String, Double> defaultAttributes() {
        Map<String, Double> a = new HashMap<>();
        a.put("freshness", 0.85);
        a.put("popularity", 0.60);
        a.put("recency", 0.90);
        a.put("cost", 1.20);
        a.put("latency", 0.05);
        a.put("quality", 0.95);
        return a;
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /** Read-through decision — the record published to the topic bus. */
    public static final class AcocaDecision {
        public final String itemId;
        public final double poa;
        public final double cf;
        public final boolean cache;
        public final double belief;
        public final long ttlMs;
        public final int replicas;

        public AcocaDecision(String itemId, double poa, double cf, boolean cache,
                             double belief, long ttlMs, int replicas) {
            this.itemId = itemId;
            this.poa = poa;
            this.cf = cf;
            this.cache = cache;
            this.belief = belief;
            this.ttlMs = ttlMs;
            this.replicas = replicas;
        }

        public String toJson() {
            return "{\"item_id\":\"" + itemId + "\"," +
                   "\"poa\":" + poa + "," +
                   "\"cf\":" + cf + "," +
                   "\"cache\":" + cache + "," +
                   "\"belief\":" + belief + "," +
                   "\"ttl_ms\":" + ttlMs + "," +
                   "\"replicas\":" + replicas + "}";
        }

        static AcocaDecision fallback(String itemId) {
            return new AcocaDecision(itemId, 0.5, 0.0, false, 0.0, 30_000L, 1);
        }
    }
}
