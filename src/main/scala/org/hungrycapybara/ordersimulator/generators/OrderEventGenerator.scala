package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.model.{CartItem, Order, OrderEvent}
import org.hungrycapybara.ordersimulator.model.OrderEventType.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Random

object OrderEventGenerator extends EventGenerator:
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

  def randomOrderItem(): CartItem =
    CartItem(
      itemId = UUID.randomUUID().toString,
      name = itemNames(Random.nextInt(itemNames.size)),
      price = roundCurrency(Random.between(4.0, 35.0)),
      quantity = Random.between(1, 5)
    )

  def randomOrder(): Order =
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
      customerId = UUID.randomUUID().toString,
      restaurantId = UUID.randomUUID().toString,
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

  def randomOrderEvent(): OrderEvent =
    val eventType = Random.nextInt(5) match
      case 0 => OrderCreated
      case 1 => PaymentAuthorized
      case 2 => OrderConfirmed
      case 3 => OrderCancelled
      case 4 => OrderCompleted

    OrderEvent(
      eventId = s"evt_${Random.between(1000, 10000)}",
      eventType = eventType,
      eventTs = Instant.now(),
      order = randomOrder()
    )

  override protected def generateEvent(using EventGenerator.Context): IO[OrderEvent] =
    IO.delay(randomOrderEvent())
