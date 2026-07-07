#!/usr/bin/env bash
# Launches N self-registered simulated devices in parallel -- no real hardware
# needed. Each device registers itself against hufng-ingest, then publishes
# telemetry over MQTT until Ctrl-C.
#
# Usage:
#   ./run_fleet.sh [device_count] [ingest_url] [mqtt_broker] [mqtt_port]
#
# Example (defaults shown):
#   ./run_fleet.sh 3 http://localhost:8084 localhost 1883
set -euo pipefail

DEVICE_COUNT="${1:-3}"
INGEST_URL="${2:-http://localhost:8084}"
MQTT_BROKER="${3:-localhost}"
MQTT_PORT="${4:-1883}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PIDS=()

cleanup() {
  echo "Stopping fleet..."
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
}
trap cleanup EXIT INT TERM

# One device (device #1, first started) occasionally resends an old seq, to
# demo dedupe (eventual in ClickHouse, exact in processed_event downstream).
for i in $(seq 1 "$DEVICE_COUNT"); do
  extra_args=()
  if [ "$i" -eq 1 ]; then
    extra_args=(--repeat-seq 5)
  fi

  python3 "$SCRIPT_DIR/simulate_device.py" \
    --register --ingest-url "$INGEST_URL" \
    --device-name "fleet-device-$i" \
    --broker "$MQTT_BROKER" --port "$MQTT_PORT" \
    "${extra_args[@]}" &
  PIDS+=($!)
done

echo "Started $DEVICE_COUNT simulated device(s). Ctrl-C to stop."
wait
