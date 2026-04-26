#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${1:-$(pwd)}"
ROOT_DIR="$(cd "$ROOT_DIR" && pwd)"

APP_CONTAINER="${APP_CONTAINER:-kanbin-app-eco}"
OUT_DIR="${OUT_DIR:-$ROOT_DIR/benchmark-results}"
mkdir -p "$OUT_DIR"

STAMP="$(date +%Y%m%d-%H%M%S)"
STATS_FILE="$OUT_DIR/docker-stats-$STAMP.csv"
ECO_LOG_FILE="$OUT_DIR/eco-run-$STAMP.log"

cd "$ROOT_DIR"

docker compose up -d --build

echo "Waiting for Kanbin on http://localhost:8080/ ..."
for _ in $(seq 1 60); do
  if curl -fsS http://localhost:8080/ >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "timestamp_iso|timestamp_epoch_ms|container|cpu_perc|mem_usage|mem_perc|net_io|block_io|pids" > "$STATS_FILE"

collect_stats() {
  while true; do
    TS_ISO="$(date --iso-8601=seconds)"
    TS_MS="$(python3 - <<'PY'
import time
print(int(time.time() * 1000))
PY
)"
    docker stats --no-stream --format '{{.Container}}|{{.CPUPerc}}|{{.MemUsage}}|{{.MemPerc}}|{{.NetIO}}|{{.BlockIO}}|{{.PIDs}}' "$APP_CONTAINER" \
      | awk -F'|' -v iso="$TS_ISO" -v ms="$TS_MS" '{print iso "|" ms "|" $0}' >> "$STATS_FILE" || true
    sleep 1
  done
}

collect_stats &
STATS_PID=$!

cleanup() {
  kill "$STATS_PID" 2>/dev/null || true
}
trap cleanup EXIT

echo "Collecting docker stats to $STATS_FILE"
echo "Running EcoExtension benchmark..."
./run-eco-benchmark.sh 2>&1 | tee "$ECO_LOG_FILE"

kill "$STATS_PID" 2>/dev/null || true
trap - EXIT

echo
echo "Artifacts:"
echo "  docker stats csv: $STATS_FILE"
echo "  eco benchmark log: $ECO_LOG_FILE"
echo
echo "Analyze with:"
echo "  python3 /mnt/data/analyze-docker-stats.py \"$STATS_FILE\""
