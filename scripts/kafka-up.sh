#!/usr/bin/env bash
set -euo pipefail

docker compose up -d kafka
docker compose up kafka-topic-init
docker compose up -d kafka-ui

echo
echo "Kafka is available at localhost:9092"
echo "Kafka UI is available at http://localhost:8080"
echo "Configured topics:"
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
