#!/usr/bin/env bash
set -euo pipefail

docker compose up -d kafka
docker compose up kafka-topic-init

echo
echo "Kafka is available at localhost:9092"
echo "Configured topics:"
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
