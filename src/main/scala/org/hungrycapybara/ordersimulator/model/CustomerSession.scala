package org.hungrycapybara.ordersimulator.model

import java.time.Instant

enum CustomerSessionEventType:
  case SessionStarted, SessionEnded

case class CustomerSessionEvent(
  eventId: String,
  eventType: CustomerSessionEventType,
  eventTs: Instant,
  sessionId: String,
  customerId: String,
  deviceType: String,
  appVersion: String,
  entryPoint: String,
)
