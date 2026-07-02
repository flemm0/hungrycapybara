package org.hungrycapybara.ordersimulator.generators

import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.model.{Restaurant, RestaurantCatalogEvent, MenuItem}
import org.hungrycapybara.ordersimulator.model.RestaurantCatalogEventType.*
import org.hungrycapybara.ordersimulator.helper.RestaurantDatabase
import org.hungrycapybara.ordersimulator.helper.SeedData
import cats.effect.IO
import scala.concurrent.duration.*
import scala.util.Random
import java.util.UUID

final class RestaurantCatalogEventGenerator(restaurantDatabase: RestaurantDatabase) extends EventGenerator:
  type Event = RestaurantCatalogEvent

  override protected val name: String = "restaurant-catalog"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(20, 60).seconds)

  private def randomRestaurantUpdate(restaurant: Restaurant): Restaurant =
    SeedData.randomRestaurant().copy(restaurantId = restaurant.restaurantId)

  def randomMenuItem(restaurant: Restaurant): MenuItem =
    val itemId = UUID.randomUUID().toString
    val name = SeedData.randomMenuItemName(restaurant.cuisineTypes.headOption)
    val basePrice = Random.between(5.0, 50.0)
    val available = Random.nextBoolean()
    MenuItem(itemId, name, basePrice, available)

  def randomRestaurantCatalogEvent(): RestaurantCatalogEvent =
    val eventId = UUID.randomUUID().toString
    val eventType = Random.nextInt(4) match
      case 0 => RestaurantCreated
      case 1 => RestaurantUpdated
      case 2 => MenuUpdated
      case 3 => ItemAvailabilityChanged
    val eventTs = java.time.Instant.now()
    val restaurant =
      eventType match
        case RestaurantCreated =>
          restaurantDatabase.create(SeedData.randomRestaurant())
        case RestaurantUpdated =>
          restaurantDatabase
            .updateRandom(randomRestaurantUpdate)
            .getOrElse(restaurantDatabase.create(SeedData.randomRestaurant()))
        case MenuUpdated | ItemAvailabilityChanged =>
          restaurantDatabase
            .randomRestaurant()
            .getOrElse(restaurantDatabase.create(SeedData.randomRestaurant()))
    val menuItem =
      if eventType == MenuUpdated || eventType == ItemAvailabilityChanged then
        Some(randomMenuItem(restaurant))
      else None
    RestaurantCatalogEvent(
      eventId = eventId,
      eventType = eventType,
      eventTs = eventTs,
      restaurant = restaurant,
      menuItem = menuItem
    )

  override protected def generateEvent(using EventGenerator.Context): IO[RestaurantCatalogEvent] =
    IO.delay(randomRestaurantCatalogEvent())

object RestaurantCatalogEventGenerator:
  def apply(restaurantDatabase: RestaurantDatabase): RestaurantCatalogEventGenerator =
    new RestaurantCatalogEventGenerator(restaurantDatabase)
