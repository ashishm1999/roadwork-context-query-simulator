"""Consumer registry — knows about the four consumer kinds we simulate."""

from __future__ import annotations

import csv
import random
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence


@dataclass
class ConsumerProfile:
    kind: str
    weight: float
    max_latency_ms: int
    min_freshness: float
    min_qoc: float
    priority: str


DEFAULTS: tuple[ConsumerProfile, ...] = (
    ConsumerProfile("autonomous_vehicle",         0.15, 100,  0.55, 0.90, "safety-critical"),
    ConsumerProfile("consumer_navigation",        0.65, 250,  0.38, 0.70, "standard"),
    ConsumerProfile("fleet_dispatch",             0.15, 500,  0.35, 0.60, "standard"),
    ConsumerProfile("city_operations_dashboard",  0.05, 1000, 0.25, 0.50, "background"),
)


class ConsumerRegistry:
    def __init__(self, profiles: Sequence[ConsumerProfile] = DEFAULTS, vins: Sequence[str] | None = None) -> None:
        if not profiles:
            raise ValueError("empty consumer profile list")
        total = sum(p.weight for p in profiles)
        self._profiles = list(profiles)
        self._cdf = self._build_cdf(total)
        self._vins = list(vins) if vins else []

    @classmethod
    def load(cls, vin_csv: Path | None = None) -> "ConsumerRegistry":
        vins: list[str] = []
        if vin_csv is not None and vin_csv.exists():
            with vin_csv.open("r", encoding="utf-8") as fh:
                reader = csv.reader(fh)
                header = next(reader, None)
                if header:
                    idx = header.index("vin") if "vin" in header else 0
                    for row in reader:
                        if row and len(row) > idx:
                            vins.append(row[idx])
        return cls(DEFAULTS, vins)

    @property
    def profiles(self) -> list[ConsumerProfile]:
        return list(self._profiles)

    def sample_profile(self, rng: random.Random) -> ConsumerProfile:
        r = rng.random()
        for profile, cutoff in self._cdf:
            if r <= cutoff:
                return profile
        return self._profiles[-1]

    def sample_vin(self, rng: random.Random) -> str:
        if self._vins:
            return rng.choice(self._vins)
        return _random_vin(rng)

    def _build_cdf(self, total: float) -> list[tuple[ConsumerProfile, float]]:
        cdf = []
        cum = 0.0
        for p in self._profiles:
            cum += p.weight / total
            cdf.append((p, cum))
        return cdf


def _random_vin(rng: random.Random) -> str:
    alpha = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789"
    return "".join(rng.choice(alpha) for _ in range(17))
