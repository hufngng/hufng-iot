-- Telemetry read model. ReplacingMergeTree keeps the row with the highest
-- _version among duplicates sharing the same ORDER BY key (device_id, seq) --
-- but only at background merge time, not at write time. Duplicate rows are
-- eventually deduped, not never-written; querying "SELECT ... FROM telemetry
-- FINAL" (or argMax(...) ) forces a merge-consistent read when needed.
CREATE TABLE IF NOT EXISTS telemetry
(
    device_id      String,
    seq            UInt64,
    metric         LowCardinality(String),
    value          Float64,
    event_time     DateTime64(3),
    ingestion_time DateTime64(3),
    _version       UInt64 DEFAULT 1
) ENGINE = ReplacingMergeTree(_version)
ORDER BY (device_id, seq)
TTL toDateTime(event_time) + INTERVAL 7 DAY;
