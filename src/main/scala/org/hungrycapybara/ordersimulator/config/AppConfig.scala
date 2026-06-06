package org.hungrycapybara.ordersimulator.config

import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment

final case class AppConfig(
  environment: ExecutionEnvironment,
  kafka: Option[KafkaConfig]
)

final case class KafkaConfig(
  bootstrapServers: String,
  clientId: String,
  topicPrefix: String
)
