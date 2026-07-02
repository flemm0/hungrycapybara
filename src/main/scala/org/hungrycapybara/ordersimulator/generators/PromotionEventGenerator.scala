package org.hungrycapybara.ordersimulator.generators

import cats.effect.IO
import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.helper.SeedData
import org.hungrycapybara.ordersimulator.model.{Offer, PromotionEvent}
import org.hungrycapybara.ordersimulator.model.PromotionEventType.*

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.*
import scala.util.Random

final class PromotionEventGenerator(
    sessionInteractionStore: SessionInteractionStore
) extends EventGenerator:
  type Event = PromotionEvent

  override protected val name: String = "promotion"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(5, 20).seconds)

  private def randomOffer(): Offer =
    val offerType = SeedData.randomPromotionOfferType()
    val value =
      offerType match
        case "percentage_discount" => Random.between(5, 31).toDouble
        case "free_delivery"       => 0.0
        case _                     =>
          BigDecimal(Random.between(2.0, 20.0))
            .setScale(2, BigDecimal.RoundingMode.HALF_UP)
            .toDouble

    Offer(
      offerId = UUID.randomUUID().toString,
      offerType = offerType,
      value = value,
      trigger = SeedData.randomPromotionTrigger()
    )

  private def randomPromotionEvent(): PromotionEvent =
    val eventType = Random.nextInt(4) match
      case 0 => OfferPresented
      case 1 => OfferClicked
      case 2 => OfferApplied
      case 3 => OfferExpired

    val (sessionId, customerId) =
      sessionInteractionStore
        .randomActiveSession()
        .map(session => (session.sessionId, session.customerId))
        .getOrElse((UUID.randomUUID().toString, UUID.randomUUID().toString))

    PromotionEvent(
      eventId = s"evt_${Random.between(1000, 10000)}",
      eventType = eventType,
      eventTs = Instant.now(),
      sessionId = sessionId,
      customerId = customerId,
      offer = randomOffer()
    )

  override protected def generateEvent(using EventGenerator.Context): IO[PromotionEvent] =
    IO.delay(randomPromotionEvent())

object PromotionEventGenerator:
  def apply(
      sessionInteractionStore: SessionInteractionStore
  ): PromotionEventGenerator =
    new PromotionEventGenerator(sessionInteractionStore)
