package org.hungrycapybara.ordersimulator.model

import java.time.Instant

enum CartEventType:
  case CartCreated
  case ItemAddedToCart
  case ItemRemovedFromCart
  case CartViewed
  case CartAbandoned
  case CheckoutStarted

case class CartItem(
  itemId: String,
  name: String,
  price: Double,
  quantity: Int
)

case class CartEvent(
  eventId: String,
  eventType: CartEventType,
  eventTs: Instant,
  sessionId: String,
  customerId: String,
  restaurantId: String,
  cartItems: Option[List[CartItem]]
)
