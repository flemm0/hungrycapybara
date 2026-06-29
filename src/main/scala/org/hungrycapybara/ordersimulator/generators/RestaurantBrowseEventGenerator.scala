package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.helper.RestaurantDatabase
import org.hungrycapybara.ordersimulator.model.RestaurantBrowseEvent
import org.hungrycapybara.ordersimulator.model.RestaurantBrowseEventType.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Random

final class RestaurantBrowseEventGenerator(
    restaurantDatabase: RestaurantDatabase,
    sessionInteractionStore: SessionInteractionStore
) extends EventGenerator:
  type Event = RestaurantBrowseEvent

  override protected val name: String = "restaurant-browse"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(2, 8).seconds)

  private val searchQueries: Vector[String] = Vector(
    "thai noodles",
    "pizza near me",
    "sushi",
    "vegan bowls",
    "burger",
    "tacos",
    "ramen",
    "indian curry",
    "breakfast burrito",
    "bbq"
  )

  private def randomSessionIdentity(): (Option[String], String, String) =
    sessionInteractionStore
      .randomActiveSession()
      .map(s => (Some(s.sessionId), s.sessionId, s.customerId))
      .getOrElse {
        val sessionId = UUID.randomUUID().toString
        (None, sessionId, UUID.randomUUID().toString)
      }

  private def randomRestaurantBrowseEvent(): RestaurantBrowseEvent =
    val eventType = Random.nextInt(4) match
      case 0 => RestaurantImpression
      case 1 => RestaurantClick
      case 2 => SearchPerformed
      case 3 => CuisineFilterApplied

    val (trackedSessionId, sessionId, customerId) = randomSessionIdentity()

    val restaurantId =
      eventType match
        case SearchPerformed => None
        case _               =>
          val selected = trackedSessionId
            .flatMap(sessionInteractionStore.snapshot)
            .flatMap(_.restaurantId)
            .orElse(restaurantDatabase.randomRestaurant().map(_.restaurantId))
            .orElse(Some(UUID.randomUUID().toString))

          trackedSessionId.foreach { activeSessionId =>
            selected.foreach(sessionInteractionStore.assignRestaurant(activeSessionId, _))
          }

          selected

    val searchQuery =
      eventType match
        case RestaurantImpression | RestaurantClick | SearchPerformed =>
          Some(searchQueries(Random.nextInt(searchQueries.size)))
        case CuisineFilterApplied =>
          None

    RestaurantBrowseEvent(
      eventId = s"evt_${Random.between(1000, 10000)}",
      eventType = eventType,
      eventTs = Instant.now(),
      sessionId = sessionId,
      customerId = customerId,
      restaurantId = restaurantId,
      searchQuery = searchQuery,
      feedRank = Random.between(1, 51),
      deliveryEtaMinutes = Random.between(10, 61)
    )

  override protected def generateEvent(using EventGenerator.Context): IO[RestaurantBrowseEvent] =
    IO.delay(randomRestaurantBrowseEvent())

object RestaurantBrowseEventGenerator:
  def apply(
      restaurantDatabase: RestaurantDatabase,
      sessionInteractionStore: SessionInteractionStore
  ): RestaurantBrowseEventGenerator =
    new RestaurantBrowseEventGenerator(restaurantDatabase, sessionInteractionStore)
