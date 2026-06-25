package org.hungrycapybara.ordersimulator.config

import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment

final case class AppConfig(
    environment: ExecutionEnvironment,
    kafka: Option[KafkaConfig],
    initialCustomerCount: Int,
    initialRestaurantCount: Int
)

object AppConfig:
  val DefaultInitialCustomerCount: Int = 1_000_000
  val DefaultInitialRestaurantCount: Int = 10_000

final case class KafkaConfig(
    bootstrapServers: String,
    clientId: String,
    topicPrefix: String
)
