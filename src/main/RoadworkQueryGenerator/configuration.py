"""Load and validate config.ini for the roadwork query generator."""

from __future__ import annotations

import configparser
import os
from pathlib import Path
from typing import Any

from error import ConfigurationError


class Configuration:
    """Read-only view over config.ini with typed accessors."""

    def __init__(self, path: str | Path = "config.ini") -> None:
        path = Path(path)
        if not path.exists():
            raise ConfigurationError(f"Config file not found: {path}")
        self._parser = configparser.ConfigParser()
        self._parser.read(path)
        self._base = path.parent

    # --- General ---------------------------------------------------------

    @property
    def scenario(self) -> str:
        return self._get("general", "scenario", "melbourne-roadwork")

    @property
    def random_seed(self) -> int:
        return int(self._get("general", "random_seed", "0"))

    @property
    def default_horizon_hours(self) -> int:
        return int(self._get("general", "default_horizon_hours", "24"))

    @property
    def default_query_count(self) -> int:
        return int(self._get("general", "default_query_count", "280000"))

    # --- Datasets --------------------------------------------------------

    @property
    def sites_json(self) -> Path:
        return self._resolve_path(self._get("sites", "sites_json"))

    @property
    def consumer_types_json(self) -> Path:
        return self._resolve_path(self._get("consumers", "consumer_types_json"))

    @property
    def vin_registry_csv(self) -> Path:
        return self._resolve_path(self._get("consumers", "vin_registry_csv"))

    @property
    def incidents_csv(self) -> Path:
        return self._resolve_path(self._get("incidents", "history_csv"))

    @property
    def burst_probability(self) -> float:
        return float(self._get("incidents", "burst_probability", "0.05"))

    @property
    def burst_horizon_min(self) -> int:
        return int(self._get("incidents", "burst_horizon_min", "30"))

    @property
    def weather_stations_csv(self) -> Path:
        return self._resolve_path(self._get("weather", "stations_csv"))

    # --- Distribution ----------------------------------------------------

    def distribution_params(self) -> dict[str, float]:
        keys = (
            "morning_peak_hour", "evening_peak_hour",
            "morning_sigma_h", "evening_sigma_h",
            "offpeak_floor", "peak_amplitude",
        )
        return {k: float(self._get("distribution", k)) for k in keys}

    # --- Output ----------------------------------------------------------

    @property
    def sample_dir(self) -> Path:
        return self._resolve_path(self._get("output", "sample_dir", "../../../sample"))

    @property
    def template_dir(self) -> Path:
        return self._resolve_path(self._get("output", "template_dir", "../../../dataset"))

    # --- Helpers ---------------------------------------------------------

    def _get(self, section: str, key: str, default: Any | None = None) -> str:
        if not self._parser.has_section(section):
            if default is not None:
                return str(default)
            raise ConfigurationError(f"Missing section [{section}]")
        if not self._parser.has_option(section, key):
            if default is not None:
                return str(default)
            raise ConfigurationError(f"Missing key {section}.{key}")
        return self._parser.get(section, key)

    def _resolve_path(self, raw: str) -> Path:
        expanded = os.path.expandvars(raw)
        p = Path(expanded)
        if not p.is_absolute():
            p = (self._base / p).resolve()
        return p
