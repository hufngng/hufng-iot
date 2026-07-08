# Logging stack (EFK)

Elasticsearch + Fluent Bit + Kibana, collecting logs from **every container
on this host** (not just `hufng-iot`'s) by tailing Docker's own
`json-file` log driver output on disk — no per-service logging config
needed anywhere else.

This is a **separate Compose project** from `../docker-compose.yml` on
purpose: independent lifecycle (start/stop without touching the app
stack) and independent resource budget, so it doesn't compete with the
app containers for RAM on the same machine.

EFK (not ELK) because Fluent Bit is what runs as the log collector on
Kubernetes too — a DaemonSet, one pod per node, tailing the node's log
files the same way it tails `/var/lib/docker/containers` here. Moving
this to k8s later only changes the deployment manifest (DaemonSet +
ConfigMap), not the collector config itself. See `TODO_K8S.md` for the
checklist of what that migration still needs.

## Flow

```
Every container on this host (hufng-iot-*, hufng-logging-* itself, ...)
      │  stdout/stderr, written by Docker's json-file log driver
      ▼
/var/lib/docker/containers/<id>/<id>-json.log   (on the host disk)
      │  bind-mounted read-only into fluent-bit
      ▼
fluent-bit  [INPUT] tail
      │  Path /var/lib/docker/containers/*/*.log
      │  Parser docker            -- parses {"log","stream","time"}
      │  Docker_Mode On           -- reassembles partial lines
      │  Read_from_Head On        -- backfills existing history once,
      │                              then only tails new lines
      │  DB /fluent-bit/tail.db   -- checkpoints per-file offset
      │  Mem_Buf_Limit 5MB        -- caps in-flight batch size
      ▼
fluent-bit  [FILTER] lua (extract_container_id.lua)
      │  reads record["log_file"] (full host path), regexes out the
      │  container id, adds record["container_id"] (first 12 chars)
      ▼
fluent-bit  [OUTPUT] es
      │  Index docker-logs, Logstash_Format On -> docker-logs-YYYY.MM.DD
      │  Generate_ID On           -- id = hash(record), so a retried
      │                              batch overwrites instead of duplicating
      ▼
elasticsearch :9200  (single-node, index docker-logs-*)
      ▼
kibana :5601  ->  Discover, data view "docker-logs*"
```

## Usage

```bash
cd infra/logging
docker compose up -d      # start
docker compose down       # stop (keeps data in the es-data volume)
docker compose down -v    # stop and wipe indexed logs
```

- Kibana: http://localhost:5601 — Discover → create a data view on the
  `docker-logs*` index pattern to browse logs.
- Elasticsearch API: http://localhost:9200

Each log document is enriched with `container_id` (first 12 chars),
extracted from the source file path by `extract_container_id.lua`, so
you can filter/group by container in Kibana.

### Backfill behavior (existing container logs, not just new ones)

`Read_from_Head On` (in `fluent-bit.conf`) makes Fluent Bit read each
container's **entire existing log file** on first sight, not just lines
written after it starts — so a fresh start picks up history for every
already-running container, e.g. `hufng-iot-*`, automatically.

This only happens **once per file**, tracked by the `DB` state file
(`/fluent-bit/tail.db`, persisted in the `fluent-bit-db` volume). Once a
file's offset is checkpointed, restarting `fluent-bit` only tails new
lines from where it left off — it will not re-read history again.

If you ever need a full re-backfill (e.g. after wiping `es-data`), you
must also wipe the state DB, otherwise Fluent Bit thinks it already read
everything and only new lines show up:

```bash
docker compose stop fluent-bit
docker compose rm -f -v fluent-bit
docker volume rm logging_fluent-bit-db
curl -X DELETE "http://localhost:9200/docker-logs-<pattern-matching-existing-index-names>"  # wildcard delete is blocked by default
docker compose up -d fluent-bit
```

The ES output also sets `Generate_ID On`, so document IDs are derived
from record content rather than random — if a large backfill batch gets
retried (e.g. due to a slow/oversized bulk response), the retry
overwrites the same IDs instead of creating duplicates.

## Resource budget

| Service       | mem_limit | Notes                                          |
|---------------|-----------|-------------------------------------------------|
| elasticsearch | 1.5g      | JVM heap pinned to 512m via `ES_JAVA_OPTS`; total container usage runs ~2x heap, hence the higher limit |
| kibana        | 1g        | Node heap capped at 768m via `NODE_OPTIONS`; needs >512m just to boot |
| fluent-bit    | 128m      | Lightweight tail + forward, no buffering of note |

If either `elasticsearch` or `kibana` gets OOM-killed after a config
change, check `docker stats` — the JVM/Node heap flags must stay
meaningfully below `mem_limit` to leave room for off-heap/native
overhead.

## Files

- `docker-compose.yml` — the three services + the `es-data` volume.
- `fluent-bit.conf` — tails `/var/lib/docker/containers/*/*.log` with
  `Docker_Mode On` (reassembles partial log lines), parses with the
  `docker` parser, ships to Elasticsearch under the `docker-logs-*`
  daily index pattern.
- `parsers.conf` — the `docker` parser definition (Docker's json-file
  log line format: `{"log":..., "stream":..., "time":...}`).
- `extract_container_id.lua` — Lua filter that pulls the 12-char
  container ID out of the tailed file's path.

## Prerequisites

Fluent Bit reads `/var/lib/docker/containers` and `/var/run/docker.sock`
directly (both mounted read-only) — this only works when Docker's log
driver is the default `json-file`. If a container overrides it (e.g.
`journald`, `syslog`), its logs won't show up here.
