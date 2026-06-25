package org.hungrycapybara.ordersimulator.generators

import java.time.Instant
import java.util.UUID

import scala.concurrent.duration.*
import scala.util.Random
import cats.effect.IO
import org.hungrycapybara.ordersimulator.core.EventGenerator
import org.hungrycapybara.ordersimulator.helper.SeedData
import org.hungrycapybara.ordersimulator.model.CustomerProfileEvent
import org.hungrycapybara.ordersimulator.model.CustomerProfileEventType
import org.hungrycapybara.ordersimulator.model.CustomerProfileEventType.*

object CustomerProfileEventGenerator extends EventGenerator:
  type Event = CustomerProfileEvent

  override protected val name: String = "customer-profile"
  override protected def eventInterval: IO[FiniteDuration] =
    IO.delay(Random.between(1, 11).seconds)

  private def randomWeightedEventType: CustomerProfileEventType =
    Random.nextDouble() match
      case x if x < 0.12 => CustomerCreated
      case x if x < 0.98 => CustomerUpdated
      case _             => CustomerDeleted

  /**
    * A simple method to generate a random customer profile event with the set schema.
    * Enhancements will be to generate more realistic event data.
    */
  def randomCustomerProfileEvent(): CustomerProfileEvent =
    val eventId = UUID.randomUUID().toString
    val eventType = randomWeightedEventType
    val eventTs = Instant.now()
    val customer = SeedData.randomCustomer().copy(isActive = eventType != CustomerDeleted)

    CustomerProfileEvent(
      eventId = eventId,
      eventType = eventType,
      eventTs = eventTs,
      customer = customer
    )

  override protected def generateEvent(using EventGenerator.Context): IO[CustomerProfileEvent] =
    IO.delay(randomCustomerProfileEvent())
