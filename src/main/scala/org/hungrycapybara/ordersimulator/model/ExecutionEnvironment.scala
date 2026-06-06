package org.hungrycapybara.ordersimulator.model

enum ExecutionEnvironment:
  case Local, Staging, Production

object ExecutionEnvironment:
  def parse(value: String): Either[String, ExecutionEnvironment] =
    ExecutionEnvironment.values
      .find(_.toString.equalsIgnoreCase(value))
      .toRight(s"Unsupported execution environment: $value")
