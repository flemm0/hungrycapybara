package org.hungrycapybara.ordersimulator.generators

import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.model.CustomerSessionEvent
import org.hungrycapybara.ordersimulator.model.CustomerSessionEventType.*
import scala.concurrent.duration.*
import scala.util.Random
import cats.effect.IO

object CustomerSessionEventGenerator extends EventGenerator:
  type Event = CustomerSessionEvent

  override protected val name: String = "customer-session"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(5, 15).seconds)

  def randomCustomerSessionEvent(): CustomerSessionEvent =
    val eventId = java.util.UUID.randomUUID().toString
    val eventType = Random.nextInt(2) match
      case 0 => SessionStarted
      case 1 => SessionEnded
    val eventTs = java.time.Instant.now()
    val customerId = java.util.UUID.randomUUID().toString
    val sessionId = java.util.UUID.randomUUID().toString
    val deviceType = if Random.nextBoolean() then "mobile" else "desktop"
    val appVersion = s"1.${Random.nextInt(10)}.${Random.nextInt(100)}"
    val entryPoint = if Random.nextBoolean() then "homepage" else "push_notification"

    CustomerSessionEvent(
      eventId = eventId,
      eventType = eventType,
      eventTs = eventTs,
      customerId = customerId,
      sessionId = sessionId,
      deviceType = deviceType,
      appVersion = appVersion,
      entryPoint = entryPoint
    )

  override protected def generateEvent(using EventGenerator.Context): IO[CustomerSessionEvent] =
    IO.delay(randomCustomerSessionEvent())
