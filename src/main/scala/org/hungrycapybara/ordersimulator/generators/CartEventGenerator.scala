package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.helper.RestaurantDatabase
import org.hungrycapybara.ordersimulator.model.{CartEvent, CartItem}
import org.hungrycapybara.ordersimulator.model.CartEventType
import org.hungrycapybara.ordersimulator.model.CartEventType.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Random

final class CartEventGenerator(
    restaurantDatabase: RestaurantDatabase,
    sessionInteractionStore: SessionInteractionStore
) extends EventGenerator:
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

  private def randomCartItem(): CartItem =
    CartItem(
      itemId = UUID.randomUUID().toString,
      name = itemNames(Random.nextInt(itemNames.size)),
      price =
        BigDecimal(Random.between(4.0, 35.0)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble,
      quantity = Random.between(1, 5)
    )

  private def randomStandaloneEventType(): CartEventType =
    Random.nextInt(4) match
      case 0 => CartCreated
      case 1 => ItemAddedToCart
      case 2 => CartViewed
      case 3 => CartAbandoned

  private def randomStatefulEventType(snapshot: SessionSnapshot): CartEventType =
    if snapshot.checkoutStarted then
      if Random.nextBoolean() then CartViewed else CartAbandoned
    else if snapshot.cartItemsCount <= 0 then
      Random.nextInt(3) match
        case 0 => CartCreated
        case 1 => ItemAddedToCart
        case _ => CartViewed
    else
      Random.nextInt(5) match
        case 0 => ItemAddedToCart
        case 1 => ItemRemovedFromCart
        case 2 => CartViewed
        case 3 => CartAbandoned
        case _ => CheckoutStarted

  private def randomCartEvent(): CartEvent =
    val sessionSnapshot = sessionInteractionStore.randomSnapshot()

    val eventType = sessionSnapshot
      .map(randomStatefulEventType)
      .getOrElse(randomStandaloneEventType())

    val sessionId = sessionSnapshot.map(_.sessionId).getOrElse(UUID.randomUUID().toString)
    val customerId = sessionSnapshot.map(_.customerId).getOrElse(UUID.randomUUID().toString)

    val restaurantId = sessionSnapshot
      .flatMap(_.restaurantId)
      .orElse(restaurantDatabase.randomRestaurant().map(_.restaurantId))
      .getOrElse(UUID.randomUUID().toString)

    val cartItems =
      eventType match
        case CartCreated =>
          Some(List.empty)
        case ItemAddedToCart | ItemRemovedFromCart | CartViewed | CartAbandoned =>
          Some(List.fill(Random.between(1, 5))(randomCartItem()))
        case CheckoutStarted =>
          val canCheckout = sessionSnapshot.exists(snapshot => snapshot.cartItemsCount > 0)
          if canCheckout then Some(List.fill(Random.between(1, 5))(randomCartItem()))
          else Some(List.fill(Random.between(1, 5))(randomCartItem()))

    sessionSnapshot.foreach { snapshot =>
      eventType match
        case CartCreated =>
          sessionInteractionStore.recordCartCreated(snapshot.sessionId, restaurantId)
        case ItemAddedToCart =>
          sessionInteractionStore.recordCartItemDelta(snapshot.sessionId, restaurantId, Random.between(1, 3))
        case ItemRemovedFromCart =>
          sessionInteractionStore.recordCartItemDelta(snapshot.sessionId, restaurantId, -Random.between(1, 3))
        case CartViewed =>
          sessionInteractionStore.assignRestaurant(snapshot.sessionId, restaurantId)
        case CartAbandoned =>
          sessionInteractionStore.recordCartAbandoned(snapshot.sessionId)
        case CheckoutStarted =>
          if !sessionInteractionStore.tryStartCheckout(snapshot.sessionId) then
            sessionInteractionStore.recordCartItemDelta(snapshot.sessionId, restaurantId, Random.between(1, 3))
            sessionInteractionStore.tryStartCheckout(snapshot.sessionId)
    }

    CartEvent(
      eventId = s"evt_${Random.between(1000, 10000)}",
      eventType = eventType,
      eventTs = Instant.now(),
      sessionId = sessionId,
      customerId = customerId,
      restaurantId = restaurantId,
      cartItems = cartItems
    )

  override protected def generateEvent(using EventGenerator.Context): IO[CartEvent] =
    IO.delay(randomCartEvent())

object CartEventGenerator:
  def apply(
      restaurantDatabase: RestaurantDatabase,
      sessionInteractionStore: SessionInteractionStore
  ): CartEventGenerator =
    new CartEventGenerator(restaurantDatabase, sessionInteractionStore)
