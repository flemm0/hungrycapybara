package org.hungrycapybara.ordersimulator

import java.time.Instant
import java.util.UUID

import scala.concurrent.duration.*
import scala.util.Random
import cats.effect.IO
import org.hungrycapybara.ordersimulator.helper.SeedData
import org.hungrycapybara.ordersimulator.model.CustomerProfileEvent
import org.hungrycapybara.ordersimulator.model.CustomerProfileEventType.*

object CustomerProfileEvents extends EventGenerator:
  type Event = CustomerProfileEvent

  override protected val name: String = "customer-profile"
  override protected val eventInterval: FiniteDuration = 1.second

  /**
    * A simple method to generate a random customer profile event with the set schema.
    * Enhancements will be to generate more realistic event data.
    */
  def randomCustomerProfileEvent(): CustomerProfileEvent =
    val eventId = UUID.randomUUID().toString
    val eventType = Random.nextInt(3) match
      case 0 => CustomerCreated
      case 1 => CustomerUpdated
      case 2 => CustomerDeleted
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
