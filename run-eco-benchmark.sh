#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$ROOT_DIR"

docker compose up -d --build

echo "Waiting for Kanbin on http://localhost:8080/ ..."
for _ in $(seq 1 60); do
  if curl -fsS http://localhost:8080/ >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

cd testbench
export POD_IP="${POD_IP:-127.0.0.1}"

mvn -Dtest=KanbinEcoScenarioIT test
