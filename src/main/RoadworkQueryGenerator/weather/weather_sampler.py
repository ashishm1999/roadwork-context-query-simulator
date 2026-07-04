"""Weather sampler — mixes categorical states with BOM-shaped continuous fields."""

from __future__ import annotations

import csv
import random
from dataclasses import dataclass
from pathlib import Path


WEATHER_STATES = ("clear", "cloudy", "overcast", "light-rain", "heavy-rain", "fog")
WEATHER_WEIGHTS = (0.42, 0.22, 0.14, 0.13, 0.06, 0.03)


@dataclass
class WeatherReading:
    station_id: str
    condition: str
    temperature_c: float
    humidity_pct: float
    precipitation_mm_h: float
    visibility_km: float
    wind_speed_kph: float


class WeatherSampler:
    def __init__(self, stations: list[str]) -> None:
        self._stations = list(stations) if stations else ["melb-central"]

    @classmethod
    def load(cls, path: Path) -> "WeatherSampler":
        stations: list[str] = []
        if path.exists():
            with path.open("r", encoding="utf-8") as fh:
                reader = csv.DictReader(fh)
                for row in reader:
                    if row.get("station_id"):
                        stations.append(row["station_id"])
        return cls(stations)

    def sample(self, rng: random.Random) -> WeatherReading:
        condition = _weighted_choice(WEATHER_STATES, WEATHER_WEIGHTS, rng)
        station = rng.choice(self._stations)
        base_temp = rng.gauss(18.0, 4.5)
        base_humidity = rng.uniform(45.0, 90.0)
        if condition in ("light-rain", "heavy-rain"):
            base_humidity = max(base_humidity, 78.0)
        precipitation = 0.0
        if condition == "light-rain":
            precipitation = rng.uniform(0.5, 2.5)
        elif condition == "heavy-rain":
            precipitation = rng.uniform(4.0, 15.0)
        visibility = rng.uniform(6.0, 20.0)
        if condition == "fog":
            visibility = rng.uniform(0.2, 2.0)
        wind = rng.uniform(3.0, 35.0)
        return WeatherReading(
            station_id=station,
            condition=condition,
            temperature_c=round(base_temp, 1),
            humidity_pct=round(base_humidity, 1),
            precipitation_mm_h=round(precipitation, 1),
            visibility_km=round(visibility, 1),
            wind_speed_kph=round(wind, 1),
        )


def _weighted_choice(values, weights, rng):
    r = rng.random()
    acc = 0.0
    for v, w in zip(values, weights):
        acc += w
        if r <= acc:
            return v
    return values[-1]
