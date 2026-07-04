# Context Query Simulator for the Roadwork Scenario

The rapid growth in Internet of Things (IoT) deployments has driven demand for
context-aware applications that respond to fast-changing real-world conditions.
Context Management Platforms (CMPs) integrate heterogeneous IoT sources and
serve inferred context to consumers, but their caching layer has to make
decisions on the fly under strict latency and freshness budgets. To evaluate
context-caching strategies at scale, researchers need query workloads that
resemble real deployments — but commercial CMP query logs are proprietary and
real-world deployments are expensive to reproduce.

This repository provides a **context query simulator for the smart-transport
roadwork scenario in Melbourne**, developed to drive the experimental
evaluation of ACOCA-P and ECCF in the PhD thesis
*Parameters Impacting Context Caching for Real-Time Context-Aware IoT
Applications* (Manchanda, Swinburne University of Technology, 2026).

The simulator generates realistic CDQL context queries against four active
roadwork sites (Hawthorn, Burwood, Malvern, and the Swanston/Collins CBD
site), replays them against a CoaaS-hosted CMP at the observed diurnal peak
of 85{,}692 queries per hour, and supports two high-volatility incident
regimes (lane closures, worker-on-road events) that stress the caching
layer's freshness and volatility handling.

## Publications supported

- IEEE Access 2024 — Adaptive Context Monitoring Framework
- MobiQuitous 2023 — A Hybrid Approach to Monitor Context Parameters
- IEEE CLOUD 2024 — Optimizing Context Caching Using a Hybrid Strategy
- IEEE/ACM CCGrid 2026 — Volatility-Aware Adaptive Context Caching (VACF)
- IEEE MDM 2026 — Adaptive Edge Context Caching Framework (ECCF, Best Paper)

## Generating the context query dataset

The generator runs in two stages:

1. **Template generation** — sample from the parameter distributions defined
   in `dataset/Context Query Templates.csv` to produce raw query records.
2. **CDQL transformation** — turn each template row into an executable CDQL
   query that CoaaS can dispatch to CAPME, CFMS, DCMF, VACF, or ECCF.

Both stages run via the Bash driver:

```bash
bash src/main/RoadworkQueryGenerator/queryGenerator.bash
```

Under the default configuration the generator produces roughly 280,000 queries
across a 24-hour window and completes in ~30 minutes on a MacBook Pro
(M-series CPU, 16 GB RAM). Larger runs (multi-day workloads, up to 900,000
queries) are supported through the `--horizon` flag.

## Executing the context query dataset

Once the dataset has been created, the replay server dispatches queries to
the CoaaS endpoint at their captured timestamps, so that inter-arrival
distributions and diurnal peaks are reproduced rather than uniformly sampled:

```bash
mvn package
java -jar target/roadwork-context-query-simulator-1.0.0.jar \
    --queries sample/diurnal-queries.json \
    --coaas   http://coaas:8080 \
    --pace    realtime
```

The `--pace` flag accepts `realtime`, `2x`, `10x`, or `burst` to compress or
expand the replay window against wall-clock time.

## Description of the template dataset

The template dataset is at `dataset/Context Query Templates.csv`. Each row
defines a query parameter, its type, whether it can appear conditionally,
and the distribution the generator samples from.

| Parameter | Description | Nullable |
| --- | --- | --- |
| vehicle_id | Vehicle identifier of the consumer requesting the roadwork context | False |
| location.lat | Latitude of the vehicle at the time of query execution | False |
| location.lng | Longitude of the vehicle at the time of query execution | False |
| target_site | Roadwork site whose context is being queried | False |
| max_delay_min | Maximum acceptable delay in minutes before a detour is preferred | True |
| min_freshness | Minimum acceptable context freshness (CF) in the range [0, 1] | True |
| min_qoc | Minimum acceptable Quality of Context aggregate | True |
| max_distance_m | Maximum radius around the vehicle to consider roadwork context for | True |
| route_preference | fastest, shortest, or scenic | True |
| context_type | roadwork, incident, weather, or aggregated | False |
| day | Day of the week that the query is executed | False |
| hour | Hour of day (0–23) that the query is executed | False |
| minute | Minute of hour (0–59) | False |
| second | Second of minute (0–59) | False |
| sla_ms | Consumer-side latency SLA in milliseconds | True |

## Sample context query loads

Three pre-generated CDQL query loads are checked in under `sample/` to make
the caching evaluations reproducible without re-running the generator:

- **`sample/monday-queries.json`** — one day (Monday) of realistic diurnal
  demand across the four roadwork sites, roughly 40,000 queries. Suited to
  smoke tests and CI runs.
- **`sample/incident-burst-queries.json`** — a 30-minute high-volatility
  incident burst injected at minute 8 of the trace (CVI 0.75–0.90) matching
  the incident regime used to evaluate VACF in Chapter 8 and ECCF in
  Chapter 10.
- **`sample/diurnal-queries.json`** — a 3.5-hour trace spanning the morning
  peak, an off-peak trough, and the evening peak, matching the diurnal
  experiment reported in Chapter 9.

### Sample Query 1 — freshness-constrained roadwork lookup

```
prefix schema:http//schema.org
pull (targetRoadwork.congestion, targetRoadwork.detour, targetRoadwork.time_delay)
when distance(consumerCar.location, targetRoadwork.location) <= {"value": 5, "unit":"km"}
define
  entity consumerCar    is from schema:Vehicle  where consumerCar.vin = "13UNVER82367G4",
  entity targetRoadwork is from coaas:Roadwork  where
      targetRoadwork.site_id = "hawthorn-glenferrie-rd"
      and targetRoadwork.freshness >= 0.38
      and targetRoadwork.time_delay < {"value": 15, "unit":"min"}
```

### Sample Query 2 — multi-attribute situational query with QoC

```
prefix schema:http//schema.org
push (targetRoadwork.*)
define
  entity consumerCar    is from schema:Vehicle  where consumerCar.vin = "13UNVER82367G4",
  entity targetWeather  is from schema:Thing    where targetWeather.location = "Melbourne, VIC",
  entity targetRoadwork is from coaas:Roadwork  where
      distance(targetRoadwork.location, consumerCar.location) <= {"value": 5, "unit":"km"}
      and targetRoadwork.freshness >= 0.38
      and targetRoadwork.qoc      >= 0.7
      and (
          targetRoadwork.congestion in ("moderate", "congested", "jammed")
          or targetRoadwork.incident != "clear"
      )
      and (targetWeather.condition != "heavy-rain" or targetRoadwork.qoc >= 0.9)
      and targetRoadwork.time_delay < {"value": 15, "unit":"min"}
      and targetRoadwork.sla_ms    <= 250
```

## Docker

The simulator is packaged as a Docker container that plays well with the
CoaaS deployment shipped in the ACOCA-P reference implementation:

```bash
docker build -t roadwork-context-query-simulator .
docker run --rm --network host \
    -e COAAS_URL=http://localhost:8080 \
    -e QUERY_LOAD=/data/diurnal-queries.json \
    -v $(pwd)/sample:/data \
    roadwork-context-query-simulator
```

## Related repositories

- [ashishm1999/ACOCA-P](https://github.com/ashishm1999/ACOCA-P) — the ACOCA-P
  reference implementation this simulator drives.
- [IBA-Group-IT/IoT-data-simulator](https://github.com/IBA-Group-IT/IoT-data-simulator) —
  the provider-side simulator used to replay the roadwork dataset.

## License

MIT.
