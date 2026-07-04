"""Site registry — reads roadwork_sites.json into typed objects."""

from __future__ import annotations

import json
import random
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence


@dataclass
class Site:
    site_id: str
    suburb: str
    road: str
    lat: float
    lng: float
    speed_limit_kph: int
    lanes: int
    weight: float


class SiteRegistry:
    def __init__(self, sites: Sequence[Site]) -> None:
        if not sites:
            raise ValueError("empty site list")
        total = sum(s.weight for s in sites)
        if total <= 0:
            raise ValueError("site weights sum to zero")
        self._sites = list(sites)
        self._cdf = self._build_cdf(total)

    @classmethod
    def load(cls, path: Path) -> "SiteRegistry":
        with path.open("r", encoding="utf-8") as fh:
            data = json.load(fh)
        raw = data.get("sites", data)  # support both {sites: [...]} and bare list
        sites = []
        weights = {
            "hawthorn-glenferrie-rd": 0.30,
            "burwood-highway-warrigal": 0.25,
            "malvern-dandenong-rd": 0.25,
            "cbd-swanston-collins": 0.20,
        }
        for entry in raw:
            geo = entry.get("geo", {})
            sites.append(Site(
                site_id=entry["site_id"],
                suburb=entry.get("suburb", entry["site_id"]),
                road=entry.get("road", ""),
                lat=float(geo.get("lat", entry.get("lat", 0.0))),
                lng=float(geo.get("lng", entry.get("lng", 0.0))),
                speed_limit_kph=int(entry.get("speed_limit_kph", 60)),
                lanes=int(entry.get("lanes", 2)),
                weight=float(weights.get(entry["site_id"], 0.25)),
            ))
        return cls(sites)

    @property
    def sites(self) -> list[Site]:
        return list(self._sites)

    def by_id(self, site_id: str) -> Site:
        for s in self._sites:
            if s.site_id == site_id:
                return s
        raise KeyError(site_id)

    def sample(self, rng: random.Random) -> Site:
        r = rng.random()
        for site, cutoff in self._cdf:
            if r <= cutoff:
                return site
        return self._sites[-1]

    def _build_cdf(self, total: float) -> list[tuple[Site, float]]:
        cdf = []
        cum = 0.0
        for s in self._sites:
            cum += s.weight / total
            cdf.append((s, cum))
        return cdf
