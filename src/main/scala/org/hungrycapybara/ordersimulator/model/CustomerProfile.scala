package org.hungrycapybara.ordersimulator.model

import java.time.Instant

enum CustomerProfileEventType:
  case CustomerCreated
  case CustomerUpdated

enum CustomerLoyaltyTier:
  case Bronze, Silver, Gold, Platinum

case class Customer(
  customerId: String,
  signupDate: Instant,
  homeCity: String,
  favoriteCuisines: List[String],
  loyaltyTier: CustomerLoyaltyTier,
  averageOrderValue: Double,
)

case class CustomerProfileEvent(
  eventId: String,
  eventType: CustomerProfileEventType,
  eventTs: Instant,
  customer: Customer
)
