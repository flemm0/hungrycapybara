package org.hungrycapybara.ordersimulator

import cats.effect.IO

trait EventPublisher:
  def publish[A](streamName: String, key: String, event: A): IO[Unit]
