"""Query template data model + sampler.

The template is the Stage 1 output of the generator (see queryGenerator.bash).
Stage 2 folds each template into an executable CDQL query.
"""

from __future__ import annotations

import random
from dataclasses import dataclass, field, asdict
from typing import Any

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from constants import (
    CONTEXT_TYPES, FRESHNESS_MIN, FRESHNESS_MAX, QOC_MIN, QOC_MAX,
    SLA_BUCKETS, SLA_WEIGHTS,
)
from distribution import DiurnalProfile, choice_weighted, sample_diurnal_hour


@dataclass
class QueryTemplate:
    query_id: str
    vehicle_id: str
    location_lat: float
    location_lng: float
    target_site: str
    context_type: str
    day: str
    hour: int
    minute: int
    second: int
    submit_at_ms: int
    consumer_kind: str
    max_delay_min: int | None = None
    min_freshness: float | None = None
    min_qoc: float | None = None
    max_distance_m: int | None = None
    route_preference: str | None = None
    sla_ms: int | None = None
    meta: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


def sample_template(seq: int, base_ms: int, rng: random.Random,
                    site_id: str, lat: float, lng: float,
                    consumer_kind: str, consumer_min_freshness: float,
                    consumer_min_qoc: float, consumer_sla_ms: int,
                    profile: DiurnalProfile) -> QueryTemplate:
    """Materialise one query template from the sampled site + consumer."""
    context_type = choice_weighted(CONTEXT_TYPES, (0.55, 0.20, 0.15, 0.10), rng)

    max_delay = rng.randint(2, 20) if rng.random() < 0.85 else None
    min_freshness = round(rng.uniform(FRESHNESS_MIN, FRESHNESS_MAX), 2) if rng.random() < 0.75 else consumer_min_freshness
    min_qoc = round(rng.uniform(QOC_MIN, QOC_MAX), 2) if rng.random() < 0.35 else consumer_min_qoc
    max_distance = choice_weighted(("500", "1000", "2500", "5000", "10000"),
                                   (0.25, 0.20, 0.20, 0.20, 0.15), rng) if rng.random() < 0.55 else None
    route_pref = choice_weighted(("fastest", "shortest", "scenic"),
                                 (0.65, 0.25, 0.10), rng) if rng.random() < 0.60 else None
    sla_ms = choice_weighted(tuple(str(x) for x in SLA_BUCKETS), SLA_WEIGHTS, rng) if rng.random() < 0.85 else str(consumer_sla_ms)

    hour_of_day = int((base_ms / 3_600_000.0) % 24)
    in_hour_ms = base_ms % 3_600_000
    minute = int(in_hour_ms // 60_000)
    second = int((in_hour_ms % 60_000) // 1000)
    day = ("monday","tuesday","wednesday","thursday","friday","saturday","sunday")[(base_ms // 86_400_000) % 7]

    return QueryTemplate(
        query_id=f"q-{seq:09d}",
        vehicle_id="",  # caller fills the VIN
        location_lat=round(lat + rng.gauss(0, 0.01), 6),
        location_lng=round(lng + rng.gauss(0, 0.015), 6),
        target_site=site_id,
        context_type=context_type,
        day=day,
        hour=hour_of_day,
        minute=minute,
        second=second,
        submit_at_ms=int(base_ms),
        consumer_kind=consumer_kind,
        max_delay_min=max_delay,
        min_freshness=min_freshness,
        min_qoc=min_qoc,
        max_distance_m=int(max_distance) if max_distance else None,
        route_preference=route_pref,
        sla_ms=int(sla_ms),
    )
