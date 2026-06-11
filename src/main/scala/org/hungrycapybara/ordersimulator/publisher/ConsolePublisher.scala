package org.hungrycapybara.ordersimulator.publisher

import org.hungrycapybara.ordersimulator.core.EventPublisher
import cats.effect.IO
import java.time.Instant

object ConsoleEventPublisher extends EventPublisher:
  override def publish[A](streamName: String, key: String, event: A): IO[Unit] =
    IO.println(
      s"{" +
        s"\"timestamp\":${JsonValueEncoder.encode(Instant.now().toString)}," +
        s"\"streamName\":${JsonValueEncoder.encode(streamName)}," +
        s"\"key\":${JsonValueEncoder.encode(key)}," +
        s"\"eventClass\":${JsonValueEncoder.encode(event.getClass.getSimpleName)}," +
        s"\"event\":${JsonValueEncoder.encode(event)}" +
        s"}"
    )

private object JsonValueEncoder:
  def encode(value: Any): String =
    value match
      case null          => "null"
      case text: String  => quote(text)
      case text: Char    => quote(text.toString)
      case flag: Boolean => flag.toString
      case number: Byte  => number.toString
      case number: Short => number.toString
      case number: Int   => number.toString
      case number: Long  => number.toString
      case number: Float =>
        if number.isInfinite || number.isNaN then quote(number.toString) else number.toString
      case number: Double =>
        if number.isInfinite || number.isNaN then quote(number.toString) else number.toString
      case number: BigInt     => number.toString
      case number: BigDecimal => number.toString
      case instant: Instant   => quote(instant.toString)
      case option: Option[?]  => option.map(encode).getOrElse("null")
      case values: Iterable[?] =>
        values.iterator.map(encode).mkString("[", ",", "]")
      case values: Array[?] =>
        values.iterator.map(encode).mkString("[", ",", "]")
      case product: Product =>
        product.productElementNames
          .zip(product.productIterator)
          .map { case (name, fieldValue) => s"${quote(name)}:${encode(fieldValue)}" }
          .mkString("{", ",", "}")
      case other =>
        quote(other.toString)

  private def quote(value: String): String =
    val escaped = value.flatMap {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case ch if Character.isISOControl(ch) => f"\\u${ch.toInt}%04x"
      case ch => ch.toString
    }
    s"\"$escaped\""
