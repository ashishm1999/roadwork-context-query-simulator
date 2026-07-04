"""Incident registry — loads historical incidents and can inject bursts."""

from __future__ import annotations

import csv
import random
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


@dataclass
class Incident:
    incident_id: str
    site_id: str
    type: str
    severity: str
    started_at_iso: str
    duration_min: int
    detour: str
    notes: str


class IncidentRegistry:
    def __init__(self, incidents: Iterable[Incident]) -> None:
        self._incidents = list(incidents)

    @classmethod
    def load(cls, path: Path) -> "IncidentRegistry":
        if not path.exists():
            return cls([])
        rows: list[Incident] = []
        with path.open("r", encoding="utf-8") as fh:
            reader = csv.DictReader(fh)
            for row in reader:
                rows.append(Incident(
                    incident_id=row.get("incident_id", ""),
                    site_id=row.get("site_id", ""),
                    type=row.get("type", ""),
                    severity=row.get("severity", ""),
                    started_at_iso=row.get("started_at_iso", ""),
                    duration_min=int(row.get("duration_min", 0) or 0),
                    detour=row.get("detour", ""),
                    notes=row.get("notes", ""),
                ))
        return cls(rows)

    @property
    def incidents(self) -> list[Incident]:
        return list(self._incidents)

    def sample(self, rng: random.Random) -> Incident:
        if not self._incidents:
            raise ValueError("no incidents loaded")
        return rng.choice(self._incidents)

    def burst_window(self, rng: random.Random, at_ms: int, horizon_min: int = 30) -> Incident:
        """Return an incident biased toward the busiest sites — used to
        inject the 30-minute high-volatility regime evaluated in Ch8/10."""
        candidates = [i for i in self._incidents if i.severity != "minor"]
        if not candidates:
            candidates = self._incidents
        return rng.choice(candidates)
