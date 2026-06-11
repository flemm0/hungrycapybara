package org.hungrycapybara.ordersimulator.generators

import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.model.{RestaurantCatalogEvent, MenuItem}
import org.hungrycapybara.ordersimulator.model.RestaurantCatalogEventType.*
import org.hungrycapybara.ordersimulator.helper.SeedData
import cats.effect.IO
import scala.concurrent.duration.*
import scala.util.Random
import java.util.UUID

object RestaurantCatalogEventGenerator extends EventGenerator:
  type Event = RestaurantCatalogEvent

  override protected val name: String = "restaurant-catalog"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(20, 60).seconds)
  
  def randomMenuItem(): MenuItem =
    val itemId = UUID.randomUUID().toString
    val name = s"Menu Item ${Random.alphanumeric.take(5).mkString}"
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
    val restaurant = SeedData.randomRestaurant()
    val menuItem = 
      if eventType == MenuUpdated || eventType == ItemAvailabilityChanged then
        Some(randomMenuItem())
      else
        None
    RestaurantCatalogEvent(
      eventId = eventId,
      eventType = eventType,
      eventTs = eventTs,
      restaurant = restaurant,
      menuItem = menuItem
    )

  override protected def generateEvent(using EventGenerator.Context): IO[RestaurantCatalogEvent] =
    IO.delay(randomRestaurantCatalogEvent())
