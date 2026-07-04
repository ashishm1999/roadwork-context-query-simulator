"""CDQL renderer — turns a QueryTemplate into an executable CDQL query."""

from __future__ import annotations

from .query_template import QueryTemplate


class CDQLRenderer:
    """Stateless renderer. Constants keep the CDQL surface predictable."""

    PREFIX = "prefix schema:http//schema.org"
    FRESHNESS_FIELD = "targetRoadwork.freshness"
    QOC_FIELD = "targetRoadwork.qoc"
    DELAY_FIELD = "targetRoadwork.time_delay"

    def render(self, t: QueryTemplate) -> str:
        distance = t.max_distance_m or 5000
        lines = [
            self.PREFIX,
            "pull (targetRoadwork.congestion, targetRoadwork.detour, targetRoadwork.time_delay)",
            f'when distance(consumerCar.location, targetRoadwork.location) <= {{"value": {distance}, "unit":"m"}}',
            "define",
            f'  entity consumerCar    is from schema:Vehicle  where consumerCar.vin = "{t.vehicle_id}",',
            f'  entity targetRoadwork is from coaas:Roadwork  where',
            f'      targetRoadwork.site_id = "{t.target_site}"',
        ]
        if t.min_freshness is not None:
            lines.append(f'      and {self.FRESHNESS_FIELD} >= {t.min_freshness:.2f}')
        if t.min_qoc is not None:
            lines.append(f'      and {self.QOC_FIELD} >= {t.min_qoc:.2f}')
        if t.max_delay_min is not None:
            lines.append(f'      and {self.DELAY_FIELD} < {{"value": {t.max_delay_min}, "unit":"min"}}')
        if t.sla_ms is not None:
            lines.append(f'      and targetRoadwork.sla_ms <= {t.sla_ms}')
        return "\n".join(lines)
