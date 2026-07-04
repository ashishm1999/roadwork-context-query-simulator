package org.coaas.roadwork.jobs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.coaas.roadwork.model.CDQLQuery;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * Loads the next batch of CDQL queries from disk. Runs on Shakthi's
 * periodic "fetch" cadence so the scheduler never needs the whole trace
 * resident in memory during long replays.
 */
public class QueryFetchJob implements Job {

    private static final Logger log = Logger.getLogger(QueryFetchJob.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public void execute(JobExecutionContext ctx) {
        Path source = (Path) ctx.getJobDetail().getJobDataMap().get("source");
        if (source == null || !Files.exists(source)) {
            log.warning("No source path for QueryFetchJob: " + source);
            return;
        }
        try {
            List<CDQLQuery> load = JSON.readValue(source.toFile(), new TypeReference<>() {});
            log.info("Loaded " + load.size() + " queries from " + source.getFileName());
            ctx.getJobDetail().getJobDataMap().put("loaded", load);
        } catch (Exception ex) {
            log.warning("QueryFetchJob failed: " + ex.getMessage());
        }
    }
}
