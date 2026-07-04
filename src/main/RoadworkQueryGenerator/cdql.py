"""Top-level Stage 2 driver — folds template rows into CDQL JSON."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

from query_temp import CDQLRenderer, QueryTemplate


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--input", required=True)
    ap.add_argument("--output", required=True)
    args = ap.parse_args()

    input_path = Path(args.input)
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    renderer = CDQLRenderer()
    count = 0
    with input_path.open("r", encoding="utf-8") as ih, output_path.open("w", encoding="utf-8") as oh:
        reader = csv.DictReader(ih)
        oh.write("[")
        first = True
        for row in reader:
            t = QueryTemplate(
                query_id=row["query_id"],
                vehicle_id=row["vehicle_id"],
                location_lat=float(row["location_lat"]),
                location_lng=float(row["location_lng"]),
                target_site=row["target_site"],
                context_type=row["context_type"],
                day=row["day"],
                hour=int(row["hour"]),
                minute=int(row["minute"]),
                second=int(row["second"]),
                submit_at_ms=int(row["submit_at_ms"]),
                consumer_kind=row["consumer_kind"],
                max_delay_min=int(row["max_delay_min"]) if row["max_delay_min"] else None,
                min_freshness=float(row["min_freshness"]) if row["min_freshness"] else None,
                min_qoc=float(row["min_qoc"]) if row["min_qoc"] else None,
                max_distance_m=int(row["max_distance_m"]) if row["max_distance_m"] else None,
                route_preference=row["route_preference"] or None,
                sla_ms=int(row["sla_ms"]) if row["sla_ms"] else None,
            )
            cdql = renderer.render(t)
            record = {
                "query_id": t.query_id,
                "submit_at_ms": t.submit_at_ms,
                "sla_ms": t.sla_ms or 250,
                "cdql": cdql,
                "meta": t.to_dict(),
            }
            if not first:
                oh.write(",\n")
            oh.write(json.dumps(record, separators=(",", ":")))
            first = False
            count += 1
        oh.write("]")
    print(f"Wrote {count:,} CDQL queries to {output_path}")


if __name__ == "__main__":
    main()
