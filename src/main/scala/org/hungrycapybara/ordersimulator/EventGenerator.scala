package org.hungrycapybara.ordersimulator

import cats.effect.{IO, Outcome}
import cats.syntax.all.*
import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment
import slick.jdbc.SQLiteProfile.api.Database

import scala.concurrent.duration.FiniteDuration

object EventGenerator:
  final case class Context(
      executionEnv: ExecutionEnvironment
      // database: Database
  )

trait EventGenerator:
  type Event

  protected def name: String
  protected def eventInterval: FiniteDuration

  protected def generateEvent(using EventGenerator.Context): IO[Event]

  protected def publishEvent(event: Event)(using EventGenerator.Context): IO[Unit] =
    IO.println(s"[$name] Generated event: $event")

  protected def runForEnvironment(using context: EventGenerator.Context): IO[Unit] =
    context.executionEnv match
      case ExecutionEnvironment.Local =>
        runContinuously
      case unsupported =>
        IO.println(s"[$name] event generator is not implemented for $unsupported")

  final def run(executionEnv: ExecutionEnvironment): IO[Unit] =
    given EventGenerator.Context = EventGenerator.Context(executionEnv)

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
    (emitOne.void *> IO.sleep(eventInterval)).foreverM

  protected def logStarted: IO[Unit] =
    IO.println(s"[$name] event generator started")

  protected def logStopped: IO[Unit] =
    IO.println(s"[$name] event generator stopped")

  protected def logFailed(error: Throwable): IO[Unit] =
    IO.println(s"[$name] event generator failed: ${error.getMessage}")
