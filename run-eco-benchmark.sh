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
export BENCHMARK_CONTAINER_NAME="${BENCHMARK_CONTAINER_NAME:-kanbin-app-eco}"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export JRE_HOME="$JAVA_HOME"
export GATLING_JAVA_HOME="$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

# Compat Gatling / sous-build Maven
export MAVEN_OPTS="${MAVEN_OPTS:-} -Dmaven.compiler.release=11 -Dmaven.compiler.source=11 -Dmaven.compiler.target=11"

echo "JAVA_HOME=$JAVA_HOME"
java -version
mvn -version

rm -rf ./gatling

mvn -Dtest=KanbinEcoScenarioIT test