#!/usr/bin/env bash
set -euo pipefail

# Gracefully stop the Kafka broker container.
docker compose stop kafka-ui kafka

echo
echo "Kafka broker and Kafka UI have been stopped."
echo "Current Kafka-related container status:"
docker compose ps kafka kafka-ui kafka-topic-init
