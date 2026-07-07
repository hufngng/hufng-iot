#!/usr/bin/env python3
"""Simulates an IoT device publishing telemetry over MQTT.

Not part of the Maven build -- a standalone demo tool, for when there's no
real hardware to test against. Requires `paho-mqtt` (`pip install paho-mqtt`);
everything else is Python stdlib.

Two ways to run it:

1. Bring your own credentials (from a prior POST /v1/devices/register):
    python3 simulate_device.py --device-id sensor-abc123 --secret sk-xxxx

2. Self-register against a running hufng-ingest and use the returned
   credentials directly (no manual curl step):
    python3 simulate_device.py --register --ingest-url http://localhost:8084

Run several instances in parallel to simulate multiple devices at once --
see run_fleet.sh for a helper that does this with N self-registered devices.
"""
import argparse
import json
import random
import time
import urllib.request
from datetime import datetime, timezone

import paho.mqtt.client as mqtt


def parse_args():
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--device-id", help="skip if --register is set")
    parser.add_argument("--secret", help="skip if --register is set")
    parser.add_argument("--register", action="store_true",
                         help="POST /v1/devices/register against --ingest-url first")
    parser.add_argument("--ingest-url", default="http://localhost:8084",
                         help="used only with --register")
    parser.add_argument("--device-name", default="simulated-device")
    parser.add_argument("--model", default="DHT22")
    parser.add_argument("--owner-id", type=int, default=1)
    parser.add_argument("--broker", default="localhost")
    parser.add_argument("--port", type=int, default=1883)
    parser.add_argument("--metric", default="temperature")
    parser.add_argument("--baseline", type=float, default=20.0)
    parser.add_argument("--stddev", type=float, default=5.0)
    parser.add_argument("--jitter-min", type=float, default=1.0)
    parser.add_argument("--jitter-max", type=float, default=3.0)
    parser.add_argument("--count", type=int, default=0, help="0 = run forever")
    parser.add_argument("--repeat-seq", type=int, default=None,
                         help="occasionally resend this seq to demo dedupe")
    args = parser.parse_args()

    if args.register:
        args.device_id, args.secret = register_device(args)
    elif not args.device_id or not args.secret:
        parser.error("either pass --device-id/--secret, or use --register")

    return args


def register_device(args):
    """No real device exists yet -- provision one the same way a device owner
    would, then use the credentials returned exactly once."""
    payload = json.dumps({
        "deviceName": args.device_name,
        "model": args.model,
        "ownerId": args.owner_id,
    }).encode()
    request = urllib.request.Request(
        f"{args.ingest_url}/v1/devices/register",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request) as response:
        body = json.loads(response.read())
    print(f"Registered device {body['deviceId']} (status={body['status']})")
    return body["deviceId"], body["deviceSecret"]


def main():
    args = parse_args()
    client = mqtt.Client(client_id=f"sim-{args.device_id}")
    client.connect(args.broker, args.port)
    client.loop_start()

    topic = f"device/{args.device_id}/telemetry"
    seq = 1
    published = 0

    try:
        while args.count == 0 or published < args.count:
            use_seq = seq
            if args.repeat_seq is not None and published > 0 and published % 10 == 0:
                use_seq = args.repeat_seq
                print(f"[{args.device_id}] resending seq={use_seq} to demo dedupe")

            value = args.baseline + random.gauss(0, args.stddev)
            message = {
                "deviceId": args.device_id,
                "seq": use_seq,
                "eventTime": datetime.now(timezone.utc).isoformat(),
                "metric": args.metric,
                "value": round(value, 2),
                "auth": args.secret,
            }
            client.publish(topic, json.dumps(message), qos=1)
            print(f"[{args.device_id}] seq={use_seq} {args.metric}={message['value']}")

            if use_seq == seq:
                seq += 1
            published += 1
            time.sleep(random.uniform(args.jitter_min, args.jitter_max))
    except KeyboardInterrupt:
        pass
    finally:
        client.loop_stop()
        client.disconnect()


if __name__ == "__main__":
    main()
