package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.EventGenerator
import org.hungrycapybara.ordersimulator.model.MenuInteractionEvent
import org.hungrycapybara.ordersimulator.model.MenuInteractionEventType.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Random

object MenuInteractionEventGenerator extends EventGenerator:
  type Event = MenuInteractionEvent

  override protected val name: String = "menu-interaction"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(2, 10).seconds)

  def randomMenuInteractionEvent(): MenuInteractionEvent =
    val eventType = Random.nextInt(3) match
      case 0 => MenuViewed
      case 1 => ItemViewed
      case 2 => ItemFavorited

    val menuItemId =
      eventType match
        case MenuViewed => None
        case _          => Some(UUID.randomUUID().toString)

    val viewDurationMs =
      eventType match
        case MenuViewed | ItemViewed => Some(Random.between(1_000L, 120_001L))
        case ItemFavorited           => None

    MenuInteractionEvent(
      eventId = s"evt_${Random.between(1000, 10000)}",
      eventType = eventType,
      eventTs = Instant.now(),
      sessionId = UUID.randomUUID().toString,
      customerId = UUID.randomUUID().toString,
      restaurantId = UUID.randomUUID().toString,
      menuItemId = menuItemId,
      viewDurationMs = viewDurationMs
    )

  override protected def generateEvent(using EventGenerator.Context): IO[MenuInteractionEvent] =
    IO.delay(randomMenuInteractionEvent())
