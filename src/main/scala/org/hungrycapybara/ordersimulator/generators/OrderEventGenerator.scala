package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.helper.RestaurantDatabase
import org.hungrycapybara.ordersimulator.model.{CartItem, Order, OrderEvent}
import org.hungrycapybara.ordersimulator.model.OrderEventType
import org.hungrycapybara.ordersimulator.model.OrderEventType.*

import java.time.Instant
import java.util.UUID
import scala.collection.mutable
import scala.concurrent.duration.*
import scala.util.Random

final class OrderEventGenerator(
    restaurantDatabase: RestaurantDatabase,
    sessionInteractionStore: SessionInteractionStore
) extends EventGenerator:
  type Event = OrderEvent

  override protected val name: String = "order"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(5, 15).seconds)

  private val itemNames: Vector[String] = Vector(
    "Pad Thai",
    "Margherita Pizza",
    "Chicken Tikka Masala",
    "Veggie Burrito",
    "Pork Ramen",
    "Falafel Bowl",
    "Cheeseburger",
    "Salmon Sushi Roll",
    "Caesar Salad",
    "Birria Tacos"
  )

  private def roundCurrency(value: Double): Double =
    BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

  private val orderStateBySession: mutable.Map[String, SessionOrderState] = mutable.Map.empty

  private def randomOrderItem(): CartItem =
    CartItem(
      itemId = UUID.randomUUID().toString,
      name = itemNames(Random.nextInt(itemNames.size)),
      price = roundCurrency(Random.between(4.0, 35.0)),
      quantity = Random.between(1, 5)
    )

  private def randomOrder(customerId: String, restaurantId: String): Order =
    val items = List.fill(Random.between(1, 6))(randomOrderItem())
    val subtotal = roundCurrency(items.map(item => item.price * item.quantity).sum)
    val tax = roundCurrency(subtotal * 0.0875)
    val deliveryFee = roundCurrency(Random.between(0.0, 7.0))
    val serviceFee = roundCurrency(subtotal * 0.12)
    val smallOrderFee = if subtotal < 12.0 then 2.0 else 0.0
    val tip = roundCurrency(subtotal * Random.between(0.1, 0.25))
    val total = roundCurrency(subtotal + tax + deliveryFee + serviceFee + smallOrderFee + tip)

    Order(
      orderId = UUID.randomUUID().toString,
      customerId = customerId,
      restaurantId = restaurantId,
      cartId = UUID.randomUUID().toString,
      items = items,
      subtotal = subtotal,
      tax = tax,
      deliveryFee = deliveryFee,
      serviceFee = serviceFee,
      smallOrderFee = smallOrderFee,
      tip = tip,
      total = total
    )

  private def nextEventType(current: OrderEventType): OrderEventType =
    current match
      case OrderCreated =>
        if Random.nextDouble() < 0.9 then PaymentAuthorized else OrderCancelled
      case PaymentAuthorized =>
        if Random.nextDouble() < 0.92 then OrderConfirmed else OrderCancelled
      case OrderConfirmed =>
        OrderCompleted
      case OrderCancelled | OrderCompleted =>
        OrderCreated

  private def randomFallbackOrderEvent(): OrderEvent =
    val customerId = UUID.randomUUID().toString
    val restaurantId = restaurantDatabase
      .randomRestaurant()
      .map(_.restaurantId)
      .getOrElse(UUID.randomUUID().toString)

    OrderEvent(
      eventId = s"evt_${Random.between(1000, 10000)}",
      eventType = OrderCreated,
      eventTs = Instant.now(),
      order = randomOrder(customerId, restaurantId)
    )

  private def randomOrderEvent(): OrderEvent =
    val now = Instant.now()

    val inProgressSessionIds = orderStateBySession.keys.toVector
      .filter(sessionId => sessionInteractionStore.snapshot(sessionId).nonEmpty)

    val inProgressSessionId = Option.when(inProgressSessionIds.nonEmpty) {
      inProgressSessionIds(Random.nextInt(inProgressSessionIds.size))
    }

    val fromInProgress =
      if inProgressSessionId.nonEmpty && Random.nextDouble() < 0.75 then inProgressSessionId
      else None

    fromInProgress
      .flatMap(sessionId =>
        orderStateBySession.get(sessionId).map { state =>
          val eventType = nextEventType(state.lastEventType)
          val nextState = SessionOrderState(order = state.order, lastEventType = eventType)

          if eventType == OrderCancelled || eventType == OrderCompleted then
            orderStateBySession.remove(sessionId)
            sessionInteractionStore.clearCheckout(sessionId)
            sessionInteractionStore.recordCartAbandoned(sessionId)
          else orderStateBySession.update(sessionId, nextState)

          OrderEvent(
            eventId = s"evt_${Random.between(1000, 10000)}",
            eventType = eventType,
            eventTs = now,
            order = state.order
          )
        }
      )
      .orElse {
        sessionInteractionStore.randomCheckoutReadySession().map { snapshot =>
          val restaurantId = snapshot.restaurantId
            .orElse(restaurantDatabase.randomRestaurant().map(_.restaurantId))
            .getOrElse(UUID.randomUUID().toString)

          val order = randomOrder(snapshot.customerId, restaurantId)
          orderStateBySession.update(
            snapshot.sessionId,
            SessionOrderState(order = order, lastEventType = OrderCreated)
          )

          OrderEvent(
            eventId = s"evt_${Random.between(1000, 10000)}",
            eventType = OrderCreated,
            eventTs = now,
            order = order
          )
        }
      }
      .getOrElse(randomFallbackOrderEvent())

  override protected def generateEvent(using EventGenerator.Context): IO[OrderEvent] =
    IO.delay(randomOrderEvent())

object OrderEventGenerator:
  def apply(
      restaurantDatabase: RestaurantDatabase,
      sessionInteractionStore: SessionInteractionStore
  ): OrderEventGenerator =
    new OrderEventGenerator(restaurantDatabase, sessionInteractionStore)

private final case class SessionOrderState(
    order: Order,
    lastEventType: OrderEventType
)
