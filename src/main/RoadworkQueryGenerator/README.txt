RoadworkQueryGenerator
======================

The Python side of the two-stage generator. This folder mirrors the layout
of ShakthiYasas/context-query-simulator's ParkingQueryGenerator/, but the
scenario is the Melbourne roadwork context caching workload described in
the PhD thesis.

Files
-----

    config.ini             INI configuration (paths, distributions, seeds)
    configuration.py       Typed loader for config.ini
    constants.py           Sites, consumer kinds, CDQL keywords, SLA buckets
    distribution.py        Two-peak Gaussian mixture and helpers
    error.py               Typed exceptions
    requirements.txt       Python dependencies
    sites/                 Roadwork site metadata + samplers
    consumers/             Consumer profile registry + VIN pool
    incidents/             Historical incident records + burst injector
    weather/               Weather stations + condition sampler
    query_temp/            Query templates + CDQL renderer

Usage
-----

    # Stage 1: generate the raw templates CSV
    python -m generator --templates ../../../dataset/Context\ Query\ Templates.csv \
                        --count 280000 --output ../../../dataset/generated_templates_20260703.csv

    # Stage 2: fold templates into executable CDQL query JSON
    python -m cdql --input ../../../dataset/generated_templates_20260703.csv \
                   --output ../../../sample/generated-queries.json
