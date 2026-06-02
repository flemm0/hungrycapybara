package org.hungrycapybara.ordersimulator

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import org.hungrycapybara.ordersimulator.model.ExecutionEnvironment
import org.hungrycapybara.ordersimulator.database.LocalDatabase
import org.hungrycapybara.ordersimulator.helper.SeedData

object App extends IOApp.Simple:
  // TODO: remove the local declaration once done with testing
  private val executionEnv = ExecutionEnvironment.Local

  private val eventGenerators: List[EventGenerator] = List(
    CustomerProfileEvents,
    // RestaurantCatalogEvents,
    // OrderEvents,
    // CartEvents,
    // OfferEvents,
    // NotificationEvents
  )

  def run: IO[Unit] =
    // LocalDatabase.resource.use { database =>
    //   val customers = SeedData.customers(100)
    //   val restaurants = SeedData.restaurants(25)

    //   IO.println("Starting local database...") *>
    //     LocalDatabase.initialize(database, customers, restaurants) *>
    //     IO.println("Starting event generation...") *>
    //     eventGenerators.parTraverse_(_.run(executionEnv, database))
    // }
    IO.println("Starting event generation...") *>
      eventGenerators.parTraverse_(_.run(executionEnv))
