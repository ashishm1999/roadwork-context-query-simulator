package org.coaas.roadwork.utils;

/**
 * Enumerated set of operations that any subscriber must recognise.
 *
 * QUERY_SUBMITTED is emitted whenever a fresh CDQL query is scheduled.
 * QUERY_RESPONSE is emitted when the CoaaS Query Engine returns a result.
 * POA_UPDATE, CF_UPDATE, CVI_UPDATE are used by the ACOCA-P adapter to
 * broadcast per-parameter refreshes.
 */
public enum Operation {
    QUERY_SUBMITTED,
    QUERY_RESPONSE,
    QUERY_ERROR,
    POA_UPDATE,
    CF_UPDATE,
    CVI_UPDATE,
    DECISION_MADE,
    POLICY_PUSHED,
    HEARTBEAT
}
