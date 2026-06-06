package org.hungrycapybara.ordersimulator.config

import cats.effect.unsafe.implicits.global
import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment

import java.nio.file.Files

class ConfigLoaderSuite extends munit.FunSuite:
  test("defaults to local environment without Kafka config") {
    val config = ConfigLoader.load(Nil).unsafeRunSync()

    assertEquals(config.environment, ExecutionEnvironment.Local)
    assertEquals(config.kafka, None)
  }

  test("loads Kafka config from YAML") {
    val path = Files.createTempFile("hungry-capybara-config", ".yaml")
    Files.writeString(
      path,
      """environment: staging
        |
        |kafka:
        |  bootstrapServers: localhost:9092
        |  clientId: test-client
        |  topicPrefix: test-topic
        |""".stripMargin
    )

    val config = ConfigLoader.load(List("--config", path.toString)).unsafeRunSync()

    assertEquals(config.environment, ExecutionEnvironment.Staging)
    assertEquals(
      config.kafka,
      Some(
        KafkaConfig(
          bootstrapServers = "localhost:9092",
          clientId = "test-client",
          topicPrefix = "test-topic"
        )
      )
    )
  }

  test("CLI args override YAML values") {
    val path = Files.createTempFile("hungry-capybara-config", ".yaml")
    Files.writeString(
      path,
      """environment: staging
        |
        |kafka:
        |  bootstrapServers: localhost:9092
        |  clientId: yaml-client
        |  topicPrefix: yaml-topic
        |""".stripMargin
    )

    val config = ConfigLoader
      .load(
        List(
          "--config",
          path.toString,
          "--env",
          "production",
          "--kafka-client-id",
          "cli-client"
        )
      )
      .unsafeRunSync()

    assertEquals(config.environment, ExecutionEnvironment.Production)
    assertEquals(config.kafka.map(_.clientId), Some("cli-client"))
    assertEquals(config.kafka.map(_.topicPrefix), Some("yaml-topic"))
  }

  test("requires Kafka config outside local environment") {
    val error = intercept[IllegalArgumentException] {
      ConfigLoader.load(List("--env", "staging")).unsafeRunSync()
    }

    assertEquals(error.getMessage, "Kafka config is required when environment is Staging")
  }
