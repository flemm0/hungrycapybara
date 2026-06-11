package org.hungrycapybara.ordersimulator.publisher

import cats.effect.IO
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.hungrycapybara.ordersimulator.config.KafkaConfig
import org.hungrycapybara.ordersimulator.core.EventPublisher

import java.util.Properties

final class KafkaEventPublisher(config: KafkaConfig) extends EventPublisher:
  private lazy val producer =
    val props = Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.bootstrapServers)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, config.clientId)
    props.put(ProducerConfig.ACKS_CONFIG, "all")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

    KafkaProducer[String, String](props)

  override def publish[A](streamName: String, key: String, event: A): IO[Unit] =
    val topic = s"${config.topicPrefix}.$streamName"
    val payload = EventPayloadEncoder.encodeEnvelope(streamName, key, event).noSpaces
    val record = ProducerRecord[String, String](topic, key, payload)

    IO.blocking(producer.send(record).get()).void

object KafkaEventPublisher:
  def apply(config: KafkaConfig): KafkaEventPublisher = new KafkaEventPublisher(config)
