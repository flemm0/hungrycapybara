package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.model.{CartEvent, CartItem}
import org.hungrycapybara.ordersimulator.model.CartEventType.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Random

object CartEventGenerator extends EventGenerator:
  type Event = CartEvent

  override protected val name: String = "cart"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(3, 12).seconds)

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

  def randomCartItem(): CartItem =
    CartItem(
      itemId = UUID.randomUUID().toString,
      name = itemNames(Random.nextInt(itemNames.size)),
      price =
        BigDecimal(Random.between(4.0, 35.0)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
      quantity = Random.between(1, 5)
    )

  def randomCartEvent(): CartEvent =
    val eventType = Random.nextInt(6) match
      case 0 => CartCreated
      case 1 => ItemAddedToCart
      case 2 => ItemRemovedFromCart
      case 3 => CartViewed
      case 4 => CartAbandoned
      case 5 => CheckoutStarted

    val cartItems =
      eventType match
        case CartCreated =>
          Some(List.empty)
        case ItemAddedToCart | ItemRemovedFromCart | CartViewed | CartAbandoned | CheckoutStarted =>
          Some(List.fill(Random.between(1, 5))(randomCartItem()))

    CartEvent(
      eventId = s"evt_${Random.between(1000, 10000)}",
      eventType = eventType,
      eventTs = Instant.now(),
      sessionId = UUID.randomUUID().toString,
      customerId = UUID.randomUUID().toString,
      restaurantId = UUID.randomUUID().toString,
      cartItems = cartItems
    )

  override protected def generateEvent(using EventGenerator.Context): IO[CartEvent] =
    IO.delay(randomCartEvent())
