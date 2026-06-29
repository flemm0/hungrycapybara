package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.helper.RestaurantDatabase
import org.hungrycapybara.ordersimulator.model.MenuInteractionEvent
import org.hungrycapybara.ordersimulator.model.MenuInteractionEventType.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Random

final class MenuInteractionEventGenerator(
    restaurantDatabase: RestaurantDatabase,
    sessionInteractionStore: SessionInteractionStore
) extends EventGenerator:
  type Event = MenuInteractionEvent

  override protected val name: String = "menu-interaction"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(2, 10).seconds)

  private def randomSessionIdentity(): (Option[String], String, String) =
    sessionInteractionStore
      .randomActiveSession()
      .map(s => (Some(s.sessionId), s.sessionId, s.customerId))
      .getOrElse {
        val sessionId = UUID.randomUUID().toString
        (None, sessionId, UUID.randomUUID().toString)
      }

  private def randomMenuInteractionEvent(): MenuInteractionEvent =
    val eventType = Random.nextInt(3) match
      case 0 => MenuViewed
      case 1 => ItemViewed
      case 2 => ItemFavorited

    val (trackedSessionId, sessionId, customerId) = randomSessionIdentity()

    val restaurantId = trackedSessionId
      .flatMap(sessionInteractionStore.snapshot)
      .flatMap(_.restaurantId)
      .orElse(restaurantDatabase.randomRestaurant().map(_.restaurantId))
      .getOrElse(UUID.randomUUID().toString)

    trackedSessionId.foreach(sessionInteractionStore.assignRestaurant(_, restaurantId))

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
      sessionId = sessionId,
      customerId = customerId,
      restaurantId = restaurantId,
      menuItemId = menuItemId,
      viewDurationMs = viewDurationMs
    )

  override protected def generateEvent(using EventGenerator.Context): IO[MenuInteractionEvent] =
    IO.delay(randomMenuInteractionEvent())

object MenuInteractionEventGenerator:
  def apply(
      restaurantDatabase: RestaurantDatabase,
      sessionInteractionStore: SessionInteractionStore
  ): MenuInteractionEventGenerator =
    new MenuInteractionEventGenerator(restaurantDatabase, sessionInteractionStore)
