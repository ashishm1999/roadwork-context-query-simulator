#!/usr/bin/env bash
# Two-stage generator: template rows -> executable CDQL queries.
#
# Usage:
#     bash queryGenerator.bash [--horizon HOURS] [--count N] [--seed N] [--output PATH]
#
# Defaults reproduce the 24-hour, 280,000-query workload used across the
# ACOCA-P evaluation chapters.

set -euo pipefail

HORIZON=24
COUNT=280000
SEED=20260703
OUTPUT="sample/generated-queries.json"
COAAS_URL="${COAAS_URL:-http://localhost:8080}"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --horizon) HORIZON="$2"; shift 2 ;;
        --count)   COUNT="$2";   shift 2 ;;
        --seed)    SEED="$2";    shift 2 ;;
        --output)  OUTPUT="$2";  shift 2 ;;
        *) echo "Unknown flag: $1"; exit 1 ;;
    esac
done

echo "Stage 1: sampling ${COUNT} template rows across ${HORIZON}h (seed=${SEED})..."
mvn -q package -DskipTests

java -cp target/roadwork-context-query-simulator-1.0.0.jar \
    org.coaas.roadwork.generator.QueryGenerator \
    --templates "dataset/Context Query Templates.csv" \
    --count "${COUNT}" \
    --horizon "${HORIZON}" \
    --seed "${SEED}" \
    --output "dataset/generated_templates_${SEED}.csv"

echo "Stage 2: transforming templates into CDQL queries..."
java -cp target/roadwork-context-query-simulator-1.0.0.jar \
    org.coaas.roadwork.generator.CDQLTransformer \
    --input  "dataset/generated_templates_${SEED}.csv" \
    --output "${OUTPUT}"

echo "Done. Query load written to ${OUTPUT}."
