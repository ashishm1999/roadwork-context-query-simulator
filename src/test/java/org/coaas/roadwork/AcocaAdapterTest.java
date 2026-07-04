package org.coaas.roadwork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.coaas.roadwork.acoca.AcocaAdapter;
import org.coaas.roadwork.jobs.RoadworkContextQuery;
import org.junit.jupiter.api.Test;

class AcocaAdapterTest {

    @Test
    void adapterIsSingletonAndEnabledByDefault() {
        assertNotNull(AcocaAdapter.getInstance());
        assertTrue(AcocaAdapter.isEnabled());
    }

    @Test
    void disablingHonoursRequest() {
        AcocaAdapter adapter = AcocaAdapter.getInstance();
        adapter.setEnabled(false);
        assertFalse(AcocaAdapter.isEnabled());
        adapter.setEnabled(true);   // restore for other tests
    }

    @Test
    void dispatchReturnsFallbackWhenServicesAreUnreachable() {
        // No CAPME/CFMS/DCMF/VACF running on the test host — the adapter
        // should complete with a fallback decision rather than throw.
        RoadworkContextQuery q = new RoadworkContextQuery(
            "monday", 7, 30, 0,
            "prefix schema:http//schema.org pull (foo)",
            "q-test-000000001",
            "Bearer test-token",
            "hawthorn-glenferrie-rd",
            250
        );
        AcocaAdapter.AcocaDecision decision = AcocaAdapter.getInstance().dispatch(q).join();
        assertNotNull(decision);
        assertEquals("hawthorn-glenferrie-rd", decision.itemId);
    }
}
