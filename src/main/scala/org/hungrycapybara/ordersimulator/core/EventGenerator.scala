package org.hungrycapybara.ordersimulator.core

import cats.effect.{IO, Outcome}
import cats.syntax.all.*
import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment

import scala.concurrent.duration.FiniteDuration

object EventGenerator:
  final case class Context(
      executionEnv: ExecutionEnvironment,
      publisher: EventPublisher
      // database: Database
  )

trait EventGenerator:
  type Event

  protected def name: String
  protected def eventInterval: IO[FiniteDuration]

  protected def generateEvent(using EventGenerator.Context): IO[Event]

  protected def streamName: String = name

  protected def eventKey(event: Event): String =
    List("eventId", "orderId", "cartId", "sessionId", "customerId", "restaurantId")
      .iterator
      .flatMap(fieldName => extractFieldValue(event, fieldName).iterator)
      .take(1)
      .toList
      .headOption
      .getOrElse(s"$name-${Integer.toUnsignedString(event.hashCode(), 16)}")

  protected def publishEvent(event: Event)(using EventGenerator.Context): IO[Unit] =
    summon[EventGenerator.Context].publisher.publish(streamName, eventKey(event), event)

  protected def runForEnvironment(using context: EventGenerator.Context): IO[Unit] =
    context.executionEnv match
      case ExecutionEnvironment.Local =>
        runContinuously
      case unsupported =>
        IO.println(s"[$name] event generator is not implemented for $unsupported")

  final def run(executionEnv: ExecutionEnvironment, publisher: EventPublisher): IO[Unit] =
    given EventGenerator.Context = EventGenerator.Context(executionEnv, publisher)

    (logStarted *> runForEnvironment)
      .guaranteeCase {
        case Outcome.Canceled() =>
          logStopped
        case Outcome.Errored(error) =>
          logFailed(error)
        case Outcome.Succeeded(_) =>
          IO.unit
      }

  final protected def emitOne(using EventGenerator.Context): IO[Event] =
    generateEvent.flatTap(publishEvent)

  final protected def runContinuously(using EventGenerator.Context): IO[Unit] =
    (emitOne.void *> eventInterval.flatMap(IO.sleep)).foreverM

  protected def logStarted: IO[Unit] =
    IO.println(s"[$name] event generator started")

  protected def logStopped: IO[Unit] =
    IO.println(s"[$name] event generator stopped")

  protected def logFailed(error: Throwable): IO[Unit] =
    IO.println(s"[$name] event generator failed: ${error.getMessage}")

  private def extractFieldValue(value: Any, fieldName: String): Option[String] =
    value match
      case product: Product =>
        product.productElementNames
          .zip(product.productIterator)
          .collectFirst {
            case (name, fieldValue) if name == fieldName =>
              normalizeFieldValue(fieldValue)
          }
          .flatten
      case _ =>
        None

  private def normalizeFieldValue(value: Any): Option[String] =
    value match
      case null        => None
      case None        => None
      case Some(inner) => Option(inner).map(_.toString)
      case other       => Some(other.toString)