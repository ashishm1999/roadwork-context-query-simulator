package org.coaas.roadwork.jobs;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.coaas.roadwork.acoca.AcocaAdapter;
import org.coaas.roadwork.utils.HttpHandler;
import org.coaas.roadwork.utils.Message;
import org.coaas.roadwork.utils.Operation;
import org.coaas.roadwork.utils.pubsub.Publisher;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * Quartz job that fires one CDQL query at its scheduled instant. Publishes
 * a QUERY_SUBMITTED message on the topic bus and, if the ACOCA-P adapter
 * is enabled, immediately calls the four components in order.
 */
public class QueryJob implements Job {

    private static final Logger log = Logger.getLogger(QueryJob.class.getName());

    @Override
    public void execute(JobExecutionContext ctx) {
        RoadworkContextQuery q = (RoadworkContextQuery) ctx.getJobDetail().getJobDataMap().get("query");
        if (q == null) {
            log.warning("No query attached to job");
            return;
        }

        HttpHandler.makeContextQuery(q.getQuery(), q.getToken());

        Map<String, String> headers = new HashMap<>();
        headers.put("query_id", q.getQueryId());
        if (q.getTargetSite() != null) headers.put("target_site", q.getTargetSite());
        if (q.getSlaMs() != null) headers.put("sla_ms", q.getSlaMs().toString());

        Message message = new Message(Operation.QUERY_SUBMITTED, q.getQuery());
        for (Map.Entry<String, String> h : headers.entrySet()) message = message.withHeader(h.getKey(), h.getValue());
        Publisher.getInstance().publish("roadwork.query.submitted", message);

        if (AcocaAdapter.isEnabled()) {
            AcocaAdapter.getInstance().dispatch(q);
        }
    }
}
