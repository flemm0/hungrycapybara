package org.hungrycapybara.ordersimulator.model

import java.time.Instant

enum RestaurantBrowseEventType:
  case RestaurantImpression
  case RestaurantClick
  case SearchPerformed
  case CuisineFilterApplied

case class RestaurantBrowseEvent(
  eventId: String,
  eventType: RestaurantBrowseEventType,
  eventTs: Instant,
  sessionId: String,
  customerId: String,
  restaurantId: Option[String],
  searchQuery: Option[String],
  feedRank: Int,
  deliveryEtaMinutes: Int
)
