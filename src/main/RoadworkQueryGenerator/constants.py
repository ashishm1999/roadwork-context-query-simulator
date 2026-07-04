"""Constants shared across the roadwork query generator."""

from __future__ import annotations

# --- Roadwork scenario ---------------------------------------------------

SITE_IDS = (
    "hawthorn-glenferrie-rd",
    "burwood-highway-warrigal",
    "malvern-dandenong-rd",
    "cbd-swanston-collins",
)

CONTEXT_TYPES = ("roadwork", "incident", "weather", "aggregated")

# --- Consumer profiles --------------------------------------------------

CONSUMER_KINDS = (
    "autonomous_vehicle",      # safety-critical
    "consumer_navigation",     # standard
    "fleet_dispatch",          # commercial
    "city_operations_dashboard",  # background
)

CONSUMER_WEIGHTS = (0.15, 0.65, 0.15, 0.05)

# --- CDQL keys ----------------------------------------------------------

CDQL_PULL = "pull"
CDQL_PUSH = "push"

# --- Diurnal load parameters -------------------------------------------

MORNING_PEAK_HOUR = 7.5
EVENING_PEAK_HOUR = 17.5
MORNING_SIGMA_H = 1.2
EVENING_SIGMA_H = 1.4
OFFPEAK_FLOOR = 0.15
PEAK_AMPLITUDE = 0.85

# --- Freshness / QoC bounds --------------------------------------------

FRESHNESS_MIN = 0.20
FRESHNESS_MAX = 0.80
QOC_MIN = 0.50
QOC_MAX = 0.95

# --- Latency SLA buckets (ms) ------------------------------------------

SLA_BUCKETS = (100, 250, 500, 1000)
SLA_WEIGHTS = (0.30, 0.45, 0.20, 0.05)

# --- Provider identifiers -----------------------------------------------

PROVIDER_ROADWORK_PREFIX = "prov-roadwork-"
PROVIDER_WEATHER = "prov-weather-melb"
PROVIDER_INCIDENT = "prov-incidents-vicroads"
