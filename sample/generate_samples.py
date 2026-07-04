#!/usr/bin/env python3
"""Produce the three checked-in sample query loads under sample/.

Each JSON file contains a compact array of CDQL query objects matching the
schema emitted by src/main/java/org/coaas/roadwork/generator/CDQLTransformer.java.
The generator here is a Python analogue used only to seed the checked-in
samples so a reviewer can eyeball the load without cloning the Java
generator, running Maven, and re-running the two-stage pipeline.
"""

from __future__ import annotations

import argparse
import json
import math
import random
import string
from pathlib import Path


SITES = [
    ("hawthorn-glenferrie-rd",   -37.8218, 145.0421, 0.30),
    ("burwood-highway-warrigal", -37.8517, 145.1178, 0.25),
    ("malvern-dandenong-rd",     -37.8697, 145.0479, 0.25),
    ("cbd-swanston-collins",     -37.8154, 144.9660, 0.20),
]

CONTEXT_TYPES = ("roadwork", "incident", "weather", "aggregated")
CONTEXT_WEIGHTS = (0.55, 0.20, 0.15, 0.10)


def pick(values, weights, rng: random.Random):
    r = rng.random()
    acc = 0.0
    for v, w in zip(values, weights):
        acc += w
        if r <= acc:
            return v
    return values[-1]


def diurnal_multiplier(hour: float) -> float:
    morning = math.exp(-0.5 * ((hour - 7.5) / 1.2) ** 2)
    evening = math.exp(-0.5 * ((hour - 17.5) / 1.4) ** 2)
    return 0.15 + 0.85 * (morning + evening)


def random_vin(rng: random.Random) -> str:
    alpha = string.ascii_uppercase.replace("I", "").replace("O", "").replace("Q", "") + string.digits
    return "".join(rng.choice(alpha) for _ in range(17))


def render_cdql(t: dict) -> str:
    parts = [
        "prefix schema:http//schema.org",
        "pull (targetRoadwork.congestion, targetRoadwork.detour, targetRoadwork.time_delay)",
        f'when distance(consumerCar.location, targetRoadwork.location) <= {{"value": {t.get("max_distance_m", 5000)}, "unit":"m"}}',
        "define",
        f'  entity consumerCar    is from schema:Vehicle  where consumerCar.vin = "{t["vehicle_id"]}",',
        f'  entity targetRoadwork is from coaas:Roadwork  where',
        f'      targetRoadwork.site_id = "{t["target_site"]}"',
    ]
    if t.get("min_freshness") is not None:
        parts.append(f'      and targetRoadwork.freshness >= {t["min_freshness"]:.2f}')
    if t.get("min_qoc") is not None:
        parts.append(f'      and targetRoadwork.qoc >= {t["min_qoc"]:.2f}')
    if t.get("max_delay_min") is not None:
        parts.append(f'      and targetRoadwork.time_delay < {{"value": {t["max_delay_min"]}, "unit":"min"}}')
    if t.get("sla_ms") is not None:
        parts.append(f'      and targetRoadwork.sla_ms <= {t["sla_ms"]}')
    return "\n".join(parts)


def sample_query(rng: random.Random, seq: int, submit_at_ms: int) -> dict:
    site_id, lat, lng, _ = pick(SITES, [w for *_, w in SITES], rng)
    t = {
        "vehicle_id":    random_vin(rng),
        "location_lat":  round(lat + rng.gauss(0, 0.01), 6),
        "location_lng":  round(lng + rng.gauss(0, 0.015), 6),
        "target_site":   site_id,
        "context_type":  pick(CONTEXT_TYPES, CONTEXT_WEIGHTS, rng),
        "submit_at_ms":  submit_at_ms,
    }
    if rng.random() < 0.85:
        t["max_delay_min"] = 2 + rng.randint(0, 17)
    if rng.random() < 0.75:
        t["min_freshness"] = round(0.20 + rng.random() * 0.60, 2)
    if rng.random() < 0.35:
        t["min_qoc"] = round(0.50 + rng.random() * 0.45, 2)
    if rng.random() < 0.55:
        t["max_distance_m"] = pick([500, 1000, 2500, 5000, 10000], [0.25, 0.20, 0.20, 0.20, 0.15], rng)
    if rng.random() < 0.60:
        t["route_preference"] = pick(["fastest", "shortest", "scenic"], [0.65, 0.25, 0.10], rng)
    if rng.random() < 0.85:
        t["sla_ms"] = pick([100, 250, 500, 1000], [0.30, 0.45, 0.20, 0.05], rng)

    hour = (submit_at_ms // 3_600_000) % 24
    t["hour"] = int(hour)
    t["minute"] = int((submit_at_ms % 3_600_000) // 60_000)
    t["second"] = int((submit_at_ms % 60_000) // 1000)
    t["day"] = "monday"
    return {
        "query_id":     f"q-{seq:09d}",
        "submit_at_ms": submit_at_ms,
        "sla_ms":       t.get("sla_ms", 250),
        "cdql":         render_cdql(t),
        "meta":         t,
    }


def build(n: int, horizon_hours: float, seed: int, out_path: Path, weight_by_diurnal: bool = True) -> None:
    rng = random.Random(seed)
    horizon_ms = int(horizon_hours * 3_600_000)
    load = []
    for i in range(n):
        base = int(i * horizon_ms / n)
        if weight_by_diurnal:
            hour = (base / 3_600_000) % 24
            jitter = int(rng.gauss(0.0, 5000) * (1.0 - min(0.9, diurnal_multiplier(hour))))
        else:
            jitter = int(rng.gauss(0.0, 500))
        load.append(sample_query(rng, i, max(0, base + jitter)))
    out_path.write_text(json.dumps(load, separators=(",", ":")))
    size_kb = out_path.stat().st_size / 1024
    print(f"{out_path}: {n:,} queries, {size_kb:,.1f} KB")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--out-dir", type=str, default=".")
    args = ap.parse_args()

    out = Path(args.out_dir)
    out.mkdir(parents=True, exist_ok=True)

    # 3 checked-in sample files:
    #   monday-queries: one weekday, low volume, 8,000 queries
    #   incident-burst-queries: 30-minute high-volatility burst, 2,000 queries
    #   diurnal-queries: 3.5-hour trace across two peaks, 15,000 queries
    build(8_000, 24.0, seed=20260703, out_path=out / "monday-queries.json", weight_by_diurnal=True)
    build(2_000,  0.5, seed=20260704, out_path=out / "incident-burst-queries.json", weight_by_diurnal=False)
    build(15_000, 3.5, seed=20260705, out_path=out / "diurnal-queries.json", weight_by_diurnal=True)


if __name__ == "__main__":
    main()
