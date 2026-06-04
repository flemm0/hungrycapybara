package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.EventGenerator
import org.hungrycapybara.ordersimulator.model.{Offer, PromotionEvent}
import org.hungrycapybara.ordersimulator.model.PromotionEventType.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Random

object PromotionEventGenerator extends EventGenerator:
  type Event = PromotionEvent

  override protected val name: String = "promotion"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(5, 20).seconds)

  private val offerTypes: Vector[String] = Vector(
    "percentage_discount",
    "fixed_discount",
    "free_delivery",
    "cashback"
  )

  private val triggers: Vector[String] = Vector(
    "homepage_banner",
    "checkout_prompt",
    "restaurant_page",
    "push_notification",
    "email_campaign"
  )

  def randomOffer(): Offer =
    val offerType = offerTypes(Random.nextInt(offerTypes.size))
    val value =
      offerType match
        case "percentage_discount" => Random.between(5, 31).toDouble
        case "free_delivery"       => 0.0
        case _                     => BigDecimal(Random.between(2.0, 20.0)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble

    Offer(
      offerId = UUID.randomUUID().toString,
      offerType = offerType,
      value = value,
      trigger = triggers(Random.nextInt(triggers.size))
    )

  def randomPromotionEvent(): PromotionEvent =
    val eventType = Random.nextInt(4) match
      case 0 => OfferPresented
      case 1 => OfferClicked
      case 2 => OfferApplied
      case 3 => OfferExpired

    PromotionEvent(
      eventId = s"evt_${Random.between(1000, 10000)}",
      eventType = eventType,
      eventTs = Instant.now(),
      sessionId = UUID.randomUUID().toString,
      customerId = UUID.randomUUID().toString,
      offer = randomOffer()
    )

  override protected def generateEvent(using EventGenerator.Context): IO[PromotionEvent] =
    IO.delay(randomPromotionEvent())
