package org.hungrycapybara.ordersimulator.model

import java.time.Instant

enum RestaurantCatalogEventType:
  case RestaurantCreated
  case RestaurantUpdated
  case MenuUpdated
  case ItemAvailabilityChanged

enum PriceRange:
  case Low, Medium, High, Premium

case class Restaurant(
  restaurantId: String,
  name: String,
  cuisineTypes: List[String],
  rating: Double,
  priceRange: PriceRange
)

case class MenuItem(
  itemId: String,
  name: String,
  basePrice: Double,
  available: Boolean
)

case class RestaurantCatalogEvent(
  eventId: String,
  eventType: RestaurantCatalogEventType,
  eventTs: Instant,
  restaurant: Restaurant,
  menuItem: Option[MenuItem]
)
