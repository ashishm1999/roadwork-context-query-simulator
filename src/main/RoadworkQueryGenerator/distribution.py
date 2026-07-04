"""Probability distributions used by the roadwork query generator.

The core distribution is a two-peak Gaussian mixture mimicking the observed
Melbourne commute (morning at ~7:30, evening at ~17:30). Auxiliary helpers
cover categorical draws and diurnal jitter used to spread queries around
their nominal submission time.
"""

from __future__ import annotations

import math
import random
from dataclasses import dataclass
from typing import Sequence


@dataclass
class DiurnalProfile:
    """Two-peak Gaussian mixture over hours 0..24."""

    morning_peak_hour: float = 7.5
    evening_peak_hour: float = 17.5
    morning_sigma_h: float = 1.2
    evening_sigma_h: float = 1.4
    offpeak_floor: float = 0.15
    peak_amplitude: float = 0.85

    def load_at(self, hour: float) -> float:
        morning = math.exp(-0.5 * ((hour - self.morning_peak_hour) / self.morning_sigma_h) ** 2)
        evening = math.exp(-0.5 * ((hour - self.evening_peak_hour) / self.evening_sigma_h) ** 2)
        return self.offpeak_floor + self.peak_amplitude * (morning + evening)


def sample_diurnal_hour(profile: DiurnalProfile, rng: random.Random) -> float:
    """Draw an hour of day weighted by the diurnal profile."""
    # Rejection sampling — the mixture has bounded support.
    while True:
        h = rng.uniform(0, 24)
        u = rng.uniform(0, profile.offpeak_floor + 2.0 * profile.peak_amplitude)
        if u <= profile.load_at(h):
            return h


def choice_weighted(values: Sequence[str], weights: Sequence[float], rng: random.Random) -> str:
    """Categorical draw."""
    r = rng.random()
    acc = 0.0
    for v, w in zip(values, weights):
        acc += w
        if r <= acc:
            return v
    return values[-1]


def gaussian_bounded(mean: float, sigma: float, low: float, high: float, rng: random.Random) -> float:
    """Draw from a truncated Gaussian by rejection."""
    while True:
        x = rng.gauss(mean, sigma)
        if low <= x <= high:
            return x


def jitter_submit_time(profile: DiurnalProfile, base_ms: float, rng: random.Random) -> float:
    """Perturb a base submit time by an amount that shrinks during peaks."""
    hour = (base_ms / 3_600_000.0) % 24
    load = profile.load_at(hour)
    scale = max(0.05, 1.0 - min(0.9, load))
    return base_ms + rng.gauss(0.0, 5000.0) * scale


def poisson_arrivals(rate_per_second: float, duration_s: float, rng: random.Random) -> list[float]:
    """Inhomogeneous Poisson process — thin arrivals against rate_per_second."""
    if rate_per_second <= 0:
        return []
    arrivals: list[float] = []
    t = 0.0
    while t < duration_s:
        t += rng.expovariate(rate_per_second)
        if t < duration_s:
            arrivals.append(t)
    return arrivals
