# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`hufng-iot` is an IoT ingestion pipeline — Java Gradle (Kotlin DSL) multi-project build, Spring Boot 3.3.4 on Java 21. It is an **independent build**, not a submodule of the sibling project `hufng-pet`: `hufng-common` is copied in rather than depended on across repos, so this repo builds and runs entirely on its own.

End-to-end story: a device publishes telemetry over MQTT → `hufng-ingest` authenticates the device and buffers into Kafka → `hufng-report` writes it to ClickHouse and checks alert rules → `hufng-notification` dedupes and routes the alert to Telegram/Discord.

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
Kafka topic RESOURCE_ALERT (eventId generated here, by the producer)
      │
      ▼
hufng-notification :8082    dedupe processed_event (MySQL, exact) → Telegram/Discord
```

## Module structure

- Root `settings.gradle.kts` lists the 4 subprojects. Root `build.gradle.kts` sets groupId `com.hufng.iot`, the Java 21 toolchain, and — in a `subprojects {}` block — applies `io.spring.dependency-management` importing the `spring-boot-dependencies` + `spring-cloud-dependencies` BOMs and pinning `clickhouse-jdbc`, so module build files declare dependencies without versions. Version numbers live in `gradle.properties` (except the plugin versions in the root `plugins {}` block, which must stay in sync with `springBootVersion`). Does not contain code itself.
- `hufng-common/` — copied verbatim from `hufng-pet/hufng-common` (package `com.hufng.common`, unchanged so nothing downstream needed an import rewrite): `CommonResult`, `BaseRedisConfig`/`RedisService`, exception handling (`GlobalExceptionHandler`, `Asserts`), `WebLogAspect`, `JacksonUtils`.
- `hufng-ingest/` — MQTT bridge (executable Spring Boot app). Package root `com.hufng.ingest`. Port `8084`, no context-path, registers with Eureka. Does 5 things per message: parse → validate device → auth token → validate schema → produce Kafka; any failure logs a warning and drops the message (MQTT has already delivered it via QoS 1 — the bridge just chooses not to forward it, never throws back into the Paho adapter).
  - **Device registry + auth cache**: MySQL `device` table (`Device` entity: `deviceId`, `ownerId`, `deviceSecretHash` bcrypt, `status` enum `PENDING/ACTIVE/REVOKED`, `model`, timestamps). `DeviceAuthService` caches the `(status, secretHash)` projection in Redis under `device:{deviceId}` (TTL 1h, via `RedisService` from `hufng-common`) so most messages never hit MySQL; revoke = update DB + evict cache (evict path exists on the service but isn't wired to an admin endpoint yet).
  - **MQTT inbound**: `MqttConfig` sets up a `MqttPahoMessageDrivenChannelAdapter` subscribed to the literal topic string `$share/hufng-ingest/device/+/telemetry` (MQTT shared subscription — broker-side load balancing across every `hufng-ingest` instance in the group, the MQTT equivalent of a Kafka consumer group; no special client API needed, it's just a topic filter string), QoS 1. The whole `MqttConfig` class is gated by `@ConditionalOnProperty(mqtt.enabled, matchIfMissing = true)`, set to `false` in test `application.yml` so `./gradlew test` never needs a real broker. `TelemetryIngestHandler` (a plain `@Component`, not inside `MqttConfig`) does the actual parse/validate/produce and is unit-testable without Spring context.
  - **API**: `POST /v1/devices/register` (`DeviceRegistrationController`) — generates `deviceId`/`deviceSecret` (UUID), bcrypt-hashes the secret before persisting, warms the Redis cache immediately, and returns the secret in plaintext exactly once. **Intentionally has no auth** — `ownerId` comes straight from the request body; this module does not depend on `hufng-auth`/`hufng-gateway` from `hufng-pet` (see "Simplifications" below).
  - **Kafka producer**: `acks=all`, `enable.idempotence=true`; `NewTopic` bean declares `DEVICE_TELEMETRY` (3 partitions) so partitioning is controlled from day one instead of relying on broker auto-create.
- `hufng-report/` — telemetry consumer + rule engine (executable Spring Boot app). Package root `com.hufng.report`. Port `8083`, no context-path.
  - **Consumer**: `DeviceTelemetryConsumer` (`@KafkaListener(topics = "DEVICE_TELEMETRY", groupId = "hufng-report")`, manual ack) inserts into ClickHouse then runs the rule check; `ack.acknowledge()` happens in a `finally` regardless of outcome (matches the at-least-once + idempotent-consumer pattern used throughout this repo).
  - **Storage**: ClickHouse `telemetry` table, `JdbcTemplate` only (no JPA/Hibernate — ClickHouse has no dialect worth using here). `ReplacingMergeTree(_version)`, `ORDER BY (device_id, seq)`, TTL 7 days (DDL: `infra/init-clickhouse.sql`). **ClickHouse has no UNIQUE constraint** — duplicate rows land in the table regardless and are only merged away later by the background merge; querying `SELECT ... FROM telemetry FINAL` (or `argMax(...)`) forces a merge-consistent read when exactness is needed. This is an accepted, **eventual** dedupe trade-off for telemetry (losing/duplicating one data point doesn't matter) — contrast with `hufng-notification`'s **exact**, write-time dedupe below.
  - **Rule engine**: intentionally config-based, not a DB table — `report.alert-rules` in `application.yml` (`metric`, `threshold`, `channel`), loaded once at startup into an in-memory map (`RuleEngineService`, checked per message, no per-message DB query). A production-scale version would move to Kafka Streams/Flink instead of per-message evaluation in a single consumer.
  - **Alert publish**: crossing a threshold produces `RESOURCE_ALERT` (`NewTopic`, 3 partitions) with `eventId = UUID.randomUUID()` generated **here, by the producer** — never by the consumer (`hufng-notification`), otherwise re-reading the same message after a rebalance would mint a different id each time and the downstream dedupe would be meaningless.
  - Test `application.yml` excludes `DataSourceAutoConfiguration` entirely (`spring.autoconfigure.exclude`) rather than pointing at a fake ClickHouse URL — Hikari validates the connection at startup and would fail context load otherwise. Because of this, `TelemetryRepository` (constructor-injects `JdbcTemplate`) can't be exercised in a full Spring context test; `RuleEngineServiceTest` covers the rule logic with plain Mockito instead.
- `hufng-notification/` — copied from `hufng-pet/hufng-notification` (`NotificationRouter`, `TelegramService`/`DiscordService` gated by `@ConditionalOnProperty`, `MessageUtil`) then enhanced with idempotency. Package root `com.hufng.notification`. Port `8082`, `server.servlet.context-path: /notification/api`.
  - **Idempotent consumer**: `ResourceAlertConsumer` calls `ProcessedEventService.markProcessedIfNew(eventId, "hufng-notification")` *before* routing to `NotificationRouter` — **INSERT-first, not exists-then-insert**: two instances racing on the same duplicate message would otherwise both see `exists = false` under a check-then-act approach. The MySQL `processed_event` PK `(event_id, consumer)` is the arbiter.
  - `ProcessedEvent` **implements `Persistable<Id>`** with `isNew()` hardcoded to `true` — this is load-bearing, not decorative. The entity has no `@GeneratedValue` id, so Spring Data JPA's default new-entity check (`id == null`) would otherwise always see a non-null id and call `entityManager.merge()` (SELECT-then-INSERT-**or**-UPDATE) instead of `persist()` — silently turning a "duplicate" into a no-op UPDATE instead of throwing the PK violation this whole class exists to catch. `ProcessedEventService.markProcessedIfNew` also uses `saveAndFlush` (not `save`) so the constraint violation surfaces inside the `try` block — a plain `save()` only flushes at transaction commit, after the method has already returned.
  - `@Scheduled` cleanup (`ProcessedEventService.cleanupOldEvents`, daily at 03:00) deletes rows older than 7 days — retention must be ≥ the `RESOURCE_ALERT` topic's retention, since a message can't be re-read (and thus can't need dedupe) after it has left the topic.
  - Test `application.yml` uses H2 in MySQL mode (same pattern as `hufng-pet/hufng-auth`).
- `infra/` — **not a Gradle subproject.** Docker Compose for the whole stack: `eureka` (built from a copied `Dockerfile` wrapping `steeltoeoss/eureka-server`, port 8761), `mqtt` (EMQX, ports 1883 + 18083 dashboard, `EMQX_ALLOW_ANONYMOUS=true` — dev only), `clickhouse` (port 8123 HTTP/JDBC + 9000 native, `init-clickhouse.sql` mounted for table creation), `mysql`, `redis`, `kafka` (single-node KRaft — no Zookeeper, official `apache/kafka` image), and the 3 app services (`hufng-ingest`, `hufng-report`, `hufng-notification`), each built from its own multi-stage `Dockerfile` at the module root. Every app `Dockerfile`'s build stage `COPY`s the **whole repo** (not just its own module) — `settings.gradle.kts` includes all 4 modules, so `gradle :<module>:bootJar` still needs every module's `build.gradle.kts` to be readable even though it only compiles the one module + `hufng-common`; the build stage uses the `gradle:8.6-jdk21` image's own `gradle` (not `./gradlew`) to avoid downloading a distribution, and `.dockerignore` keeps the build context small (`build/`, `.gradle/`, `.git/`, `tools/` excluded). Compose passes container-network hostnames (`mysql`, `redis`, `kafka:9092`, `mqtt`, `clickhouse`, `eureka`) into each service via env vars that override the `localhost` defaults in `application.yml`.
- `tools/device-simulator/simulate_device.py` — **not part of the Gradle build.** Python + `paho-mqtt`, simulates one device publishing telemetry (jitter, incrementing `seq`, occasional threshold breach, optional `--repeat-seq` to demo dedupe). Can self-register via `--register --ingest-url ...` (POSTs to `/v1/devices/register` with stdlib `urllib`, no extra dependency) instead of requiring pre-existing credentials — there's no real device to provision from, so the simulator provisions itself the same way an owner would. `run_fleet.sh` launches N self-registered instances in parallel to simulate multiple devices at once.

## Cross-module contract: DTOs are duplicated, not shared

`hufng-ingest`'s `DeviceTelemetryMessage`/`hufng-report`'s `DeviceTelemetryMessage`, and `hufng-report`'s `ResourceAlertMessage`/`hufng-notification`'s `ResourceAlertMessage`, are **separate classes in separate packages** with no shared jar between producer and consumer modules. The JSON shape on the Kafka topic is the actual contract — if you add/rename a field on one side, update the mirrored DTO on the other side too (each file has a comment noting what it mirrors). Same for `AlertChannel` (`hufng-report`) / `NotificationChannel` (`hufng-notification`) — same enum constant names, serialized as the JSON string Jackson expects on the other side.

## Simplifications (intentional, out of scope for this iteration)

- `POST /v1/devices/register` has no auth — `ownerId` is taken directly from the request body. A fuller system would resolve it from a JWT/session (like `hufng-auth` in `hufng-pet`), which this repo deliberately doesn't depend on.
- No `hufng-gateway`/`hufng-auth` copied in — services are reached directly by port, no routing/JWT layer.
- Alert rules are config-based (`application.yml`), not a DB table (see `hufng-report` above).
- No git repo has been initialized for this project yet.

## Common commands

Requires JDK 21 (`JAVA_HOME` must point at it — Gradle 8.6 itself runs on that JVM and the toolchain resolves to it). Use the wrapper (`./gradlew`, pinned to Gradle 8.6) or a local Gradle 8.6.

- Build everything (compile + test + bootJars): `./gradlew build`
- Build a single module (and `hufng-common`): `./gradlew :hufng-ingest:build`
- Run all tests: `./gradlew test`
- Run tests for one module: `./gradlew :hufng-notification:test`
- Run a single test class / method: `./gradlew :hufng-notification:test --tests ProcessedEventServiceTest` (or `--tests '*ProcessedEventServiceTest.someMethod'`)
- Coverage: JaCoCo is applied to every subproject and `test` is `finalizedBy` `jacocoTestReport`, so any test run writes `<module>/build/reports/jacoco/test/html/index.html` + `jacocoTestReport.xml`. Run explicitly with `./gradlew jacocoTestReport` (or `:<module>:jacocoTestReport`). Aggregated report for all modules: `./gradlew jacocoRootReport` → `build/reports/jacoco/jacocoRootReport/html/index.html` (+ XML). It is a hand-rolled `JacocoReport` in the root `build.gradle.kts` that merges each module's `test.exec`/sources/classes — the built-in `jacoco-report-aggregation` plugin doesn't work here because it resolves module dependency graphs from the root project, which has no Spring BOM.
- Run one executable service locally: `./gradlew :hufng-ingest:bootRun` (or `hufng-report`/`hufng-notification`). No install step needed — `hufng-common` is a project dependency, not resolved from a local repo. Never run a bare `./gradlew bootRun`: it would start all 3 services in one invocation.
- Infra (Eureka + MQTT broker + ClickHouse + MySQL + Redis + Kafka, and the 3 app services): `cd infra && docker compose up --build -d`. Comment out the 3 app services in `docker-compose.yml` if you'd rather run them locally via `spring-boot:run` against Docker-only infra.
- `./gradlew test` never needs Docker running: H2 (MySQL mode) replaces MySQL, `mqtt.enabled: false` / `eureka.client.enabled: false` / `spring.kafka.listener.auto-startup: false` in each module's test `application.yml`, and `hufng-report`'s test yml excludes `DataSourceAutoConfiguration` outright since there's no fake-but-connectable ClickHouse substitute. This is exactly what `.github/workflows/ci.yml` relies on to run `./gradlew clean test` with no services running on the runner; a second job in that workflow builds each service's Docker image (matrix over `hufng-ingest`/`hufng-report`/`hufng-notification`) to catch Dockerfile regressions.
- Demo without real hardware: `./tools/device-simulator/run_fleet.sh 3` self-registers N devices against `hufng-ingest` and publishes telemetry over MQTT for all of them in parallel.

## Ports & context-path

| Service | Port | Context-path | Notes |
|---|---|---|---|
| `hufng-ingest` | 8084 | *(none)* | `POST /v1/devices/register`; MQTT bridge, no other HTTP surface |
| `hufng-report` | 8083 | *(none)* | consumes `DEVICE_TELEMETRY`, produces `RESOURCE_ALERT` |
| `hufng-notification` | 8082 | `/notification/api` | consumes `RESOURCE_ALERT` |
| `eureka` (Docker) | 8761 | *(none)* | dashboard + `/actuator/health` at root |
| `mqtt` / EMQX (Docker) | 1883 (MQTT), 18083 (dashboard) | — | shared subscription load-balances across `hufng-ingest` instances |
| `clickhouse` (Docker) | 8123 (HTTP/JDBC), 9000 (native) | — | `telemetry` table |

Package convention: `com.hufng.<module_name>` (no `iot`/`pet` segment), same as `hufng-pet`.

New modules are added the same way as in `hufng-pet`: create a subdirectory with its own `build.gradle.kts`, add it to `include(...)` in `settings.gradle.kts`, and depend on it via `implementation(project(":<name>"))`. Only modules with a `@SpringBootApplication` entry point apply `id("org.springframework.boot")` (and disable the plain `jar` task so `build/libs` holds just the bootJar the Dockerfile copies); `hufng-common` stays a plain `java-library` jar whose dependencies are declared `api` so they flow transitively to services, matching the old Maven compile scope.
