package org.hungrycapybara.ordersimulator

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment

import scala.concurrent.duration.*

class EventGeneratorPublisherSuite extends munit.FunSuite:
  test("delegates publish to EventPublisher with stream name and derived eventId key") {
    val publisher = RecordingPublisher()
    val generator = ProbeGenerator
    val event = PublishableEvent(eventId = "evt_1", payload = "value")
    given EventGenerator.Context = EventGenerator.Context(ExecutionEnvironment.Local, publisher)

    generator.publishOne(event).unsafeRunSync()

    assertEquals(publisher.calls.length, 1)
    assertEquals(publisher.calls.head._1, "probe-stream")
    assertEquals(publisher.calls.head._2, "evt_1")
    assertEquals(publisher.calls.head._3, event)
  }

  test("falls back to stable hash-based key when known key fields are missing") {
    val publisher = RecordingPublisher()
    val generator = ProbeGenerator
    val event = NoKeyEvent(value = "payload")
    given EventGenerator.Context = EventGenerator.Context(ExecutionEnvironment.Local, publisher)

    generator.publishNoKeyEvent(event).unsafeRunSync()

    assertEquals(publisher.calls.length, 1)
    assert(publisher.calls.head._2.startsWith("probe-"))
  }

  private final case class RecordingPublisher(var calls: List[(String, String, Any)] = Nil) extends EventPublisher:
    override def publish[A](streamName: String, key: String, event: A): IO[Unit] =
      IO {
        calls = calls :+ (streamName, key, event)
      }

  private final case class PublishableEvent(eventId: String, payload: String)
  private final case class NoKeyEvent(value: String)

  private object ProbeGenerator extends EventGenerator:
    override type Event = Any

    override protected def name: String = "probe"
    override protected def streamName: String = "probe-stream"

    override protected def eventInterval: IO[FiniteDuration] = IO.pure(1.second)

    override protected def generateEvent(using EventGenerator.Context): IO[Any] =
      IO.pure(PublishableEvent("evt_generated", "generated"))

    def publishOne(event: PublishableEvent)(using EventGenerator.Context): IO[Unit] =
      publishEvent(event)

    def publishNoKeyEvent(event: NoKeyEvent)(using EventGenerator.Context): IO[Unit] =
      publishEvent(event)
