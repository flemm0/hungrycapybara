/**
  * Generates random customer profile events.
  */

package org.hungrycapybara.ordersimulator

import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.util.Random
import org.hungrycapybara.ordersimulator.model.{
  Customer,
  CustomerProfileEvent,
  CustomerProfileEventType,
  CustomerLoyaltyTier,
  ExecutionEnvironment
}
import org.hungrycapybara.ordersimulator.helper.SeedData
import org.hungrycapybara.ordersimulator.database.LocalDatabase
import cats.effect.{IO, Outcome}
import cats.syntax.all.*
import slick.jdbc.SQLiteProfile.api.{Database, DBIO}

object CustomerProfileEvents extends EventGenerator:
  private val eventInterval = 1.second
  
  def randomCustomerUpdate(customer: Customer): Customer =
    val weights = Map(
      "homeCity" -> 0.1,
      "favoriteCuisines" -> 0.3,
      "loyaltyTier" -> 0.2,
      "averageOrderValue" -> 0.4
    )
    val updateSource = SeedData.randomCustomer()

    randomWeightedKey(weights) match
      case "homeCity" =>
        customer.copy(homeCity = updateSource.homeCity)
      case "favoriteCuisines" =>
        customer.copy(favoriteCuisines = updateSource.favoriteCuisines)
      case "loyaltyTier" =>
        customer.copy(loyaltyTier = randomDifferent(customer.loyaltyTier, CustomerLoyaltyTier.values.toList))
      case "averageOrderValue" =>
        customer.copy(averageOrderValue = updateSource.averageOrderValue)
      case _ =>
        customer

  private def randomWeightedKey(weights: Map[String, Double]): String =
    val threshold = Random.nextDouble() * weights.values.sum

    weights.iterator
      .scanLeft(("", 0.0)) { case ((_, total), (key, weight)) =>
        (key, total + weight)
      }
      .drop(1)
      .find { case (_, total) => total >= threshold }
      .map { case (key, _) => key }
      .getOrElse(weights.keys.last)

  private def randomDifferent[A](current: A, values: List[A]): A =
    Random.shuffle(values.filterNot(_ == current)).headOption.getOrElse(current)

  private def randomCustomerPrintAndInsert(database: Database): IO[Option[Customer]] =
    val action: DBIO[Option[Customer]] =
      LocalDatabase.randomCustomerAction.flatMap {
        case Some(customer) =>
          val customerToInsert =
            randomCustomerUpdate(customer)

          DBIO.successful(println(s"Random customer: $customer"))
            .andThen(LocalDatabase.insertCustomerAction(customerToInsert))
            .map(_ => Some(customerToInsert))

        case None =>
          DBIO.successful(None)
      }

    LocalDatabase.runTransaction(database, action)

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
