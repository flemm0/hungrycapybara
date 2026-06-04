package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.EventGenerator
import org.hungrycapybara.ordersimulator.model.RestaurantBrowseEvent
import org.hungrycapybara.ordersimulator.model.RestaurantBrowseEventType.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Random

object RestaurantBrowseEventGenerator extends EventGenerator:
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

  def randomRestaurantBrowseEvent(): RestaurantBrowseEvent =
    val eventType = Random.nextInt(4) match
      case 0 => RestaurantImpression
      case 1 => RestaurantClick
      case 2 => SearchPerformed
      case 3 => CuisineFilterApplied

    val restaurantId =
      eventType match
        case SearchPerformed => None
        case _               => Some(UUID.randomUUID().toString)

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
      sessionId = UUID.randomUUID().toString,
      customerId = UUID.randomUUID().toString,
      restaurantId = restaurantId,
      searchQuery = searchQuery,
      feedRank = Random.between(1, 51),
      deliveryEtaMinutes = Random.between(10, 61)
    )

  override protected def generateEvent(using EventGenerator.Context): IO[RestaurantBrowseEvent] =
    IO.delay(randomRestaurantBrowseEvent())
