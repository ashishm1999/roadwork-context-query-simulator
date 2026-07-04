package org.coaas.roadwork;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.coaas.roadwork.generator.CDQLTransformer;
import org.coaas.roadwork.model.QueryTemplate;
import org.junit.jupiter.api.Test;

class CDQLTransformerTest {

    @Test
    void rendersTargetSiteAndVin() {
        QueryTemplate t = base();
        String cdql = CDQLTransformer.render(t);
        assertTrue(cdql.contains("consumerCar.vin = \"13UNVER82367G4\""));
        assertTrue(cdql.contains("targetRoadwork.site_id = \"hawthorn-glenferrie-rd\""));
    }

    @Test
    void freshnessAppearsOnlyWhenPresent() {
        QueryTemplate withCF = base();
        withCF.minFreshness = 0.38;
        assertTrue(CDQLTransformer.render(withCF).contains("targetRoadwork.freshness >= 0.38"));

        QueryTemplate withoutCF = base();
        withoutCF.minFreshness = null;
        assertTrue(!CDQLTransformer.render(withoutCF).contains("targetRoadwork.freshness"));
    }

    @Test
    void qocAppearsOnlyWhenPresent() {
        QueryTemplate withQoc = base();
        withQoc.minQoc = 0.7;
        assertTrue(CDQLTransformer.render(withQoc).contains("targetRoadwork.qoc >= 0.70"));
    }

    @Test
    void slaBoundsAppendWhenPresent() {
        QueryTemplate t = base();
        t.slaMs = 250;
        assertTrue(CDQLTransformer.render(t).contains("targetRoadwork.sla_ms <= 250"));
    }

    private QueryTemplate base() {
        QueryTemplate t = new QueryTemplate();
        t.vehicleId = "13UNVER82367G4";
        t.targetSite = "hawthorn-glenferrie-rd";
        t.locationLat = -37.8218;
        t.locationLng = 145.0421;
        t.contextType = "roadwork";
        t.maxDistanceM = 5000;
        t.hour = 8;
        t.minute = 15;
        t.second = 0;
        t.day = "monday";
        t.submitAtMs = 29_700_000L;
        return t;
    }
}
