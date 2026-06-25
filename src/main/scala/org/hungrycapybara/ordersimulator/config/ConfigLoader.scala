package org.hungrycapybara.ordersimulator.config

import cats.effect.IO
import cats.syntax.all.*
import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment
import org.yaml.snakeyaml.Yaml

import java.nio.file.{Path, Paths}
import scala.annotation.tailrec
import scala.io.Source
import scala.jdk.CollectionConverters.*

object ConfigLoader:
  def load(args: List[String]): IO[AppConfig] =
    for
      cli <- fromConfigEither(parseArgs(args))
      yaml <- cli.configPath.traverse(loadYamlConfig).map(_.getOrElse(PartialAppConfig.empty))
      config <- fromConfigEither(yaml.merge(cli.toPartialConfig).toAppConfig)
      validated <- fromConfigEither(validate(config))
    yield validated

  private def fromConfigEither[A](value: Either[String, A]): IO[A] =
    IO.fromEither(value.leftMap(new IllegalArgumentException(_)))

  private final case class CliConfig(
      configPath: Option[Path] = None,
      environment: Option[String] = None,
      kafkaBootstrapServers: Option[String] = None,
      kafkaClientId: Option[String] = None,
      kafkaTopicPrefix: Option[String] = None,
      initialCustomerCount: Option[Int] = None
  ):
    def toPartialConfig: PartialAppConfig =
      PartialAppConfig(
        environment = environment,
        initialCustomerCount = initialCustomerCount,
        kafka = PartialKafkaConfig.fromOptions(
          bootstrapServers = kafkaBootstrapServers,
          clientId = kafkaClientId,
          topicPrefix = kafkaTopicPrefix
        )
      )

  private final case class PartialAppConfig(
      environment: Option[String] = None,
      initialCustomerCount: Option[Int] = None,
      kafka: Option[PartialKafkaConfig] = None
  ):
    def merge(overrides: PartialAppConfig): PartialAppConfig =
      PartialAppConfig(
        environment = overrides.environment.orElse(environment),
        initialCustomerCount = overrides.initialCustomerCount.orElse(initialCustomerCount),
        kafka = (kafka, overrides.kafka) match
          case (Some(base), Some(next)) => Some(base.merge(next))
          case (None, Some(next))       => Some(next)
          case (Some(base), None)       => Some(base)
          case (None, None)             => None
      )

    def toAppConfig: Either[String, AppConfig] =
      for
        parsedEnvironment <- ExecutionEnvironment.parse(environment.getOrElse("local"))
        parsedKafka <- kafka.traverse(_.toKafkaConfig)
      yield AppConfig(
        environment = parsedEnvironment,
        kafka = parsedKafka,
        initialCustomerCount = initialCustomerCount.getOrElse(AppConfig.DefaultInitialCustomerCount)
      )

  private object PartialAppConfig:
    val empty: PartialAppConfig = PartialAppConfig()

  private final case class PartialKafkaConfig(
      bootstrapServers: Option[String] = None,
      clientId: Option[String] = None,
      topicPrefix: Option[String] = None
  ):
    def merge(overrides: PartialKafkaConfig): PartialKafkaConfig =
      PartialKafkaConfig(
        bootstrapServers = overrides.bootstrapServers.orElse(bootstrapServers),
        clientId = overrides.clientId.orElse(clientId),
        topicPrefix = overrides.topicPrefix.orElse(topicPrefix)
      )

    def toKafkaConfig: Either[String, KafkaConfig] =
      val missing = List(
        "kafka.bootstrapServers" -> bootstrapServers,
        "kafka.clientId" -> clientId,
        "kafka.topicPrefix" -> topicPrefix
      ).collect { case (name, None) => name }

      Either.cond(
        missing.isEmpty,
        KafkaConfig(
          bootstrapServers = bootstrapServers.get,
          clientId = clientId.get,
          topicPrefix = topicPrefix.get
        ),
        s"Incomplete Kafka config. Missing: ${missing.mkString(", ")}"
      )

  private object PartialKafkaConfig:
    def fromOptions(
        bootstrapServers: Option[String],
        clientId: Option[String],
        topicPrefix: Option[String]
    ): Option[PartialKafkaConfig] =
      Option.when(List(bootstrapServers, clientId, topicPrefix).exists(_.isDefined))(
        PartialKafkaConfig(bootstrapServers, clientId, topicPrefix)
      )

  private def parseArgs(args: List[String]): Either[String, CliConfig] =
    @tailrec
    def loop(remaining: List[String], parsed: CliConfig): Either[String, CliConfig] =
      remaining match
        case Nil =>
          Right(parsed)
        case option :: tail if option.startsWith("--") =>
          val (name, inlineValue) = splitOption(option)
          val valueAndRest = inlineValue match
            case Some(value) =>
              Right((value, tail))
            case None =>
              tail match
                case value :: rest if !value.startsWith("--") =>
                  Right((value, rest))
                case _ =>
                  Left(s"Missing value for $name")

          valueAndRest match
            case Left(error) =>
              Left(error)
            case Right((value, rest)) =>
              updateCliConfig(parsed, name, value) match
                case Left(error)  => Left(error)
                case Right(next) => loop(rest, next)
        case unexpected :: _ =>
          Left(s"Unexpected argument: $unexpected")

    loop(args, CliConfig())

  private def splitOption(option: String): (String, Option[String]) =
    option.split("=", 2).toList match
      case name :: value :: Nil => (name, Some(value))
      case _                    => (option, None)

  private def updateCliConfig(config: CliConfig, name: String, value: String): Either[String, CliConfig] =
    nonEmptyValue(name, value).flatMap { nonEmpty =>
      name match
        case "--config" =>
          Right(config.copy(configPath = Some(Paths.get(nonEmpty))))
        case "--env" | "--environment" =>
          Right(config.copy(environment = Some(nonEmpty)))
        case "--kafka-bootstrap-servers" =>
          Right(config.copy(kafkaBootstrapServers = Some(nonEmpty)))
        case "--kafka-client-id" =>
          Right(config.copy(kafkaClientId = Some(nonEmpty)))
        case "--kafka-topic-prefix" =>
          Right(config.copy(kafkaTopicPrefix = Some(nonEmpty)))
        case "--initial-customer-count" =>
          parseNonNegativeInt("--initial-customer-count", nonEmpty).map { count =>
            config.copy(initialCustomerCount = Some(count))
          }
        case unknown =>
          Left(s"Unknown argument: $unknown")
    }

  private def nonEmptyValue(name: String, value: String): Either[String, String] =
    Either.cond(value.nonEmpty, value, s"$name cannot be empty")

  private def parseNonNegativeInt(path: String, value: String): Either[String, Int] =
    value.toLongOption
      .toRight(s"$path must be an integer")
      .flatMap(nonNegativeInt(path, _))

  private def loadYamlConfig(path: Path): IO[PartialAppConfig] =
    readFile(path).flatMap { contents =>
      IO.blocking(Option(new Yaml().load[Any](contents)))
        .flatMap {
          case None =>
            IO.pure(PartialAppConfig.empty)
          case Some(value) =>
            fromConfigEither(parseYamlRoot(value))
        }
    }

  private def readFile(path: Path): IO[String] =
    IO.blocking {
      val source = Source.fromFile(path.toFile)
      try source.mkString
      finally source.close()
    }

  private def parseYamlRoot(value: Any): Either[String, PartialAppConfig] =
    for
      root <- asMap(value, "config")
      environment <- optionalString(root, "environment", "environment")
      initialCustomerCount <- optionalInt(root, "initialCustomerCount", "initialCustomerCount")
      kafka <- optionalMap(root, "kafka", "kafka").flatMap(_.traverse(parseKafkaConfig))
    yield PartialAppConfig(environment = environment, initialCustomerCount = initialCustomerCount, kafka = kafka)

  private def parseKafkaConfig(value: Map[String, Any]): Either[String, PartialKafkaConfig] =
    for
      bootstrapServers <- optionalString(value, "bootstrapServers", "kafka.bootstrapServers")
      clientId <- optionalString(value, "clientId", "kafka.clientId")
      topicPrefix <- optionalString(value, "topicPrefix", "kafka.topicPrefix")
    yield PartialKafkaConfig(
      bootstrapServers = bootstrapServers,
      clientId = clientId,
      topicPrefix = topicPrefix
    )

  private def optionalMap(
      values: Map[String, Any],
      key: String,
      path: String
  ): Either[String, Option[Map[String, Any]]] =
    values.get(key) match
      case None | Some(null) =>
        Right(None)
      case Some(value) =>
        asMap(value, path).map(Some(_))

  private def optionalString(
      values: Map[String, Any],
      key: String,
      path: String
  ): Either[String, Option[String]] =
    values.get(key) match
      case None | Some(null) =>
        Right(None)
      case Some(value: String) if value.nonEmpty =>
        Right(Some(value))
      case Some(value: String) =>
        Left(s"$path cannot be empty")
      case Some(value) =>
        Left(s"$path must be a string, but found ${value.getClass.getSimpleName}")

  private def optionalInt(
      values: Map[String, Any],
      key: String,
      path: String
  ): Either[String, Option[Int]] =
    values.get(key) match
      case None | Some(null) =>
        Right(None)
      case Some(value: java.lang.Number) =>
        val longValue = value.longValue()
        Either.cond(
          value.doubleValue() == longValue.toDouble,
          longValue,
          s"$path must be an integer"
        ).flatMap(nonNegativeInt(path, _)).map(Some(_))
      case Some(value) =>
        Left(s"$path must be an integer, but found ${value.getClass.getSimpleName}")

  private def nonNegativeInt(path: String, value: Long): Either[String, Int] =
    Either.cond(
      value >= 0 && value <= Int.MaxValue,
      value.toInt,
      s"$path must be between 0 and ${Int.MaxValue}"
    )

  private def asMap(value: Any, path: String): Either[String, Map[String, Any]] =
    value match
      case map: java.util.Map[?, ?] =>
        map.asScala.toList
          .traverse {
            case (key: String, value) => Right(key -> value)
            case (key, _)             => Left(s"$path contains a non-string key: $key")
          }
          .map(_.toMap)
      case other =>
        Left(s"$path must be a YAML object, but found ${other.getClass.getSimpleName}")

  private def validate(config: AppConfig): Either[String, AppConfig] =
    if config.initialCustomerCount < 0 then
      Left("initialCustomerCount must be non-negative")
    else
      config.environment match
        case ExecutionEnvironment.Local =>
          Right(config)
        case ExecutionEnvironment.Staging | ExecutionEnvironment.Production =>
          config.kafka match
            case Some(_) =>
              Right(config)
            case None =>
              Left(s"Kafka config is required when environment is ${config.environment}")
