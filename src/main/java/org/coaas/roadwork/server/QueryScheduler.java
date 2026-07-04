package org.coaas.roadwork.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.coaas.roadwork.jobs.QueryJob;
import org.coaas.roadwork.jobs.RoadworkContextQuery;
import org.coaas.roadwork.model.CDQLQuery;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Wraps Quartz for the roadwork query replay pipeline. One Quartz job is
 * enqueued per CDQL query, fired at the wall-clock offset the query
 * declared in its {@code submit_at_ms} field.
 */
public final class QueryScheduler {

    private static final Logger log = Logger.getLogger(QueryScheduler.class.getName());
    private static final QueryScheduler INSTANCE = new QueryScheduler();
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Scheduler scheduler;
    private final AtomicInteger scheduled = new AtomicInteger();

    private QueryScheduler() {
        try {
            SchedulerFactory factory = new StdSchedulerFactory();
            this.scheduler = factory.getScheduler();
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Cannot initialise Quartz scheduler", ex);
        }
    }

    public static QueryScheduler getInstance() {
        return INSTANCE;
    }

    /** Load the queries from a CDQL JSON file and schedule each one. */
    public void fetchSchedule(Path queriesFile) {
        try {
            List<CDQLQuery> load = JSON.readValue(Files.newBufferedReader(queriesFile), new TypeReference<>() {});
            long startedAt = System.currentTimeMillis();
            for (CDQLQuery q : load) {
                RoadworkContextQuery roadwork = fromCDQL(q);
                enqueue(roadwork, startedAt + q.submitAtMs);
            }
            log.info("Scheduled " + scheduled.get() + " queries");
        } catch (Exception ex) {
            log.severe("Failed to load queries: " + ex.getMessage());
        }
    }

    private void enqueue(RoadworkContextQuery q, long fireAtMs) {
        JobDataMap data = new JobDataMap();
        data.put("query", q);
        JobDetail job = JobBuilder.newJob(QueryJob.class)
            .withIdentity("query-" + q.getQueryId(), "roadwork")
            .usingJobData(data)
            .storeDurably(false)
            .build();
        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity("trigger-" + q.getQueryId(), "roadwork")
            .startAt(new Date(fireAtMs))
            .withSchedule(SimpleScheduleBuilder.simpleSchedule())
            .build();
        try {
            scheduler.scheduleJob(job, trigger);
            scheduled.incrementAndGet();
        } catch (SchedulerException ex) {
            log.warning("Could not schedule job " + q.getQueryId() + ": " + ex.getMessage());
        }
    }

    public void start() throws SchedulerException {
        scheduler.start();
    }

    public void stop() throws SchedulerException {
        scheduler.shutdown(false);
    }

    public int scheduledCount() {
        return scheduled.get();
    }

    /** Extract enough state from a serialised CDQLQuery to fire the Quartz job. */
    static RoadworkContextQuery fromCDQL(CDQLQuery cdql) {
        String vin = cdql.meta == null ? "unknown" : cdql.meta.vehicleId;
        String site = cdql.meta == null ? null : cdql.meta.targetSite;
        int hour = cdql.meta == null ? 0 : cdql.meta.hour;
        int minute = cdql.meta == null ? 0 : cdql.meta.minute;
        int second = cdql.meta == null ? 0 : cdql.meta.second;
        String day = cdql.meta == null ? "monday" : cdql.meta.day;
        return new RoadworkContextQuery(day, hour, minute, second,
            cdql.cdql, cdql.queryId, "Bearer " + (vin == null ? "anon" : vin),
            site, cdql.slaMs);
    }
}
