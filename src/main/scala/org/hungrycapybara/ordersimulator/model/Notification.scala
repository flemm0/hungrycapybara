package org.hungrycapybara.ordersimulator.model

import java.time.Instant

enum NotificationEventType:
  case PushSent
  case PushOpened
  case EmailSent
  case EmailOpened
  case SmsSent
  case SmsOpened

case class NotificationEvent(
    eventId: String,
    eventType: NotificationEventType,
    eventTs: Instant,
    customerId: String,
    campaignId: String,
    messageType: String
)
