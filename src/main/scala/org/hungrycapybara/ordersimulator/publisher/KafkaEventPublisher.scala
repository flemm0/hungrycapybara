package org.hungrycapybara.ordersimulator.publisher

import org.hungrycapybara.ordersimulator.core.EventPublisher
import cats.effect.IO

object KafkaEventPublisher extends EventPublisher:
  override def publish[A](streamName: String, key: String, event: A): IO[Unit] =
    IO.raiseError(
      new UnsupportedOperationException(
        s"KafkaEventPublisher is not implemented yet. streamName=$streamName, key=$key"
      )
    )
