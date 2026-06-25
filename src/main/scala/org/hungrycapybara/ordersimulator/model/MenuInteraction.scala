package org.hungrycapybara.ordersimulator.model

import java.time.Instant

enum MenuInteractionEventType:
  case MenuViewed
  case ItemViewed
  case ItemFavorited

case class MenuInteractionEvent(
    eventId: String,
    eventType: MenuInteractionEventType,
    eventTs: Instant,
    sessionId: String,
    customerId: String,
    restaurantId: String,
    menuItemId: Option[String],
    viewDurationMs: Option[Long]
)
