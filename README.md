# hufng-iot

IoT ingestion pipeline — Gradle (Kotlin DSL) multi-project build, Spring Boot 3.3.4
on Java 21. Independent build (not a submodule of `hufng-pet`); `hufng-common` is
copied in rather than depended on, so this repo builds and runs on its own.

End-to-end story: a device publishes telemetry over MQTT, ingest authenticates
it and buffers into Kafka, report writes it to ClickHouse and checks alert
rules, notification dedupes and routes the alert to Telegram/Discord.

```
Device (own seq, MQTT QoS 1)
      │  topic: device/{id}/telemetry
      ▼
MQTT Broker (EMQX, Docker)
      │  shared subscription: $share/hufng-ingest/device/+/telemetry
      ▼
hufng-ingest :8084          MQTT bridge — device auth (Redis cache + MySQL registry) → Kafka
      │
      ▼
Kafka topic DEVICE_TELEMETRY (3 partitions, key=deviceId)
      │
      ▼
hufng-report :8083          upsert ClickHouse telemetry (dedupe eventual) + rule check
      │
      ▼
Kafka topic RESOURCE_ALERT (eventId generated here)
      │
      ▼
hufng-notification :8082    dedupe processed_event (MySQL, exact) → Telegram/Discord
```

## Module structure

| Module | Role |
|---|---|
| `hufng-common` | Shared library copied from `hufng-pet` — `CommonResult`, `RedisService`/`BaseRedisConfig`, exception handling, logging aspect. |
| `hufng-ingest` | MQTT bridge — device registry/auth (MySQL `device` table + Redis cache), subscribes MQTT shared subscription, produces `DEVICE_TELEMETRY`. Also exposes `POST /v1/devices/register`. |
| `hufng-report` | Consumes `DEVICE_TELEMETRY`, writes to ClickHouse (`telemetry` table), checks alert rules (config-based), produces `RESOURCE_ALERT`. |
| `hufng-notification` | Consumes `RESOURCE_ALERT`, dedupes exactly via MySQL `processed_event` (INSERT-first), routes to Telegram/Discord. |
| `infra/` | Not a Gradle subproject — Docker Compose for the whole stack: Eureka, EMQX (MQTT), ClickHouse, MySQL, Redis, Kafka (single-node KRaft), and the 3 app services (each built from its own multi-stage `Dockerfile`). |
| `tools/device-simulator/` | Python script (not part of the Gradle build) simulating one or more devices publishing telemetry over MQTT — can self-register against `hufng-ingest`, no real hardware needed. |

## Ports

| Service | Port | Context-path |
|---|---|---|
| `hufng-ingest` | 8084 | *(none)* |
| `hufng-notification` | 8082 | `/notification/api` |
| `hufng-report` | 8083 | *(none)* |
| `eureka` (Docker) | 8761 | *(none)* |
| `mqtt` / EMQX (Docker) | 1883 (MQTT), 18083 (dashboard) | — |
| `clickhouse` (Docker) | 8123 (HTTP/JDBC), 9000 (native) | — |

## Simplifications (intentional, out of scope for this iteration)

- `POST /v1/devices/register` has no auth — `ownerId` is taken directly from
  the request body. A fuller system would resolve it from a JWT/session
  (like `hufng-auth` in `hufng-pet`), which this repo deliberately doesn't
  depend on.
- No `hufng-gateway`/`hufng-auth` — services are reached directly by port.
- Alert rules are config-based (`application.yml`), not a DB table — fine at
  "load once, check per message" scale; a production rule engine would move to
  Kafka Streams/Flink.

## Common commands

```bash
# Requires JDK 21 as JAVA_HOME; ./gradlew pins Gradle 8.6
# Build everything
./gradlew build

# Test (no Docker/broker needed — H2 + auto-startup/eureka disabled in test yml)
./gradlew test
./gradlew :hufng-notification:test --tests ProcessedEventServiceTest   # single class

# Whole stack in Docker: eureka, mqtt (EMQX), clickhouse, mysql, redis, kafka,
# and the 3 app services (each built from its own Dockerfile). Optionally
# export TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID / DISCORD_WEBHOOK_URL first if
# you want real alerts delivered.
cd infra && docker compose up --build -d

# Or run services locally against Docker-only infra (gradle bootRun
# instead of the app containers above): comment out the 3 app services in
# infra/docker-compose.yml, `docker compose up -d` the rest, then:
./gradlew :hufng-ingest:bootRun       # one service per terminal
./gradlew :hufng-report:bootRun
./gradlew :hufng-notification:bootRun
```

## Demo end-to-end (no real device needed)

```bash
# Everything running (docker compose up, or the 3 services locally)?
# Just launch a simulated fleet -- each instance self-registers against
# hufng-ingest via POST /v1/devices/register, then publishes over MQTT.
# Needs `pip install paho-mqtt` locally.
./tools/device-simulator/run_fleet.sh 3

# Or a single device, self-registered:
python3 tools/device-simulator/simulate_device.py --register

# Or bring your own credentials from a prior register call:
python3 tools/device-simulator/simulate_device.py --device-id <id> --secret <secret>
```

Watch `hufng-ingest` logs for produced Kafka messages, `hufng-report` logs for
ClickHouse inserts + rule checks, and Telegram/Discord for alerts once
`temperature` exceeds the configured threshold (default 30, see
`hufng-report`'s `application.yml` `report.alert-rules`). `run_fleet.sh`'s
first device periodically resends an old `seq` to demonstrate dedupe:
duplicated eventually in ClickHouse, blocked exactly in
`hufng-notification`'s `processed_event`.
