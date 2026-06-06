package org.hungrycapybara.ordersimulator.publisher

import cats.effect.unsafe.implicits.global

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

    assert(line.startsWith("{"))
    assert(line.endsWith("}"))
    assert(line.contains("\"streamName\":\"orders\""))
    assert(line.contains("\"key\":\"evt_123\""))
    assert(line.contains("\"eventClass\":\"TestEvent\""))
    assert(line.contains("\"event\":{\"eventId\":\"evt_123\",\"count\":2}"))
  }

  test("escapes quotes and control characters in string payloads") {
    val line = captureStdOut {
      ConsoleEventPublisher.publish(
        streamName = "orders",
        key = "evt_escaped",
        event = TestMessage("hello \"world\"\nnext")
      )
    }

    assert(line.contains("hello \\\"world\\\"\\nnext"))
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
