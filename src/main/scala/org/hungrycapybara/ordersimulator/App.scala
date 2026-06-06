package org.hungrycapybara.ordersimulator

import cats.effect.{IO, IOApp, ExitCode}
import cats.syntax.all.*
import org.hungrycapybara.ordersimulator.config.AppConfig
import org.hungrycapybara.ordersimulator.config.ConfigLoader
import org.hungrycapybara.ordersimulator.generators.{
  CustomerProfileEventGenerator,
  RestaurantCatalogEventGenerator,
  CustomerSessionEventGenerator,
  RestaurantBrowseEventGenerator,
  MenuInteractionEventGenerator,
  CartEventGenerator,
  PromotionEventGenerator,
  OrderEventGenerator
}
import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment
import org.hungrycapybara.ordersimulator.publisher.{ConsoleEventPublisher, KafkaEventPublisher}

object App extends IOApp:
  private val eventGenerators: List[EventGenerator] = List(
    CustomerProfileEventGenerator,
    RestaurantCatalogEventGenerator,
    CustomerSessionEventGenerator,
    RestaurantBrowseEventGenerator,
    MenuInteractionEventGenerator,
    CartEventGenerator,
    PromotionEventGenerator,
    OrderEventGenerator
  )

  private def selectPublisher(config: AppConfig): EventPublisher =
    config.environment match
      case ExecutionEnvironment.Local =>
        ConsoleEventPublisher
      case ExecutionEnvironment.Staging | ExecutionEnvironment.Production =>
        KafkaEventPublisher

  override def run(args: List[String]): IO[ExitCode] =
    // LocalDatabase.resource.use { database =>
    //   val customers = SeedData.customers(100)
    //   val restaurants = SeedData.restaurants(25)

    //   IO.println("Starting local database...") *>
    //     LocalDatabase.initialize(database, customers, restaurants) *>
    //     IO.println("Starting event generation...") *>
    //     eventGenerators.parTraverse_(_.run(executionEnv, database))
    // }
    ConfigLoader.load(args).flatMap { config =>
      val publisher = selectPublisher(config)
      IO.println(s"Starting event generation in ${config.environment}...") *>
        eventGenerators.parTraverse_(_.run(config.environment, publisher))
    }.as(ExitCode.Success)
