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
  
  // def randomCustomerUpdate(customer: Customer): Customer =
  //   val updateSource = SeedData.randomCustomer()

  //   val updates: Vector[(Double, Customer => Customer)] = Vector(
  //     0.1 -> (_.copy(homeCity = updateSource.homeCity)),
  //     0.3 -> (_.copy(favoriteCuisines = updateSource.favoriteCuisines)),
  //     0.2 -> (c => c.copy(
  //       loyaltyTier = randomDifferent(c.loyaltyTier, CustomerLoyaltyTier.values.toList)
  //     )),
  //     0.4 -> (_.copy(averageOrderValue = updateSource.averageOrderValue))
  //   )

  //   randomWeighted(updates)(customer)


  // private def randomWeighted[A](choices: Vector[(Double, A)]): A =
  //   val threshold = Random.nextDouble() * choices.map(_._1).sum

  //   choices.iterator
  //     .scanLeft((0.0, choices.last._2)) { case ((total, _), (weight, value)) =>
  //       (total + weight, value)
  //     }
  //     .drop(1)
  //     .find { case (total, _) => total >= threshold }
  //     .map { case (_, value) => value }
  //     .getOrElse(choices.last._2)

  // private def randomDifferent[A](current: A, values: List[A]): A =
  //   Random.shuffle(values.filterNot(_ == current)).headOption.getOrElse(current)

  // private def randomCustomerPrintAndInsert(database: Database): IO[Option[Customer]] =
  //   val action: DBIO[Option[Customer]] =
  //     LocalDatabase.randomCustomerAction.flatMap {
  //       case Some(customer) =>
  //         val customerToInsert =
  //           randomCustomerUpdate(customer)

  //         DBIO.successful(println(s"Random customer: $customer"))
  //           .andThen(LocalDatabase.insertCustomerAction(customerToInsert))
  //           .map(_ => Some(customerToInsert))

  //       case None =>
  //         DBIO.successful(None)
  //     }

  //   LocalDatabase.runTransaction(database, action)

  // Need to create 3 functions for CRUD operations on the customer DB
  // 1. Create: insert a new customer into the database (new registration for the service)
  // 2. Update: randomly select an existing customer and update one of their attributes (e.g., change home city, update loyalty tier, etc.)
  // 3. Delete: randomly select an existing customer and delete them from the database (simulate account deletion)

  def registerNewCustomer(customer: Customer)(using database: Database): IO[CustomerProfileEvent] =
    val action: DBIO[Option[Customer]] =
      LocalDatabase
        .insertCustomerAction(customer)
        .map(_ => customer)
    LocalDatabase.runTransaction(action).map { insertedCustomer =>
      CustomerProfileEvent(
        eventId = UUID.randomUUID().toString,
        eventType = CustomerProfileEventType.CustomerCreated,
        eventTs = Instant.now(),
        customer = insertedCustomer
      )
    }

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
