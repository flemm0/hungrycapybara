#!/usr/bin/env bash
set -euo pipefail

# Gracefully stop the Kafka broker container.
docker compose stop kafka

echo
echo "Kafka broker has been stopped."
echo "Current Kafka-related container status:"
docker compose ps kafka kafka-topic-init
