package org.hungrycapybara.ordersimulator.publisher

import io.circe.{Json, JsonObject}

import java.time.Instant

object EventPayloadEncoder:
  def encodeEnvelope(streamName: String, key: String, event: Any): Json =
    Json.fromJsonObject(
      JsonObject(
        "timestamp" -> Json.fromString(Instant.now().toString),
        "streamName" -> Json.fromString(streamName),
        "key" -> Json.fromString(key),
        "eventClass" -> Json.fromString(event.getClass.getSimpleName),
        "event" -> encodeValue(event)
      )
    )

  // Policy: non-finite floating point values are encoded as JSON null.
  def encodeValue(value: Any): Json =
    value match
      case null                => Json.Null
      case text: String        => Json.fromString(text)
      case text: Char          => Json.fromString(text.toString)
      case flag: Boolean       => Json.fromBoolean(flag)
      case number: Byte        => Json.fromInt(number.toInt)
      case number: Short       => Json.fromInt(number.toInt)
      case number: Int         => Json.fromInt(number)
      case number: Long        => Json.fromLong(number)
      case number: Float       => Json.fromFloatOrNull(number)
      case number: Double      => Json.fromDoubleOrNull(number)
      case number: BigInt      => Json.fromBigInt(number)
      case number: BigDecimal  => Json.fromBigDecimal(number)
      case instant: Instant    => Json.fromString(instant.toString)
      case enumValue: Enum[?]  => Json.fromString(enumValue.name)
      case option: Option[?]   => option.map(encodeValue).getOrElse(Json.Null)
      case values: Iterable[?] =>
        Json.arr(values.iterator.map(encodeValue).toSeq*)
      case values: Array[?] =>
        Json.arr(values.iterator.map(encodeValue).toSeq*)
      case product: Product =>
        Json.obj(
          product.productElementNames
            .zip(product.productIterator)
            .map { case (name, fieldValue) => name -> encodeValue(fieldValue) }
            .toSeq*
        )
      case other =>
        Json.fromString(other.toString)
