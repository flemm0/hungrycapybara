package org.hungrycapybara.ordersimulator.generators

import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.helper.CustomerUserbase
import org.hungrycapybara.ordersimulator.helper.SeedData
import org.hungrycapybara.ordersimulator.model.CustomerSessionEvent
import org.hungrycapybara.ordersimulator.model.CustomerSessionEventType
import org.hungrycapybara.ordersimulator.model.CustomerSessionEventType.*
import scala.concurrent.duration.*
import scala.util.Random
import cats.effect.IO

import java.time.Instant
import java.util.UUID

final class CustomerSessionEventGenerator(
    customerUserbase: CustomerUserbase,
    sessionInteractionStore: SessionInteractionStore
) extends EventGenerator:
  type Event = CustomerSessionEvent

  override protected val name: String = "customer-session"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(5, 15).seconds)

  private def randomCustomerId(): String =
    customerUserbase
      .randomActiveCustomer()
      .map(_.customerId)
      .getOrElse(UUID.randomUUID().toString)

  private def buildEvent(eventType: CustomerSessionEventType, customerId: String, sessionId: String): CustomerSessionEvent =
    val eventId = UUID.randomUUID().toString
    val eventTs = Instant.now()
    val deviceType = SeedData.randomSessionDeviceType()
    val appVersion = SeedData.randomSessionAppVersion()
    val entryPoint = SeedData.randomSessionEntryPoint()

    CustomerSessionEvent(
      eventId = eventId,
      eventType = eventType,
      eventTs = eventTs,
      customerId = customerId,
      sessionId = sessionId,
      deviceType = deviceType,
      appVersion = appVersion,
      entryPoint = entryPoint
    )

  private def startSessionEvent(): CustomerSessionEvent =
    val customerId = randomCustomerId()
    val activeSession = sessionInteractionStore.startSession(customerId)
    buildEvent(SessionStarted, activeSession.customerId, activeSession.sessionId)

  private def endSessionEventOrStart(): CustomerSessionEvent =
    sessionInteractionStore
      .endRandomSession()
      .map(session => buildEvent(SessionEnded, session.customerId, session.sessionId))
      .getOrElse(startSessionEvent())

  override protected def generateEvent(using EventGenerator.Context): IO[CustomerSessionEvent] =
    IO.delay {
      val shouldEndSession = sessionInteractionStore.activeSessionCount > 0 && Random.nextDouble() < 0.35

      if shouldEndSession then endSessionEventOrStart()
      else startSessionEvent()
    }

object CustomerSessionEventGenerator:
  def apply(
      customerUserbase: CustomerUserbase,
      sessionInteractionStore: SessionInteractionStore
  ): CustomerSessionEventGenerator =
    new CustomerSessionEventGenerator(customerUserbase, sessionInteractionStore)
