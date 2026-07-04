"""Top-level Stage 1 driver — samples templates and writes them to CSV."""

from __future__ import annotations

import argparse
import csv
import random
from pathlib import Path

from configuration import Configuration
from consumers import ConsumerRegistry
from distribution import DiurnalProfile, jitter_submit_time
from query_temp import sample_template
from sites import SiteRegistry


HEADER = [
    "query_id","vehicle_id","location_lat","location_lng","target_site","context_type",
    "day","hour","minute","second","submit_at_ms","consumer_kind",
    "max_delay_min","min_freshness","min_qoc","max_distance_m","route_preference","sla_ms",
]


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--config", default="config.ini")
    ap.add_argument("--count", type=int, default=None)
    ap.add_argument("--horizon", type=float, default=None, help="hours")
    ap.add_argument("--seed", type=int, default=None)
    ap.add_argument("--output", type=str, required=True)
    args = ap.parse_args()

    cfg = Configuration(args.config)
    count = args.count or cfg.default_query_count
    horizon_hours = args.horizon or cfg.default_horizon_hours
    seed = args.seed if args.seed is not None else cfg.random_seed
    rng = random.Random(seed)

    site_registry = SiteRegistry.load(cfg.sites_json)
    consumer_registry = ConsumerRegistry.load(cfg.vin_registry_csv)
    profile = DiurnalProfile(**cfg.distribution_params())

    horizon_ms = int(horizon_hours * 3_600_000)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)

    print(f"Generating {count:,} templates across {horizon_hours:.1f}h (seed={seed})...")
    with output.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(HEADER)
        for i in range(count):
            base = int(i * horizon_ms / count)
            base = int(jitter_submit_time(profile, base, rng))
            base = max(0, base)
            site = site_registry.sample(rng)
            consumer = consumer_registry.sample_profile(rng)
            vin = consumer_registry.sample_vin(rng)
            template = sample_template(
                seq=i, base_ms=base, rng=rng,
                site_id=site.site_id, lat=site.lat, lng=site.lng,
                consumer_kind=consumer.kind,
                consumer_min_freshness=consumer.min_freshness,
                consumer_min_qoc=consumer.min_qoc,
                consumer_sla_ms=consumer.max_latency_ms,
                profile=profile,
            )
            template.vehicle_id = vin
            writer.writerow([
                template.query_id, template.vehicle_id,
                template.location_lat, template.location_lng,
                template.target_site, template.context_type,
                template.day, template.hour, template.minute, template.second,
                template.submit_at_ms, template.consumer_kind,
                template.max_delay_min or "", template.min_freshness or "",
                template.min_qoc or "", template.max_distance_m or "",
                template.route_preference or "", template.sla_ms or "",
            ])
            if (i + 1) % 20_000 == 0:
                print(f"... {i+1:,} rows")
    print(f"Wrote {count:,} rows to {output}")


if __name__ == "__main__":
    main()
