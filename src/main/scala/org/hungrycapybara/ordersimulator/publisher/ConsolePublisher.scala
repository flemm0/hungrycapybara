package org.hungrycapybara.ordersimulator.publisher

import org.hungrycapybara.ordersimulator.core.EventPublisher
import cats.effect.IO

object ConsoleEventPublisher extends EventPublisher:
  override def publish[A](streamName: String, key: String, event: A): IO[Unit] =
    val payload = EventPayloadEncoder.encodeEnvelope(streamName, key, event)
    IO.println(payload.noSpaces)
