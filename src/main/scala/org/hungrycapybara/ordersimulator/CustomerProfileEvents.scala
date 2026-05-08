/**
  * Generates random customer profile events.
  */

package org.hungrycapybara.ordersimulator

import java.util.UUID
import java.time.Instant
import scala.concurrent.duration.*
import scala.util.Random
import org.hungrycapybara.ordersimulator.model.{
  CustomerProfileEvent,
  CustomerProfileEventType,
  ExecutionEnvironment
}
import org.hungrycapybara.ordersimulator.helper.SeedData
import cats.effect.{IO, Outcome}
import cats.syntax.all.*
import slick.jdbc.SQLiteProfile.api.Database

object CustomerProfileEvents extends EventGenerator:
  private val eventInterval = 1.second

  def randomCustomerProfileEvent(): CustomerProfileEvent =
    val eventId = UUID.randomUUID().toString
    val eventType = if (Random.nextBoolean()) CustomerProfileEventType.CustomerCreated else CustomerProfileEventType.CustomerUpdated
    val eventTs = Instant.now()
    val customer = SeedData.randomCustomer()

    CustomerProfileEvent(eventId, eventType, eventTs, customer)

  private def emitOne: IO[Unit] =
    IO.delay(randomCustomerProfileEvent())
      .flatMap(event => IO.println(s"Generated event: $event"))

  private def runLoop(database: Database): IO[Unit] =
    val loop = (emitOne *> IO.sleep(eventInterval)).foreverM

    (IO.println("Customer profile event generator started") *> loop)
      .guaranteeCase {
        case Outcome.Canceled() =>
          IO.println("Customer profile event generator stopped")
        case Outcome.Errored(error) =>
          IO.println(s"Customer profile event generator failed: ${error.getMessage}")
        case Outcome.Succeeded(_) =>
          IO.unit
      }

  override def run(executionEnv: ExecutionEnvironment, database: Database): IO[Unit] =
    executionEnv match
      case ExecutionEnvironment.Local =>
        runLoop(database)
      case _ =>
        IO.println("Not yet implemented for non-local environments")
