package org.hungrycapybara.ordersimulator.model

import java.time.Instant

enum OrderEventType:
  case OrderCreated
  case PaymentAuthorized
  case OrderConfirmed
  case OrderCancelled
  case OrderCompleted

case class Order(
    orderId: String,
    customerId: String,
    restaurantId: String,
    cartId: String,
    items: List[CartItem],
    subtotal: Double,
    tax: Double,
    deliveryFee: Double,
    serviceFee: Double,
    smallOrderFee: Double,
    tip: Double,
    total: Double
)

case class OrderEvent(
    eventId: String,
    eventType: OrderEventType,
    eventTs: Instant,
    order: Order
)
