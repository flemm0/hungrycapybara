package org.hungrycapybara.ordersimulator.model

import java.time.Instant

enum PromotionEventType:
  case OfferPresented
  case OfferClicked
  case OfferApplied
  case OfferExpired

case class Offer(
    offerId: String,
    offerType: String,
    value: Double,
    trigger: String
)

case class PromotionEvent(
    eventId: String,
    eventType: PromotionEventType,
    eventTs: Instant,
    sessionId: String,
    customerId: String,
    offer: Offer
)
