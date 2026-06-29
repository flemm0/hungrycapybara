package org.hungrycapybara.ordersimulator

import cats.effect.{IO, IOApp, ExitCode}
import cats.syntax.all.*
import org.hungrycapybara.ordersimulator.config.AppConfig
import org.hungrycapybara.ordersimulator.config.ConfigLoader
import org.hungrycapybara.ordersimulator.core.{EventGenerator, EventPublisher}
import org.hungrycapybara.ordersimulator.generators.{
  CustomerProfileEventGenerator,
  RestaurantCatalogEventGenerator,
  SessionInteractionStore,
  CustomerSessionEventGenerator,
  RestaurantBrowseEventGenerator,
  MenuInteractionEventGenerator,
  CartEventGenerator,
  PromotionEventGenerator,
  OrderEventGenerator
}
import org.hungrycapybara.ordersimulator.helper.CustomerUserbase
import org.hungrycapybara.ordersimulator.helper.RestaurantDatabase
import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment
import org.hungrycapybara.ordersimulator.publisher.{ConsoleEventPublisher, KafkaEventPublisher}

object App extends IOApp:
  private def eventGenerators(
      customerUserbase: CustomerUserbase,
      restaurantDatabase: RestaurantDatabase,
      sessionInteractionStore: SessionInteractionStore
  ): List[EventGenerator] = List(
    CustomerProfileEventGenerator(customerUserbase),
    RestaurantCatalogEventGenerator(restaurantDatabase),
    CustomerSessionEventGenerator(customerUserbase, sessionInteractionStore),
    RestaurantBrowseEventGenerator(restaurantDatabase, sessionInteractionStore),
    MenuInteractionEventGenerator(restaurantDatabase, sessionInteractionStore),
    CartEventGenerator(restaurantDatabase, sessionInteractionStore),
    PromotionEventGenerator(sessionInteractionStore),
    OrderEventGenerator(restaurantDatabase, sessionInteractionStore)
  )

  private def selectPublisher(config: AppConfig): IO[EventPublisher] =
    config.environment match
      case ExecutionEnvironment.Local =>
        IO.pure(ConsoleEventPublisher)
      case ExecutionEnvironment.Staging | ExecutionEnvironment.Production =>
        config.kafka match
          case Some(kafkaConfig) =>
            IO.pure(KafkaEventPublisher(kafkaConfig))
          case None =>
            IO.raiseError(
              IllegalStateException(
                s"Kafka config is required when environment is ${config.environment}"
              )
            )

  override def run(args: List[String]): IO[ExitCode] =
    // LocalDatabase.resource.use { database =>
    //   val customers = SeedData.customers(100)
    //   val restaurants = SeedData.restaurants(25)

    //   IO.println("Starting local database...") *>
    //     LocalDatabase.initialize(database, customers, restaurants) *>
    //     IO.println("Starting event generation...") *>
    //     eventGenerators.parTraverse_(_.run(executionEnv, database))
    // }
    ConfigLoader
      .load(args)
      .flatMap { config =>
        for
          publisher <- selectPublisher(config)
          _ <- IO.println(s"Seeding ${config.initialCustomerCount} existing customers...")
          customerUserbase <- IO.blocking(CustomerUserbase.seed(config.initialCustomerCount))
          _ <- IO.println(s"Seeding ${config.initialRestaurantCount} existing restaurants...")
          restaurantDatabase <- IO.blocking(RestaurantDatabase.seed(config.initialRestaurantCount))
          sessionInteractionStore <- IO.pure(SessionInteractionStore.empty)
          _ <- IO.println(s"Starting event generation in ${config.environment}...")
          _ <- eventGenerators(customerUserbase, restaurantDatabase, sessionInteractionStore).parTraverse_(_.run(config.environment, publisher))
        yield ()
      }
      .as(ExitCode.Success)
