# TODO: migrate logging stack (EFK) to Kubernetes

Scratchpad for when this repo moves to k8s. Not implemented yet — this
is a prompt-later checklist, not a spec. Ask Claude to "gen k8s manifests
theo infra/logging/TODO_K8S.md" when that work starts.

## What exists today (docker compose, `infra/logging/`)

- `fluent-bit.conf` / `parsers.conf` / `extract_container_id.lua` —
  collector config, tails `/var/lib/docker/containers/*/*.log` on a
  single host.
- `elasticsearch` + `kibana` — single-node, no auth
  (`xpack.security.enabled: false`), fixed heap sizes, `mem_limit` caps.
- State (`fluent-bit-db` volume, `tail.db`) and index data (`es-data`
  volume) are plain Docker named volumes.

See `README.md` in this directory for the full current flow diagram and
resource budget reasoning before touching any of this.

## Known gaps to close for k8s

- [ ] **Fluent Bit → DaemonSet.** One pod per node. Swap the
      `Path /var/lib/docker/containers/*/*.log` tail input for the
      standard k8s pattern: tail `/var/log/containers/*.log`
      (symlinks into `/var/log/pods/.../*.log`) via a `hostPath` mount,
      and add the `kubernetes` filter plugin (not available/needed in
      plain Docker) to enrich with pod/namespace/labels instead of the
      current `extract_container_id.lua` regex hack.
- [ ] **Decide: raw manifests vs Helm vs ECK operator** for
      Elasticsearch + Kibana. ECK (Elastic Cloud on Kubernetes operator)
      is the standard choice if this cluster already runs other
      operators — handles TLS, scaling, upgrades. Otherwise the
      `elastic/elasticsearch` + `elastic/kibana` Helm charts are the
      lighter-weight option.
- [ ] **Turn on auth.** `xpack.security.enabled: false` is a dev-only
      shortcut here — a shared cluster needs it on, plus a Kibana
      service account and Fluent Bit output credentials
      (`HTTP_User`/`HTTP_Passwd` on the `es` output, or an API key).
- [ ] **Persistent storage.** `es-data` becomes a `PersistentVolumeClaim`
      (StorageClass depends on the cluster) instead of a local Docker
      volume — decide retention/size before provisioning.
- [ ] **Resource requests/limits.** Port the `mem_limit` values in
      `docker-compose.yml` to pod `resources.requests`/`limits`, and
      revisit them — a k8s node's memory pressure behavior differs from
      a single Docker host's OOM-killer.
- [ ] **Multi-node log volume.** Right now this collects from one
      machine. On a real cluster, decide index lifecycle / ILM policy
      (rollover, retention) — `docker-logs-*` daily indices with no
      retention policy will grow unbounded.
- [ ] **`Read_from_Head On` behavior on pod restart.** Confirm whether
      the DaemonSet's `tail.db` equivalent persists across pod restarts
      (needs a `hostPath` or `emptyDir` decision) — without it, every
      pod restart re-backfills full node log history, same class of
      problem this repo already hit once (see README's Backfill
      section).

## Non-goals for the first k8s pass

- Don't try to preserve `container_id`-based filtering — pod/namespace
  labels from the `kubernetes` filter are the k8s-native equivalent and
  strictly more useful.
- Don't attempt a live migration of existing `es-data` from the Docker
  Compose deployment — this is dev/debugging log data, fine to start
  fresh on k8s.
