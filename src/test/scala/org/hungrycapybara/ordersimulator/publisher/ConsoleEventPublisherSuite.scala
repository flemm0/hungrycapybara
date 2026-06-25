package org.hungrycapybara.ordersimulator.publisher

import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser.parse

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets

class ConsoleEventPublisherSuite extends munit.FunSuite:
  test("prints one JSON line with stream, key, class, and event payload") {
    val line = captureStdOut {
      ConsoleEventPublisher.publish(
        streamName = "orders",
        key = "evt_123",
        event = TestEvent(eventId = "evt_123", count = 2)
      )
    }

    val json = parse(line).fold(err => fail(err.getMessage), identity)
    val cursor = json.hcursor

    assertEquals(cursor.get[String]("streamName"), Right("orders"))
    assertEquals(cursor.get[String]("key"), Right("evt_123"))
    assertEquals(cursor.get[String]("eventClass"), Right("TestEvent"))
    assertEquals(cursor.downField("event").get[String]("eventId"), Right("evt_123"))
    assertEquals(cursor.downField("event").get[Int]("count"), Right(2))
  }

  test("escapes quotes and control characters in string payloads") {
    val line = captureStdOut {
      ConsoleEventPublisher.publish(
        streamName = "orders",
        key = "evt_escaped",
        event = TestMessage("hello \"world\"\nnext")
      )
    }

    val json = parse(line).fold(err => fail(err.getMessage), identity)
    val payload = json.hcursor.downField("event").get[String]("value")
    assertEquals(payload, Right("hello \"world\"\nnext"))
  }

  test("keeps nulls and encodes non-finite numbers as null") {
    val line = captureStdOut {
      ConsoleEventPublisher.publish(
        streamName = "orders",
        key = "evt_special",
        event = TestSpecials(
          optionalText = None,
          nanValue = Double.NaN,
          infValue = Double.PositiveInfinity
        )
      )
    }

    val json = parse(line).fold(err => fail(err.getMessage), identity)
    val eventCursor = json.hcursor.downField("event")

    assertEquals(eventCursor.downField("optionalText").focus, Some(Json.Null))
    assertEquals(eventCursor.downField("nanValue").focus, Some(Json.Null))
    assertEquals(eventCursor.downField("infValue").focus, Some(Json.Null))
  }

  private def captureStdOut(program: cats.effect.IO[Unit]): String =
    val bytes = ByteArrayOutputStream()
    val printStream = PrintStream(bytes)
    val originalOut = System.out

    System.setOut(printStream)
    try {
      program.unsafeRunSync()
    } finally {
      printStream.flush()
      System.setOut(originalOut)
    }

    bytes.toString(StandardCharsets.UTF_8).trim

  private final case class TestEvent(eventId: String, count: Int)
  private final case class TestMessage(value: String)
  private final case class TestSpecials(
      optionalText: Option[String],
      nanValue: Double,
      infValue: Double
  )
