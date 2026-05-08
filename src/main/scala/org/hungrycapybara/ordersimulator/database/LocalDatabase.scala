package org.hungrycapybara.ordersimulator.database

import cats.effect.{IO, Resource}
import org.hungrycapybara.ordersimulator.model.{
  Customer,
  CustomerLoyaltyTier,
  PriceRange,
  Restaurant
}
import slick.jdbc.SQLiteProfile.api.*

private given ColumnType[List[String]] = MappedColumnType.base[List[String], String](
  list => list.mkString(","),
  str => if str.isEmpty then List.empty else str.split(",").toList
)

private given ColumnType[CustomerLoyaltyTier] = MappedColumnType.base[CustomerLoyaltyTier, String](
  tier => tier.toString,
  str => CustomerLoyaltyTier.valueOf(str)
)

private given ColumnType[PriceRange] = MappedColumnType.base[PriceRange, String](
  priceRange => priceRange.toString,
  str => PriceRange.valueOf(str)
)

private class Customers(tag: Tag) extends Table[Customer](tag, "customers"):
  def customerId = column[String]("customer_id", O.PrimaryKey)
  def signupDate = column[java.time.Instant]("signup_date")
  def homeCity = column[String]("home_city")
  def favoriteCuisines = column[List[String]]("favorite_cuisines")
  def loyaltyTier = column[CustomerLoyaltyTier]("loyalty_tier")
  def averageOrderValue = column[Double]("average_order_value")

  def * = (customerId, signupDate, homeCity, favoriteCuisines, loyaltyTier, averageOrderValue).mapTo[Customer]

private class Restaurants(tag: Tag) extends Table[Restaurant](tag, "restaurants"):
  def restaurantId = column[String]("restaurant_id", O.PrimaryKey)
  def name = column[String]("name")
  def cuisineTypes = column[List[String]]("cuisine_types")
  def rating = column[Double]("rating")
  def priceRange = column[PriceRange]("price_range")

  def * = (restaurantId, name, cuisineTypes, rating, priceRange).mapTo[Restaurant]

object LocalDatabase:
  private val customersDb = TableQuery[Customers]
  private val restaurantsDb = TableQuery[Restaurants]

  def resource: Resource[IO, Database] =
    Resource.fromAutoCloseable {
      IO.blocking(Database.forURL("jdbc:sqlite::memory:?cache=shared", driver = "org.sqlite.JDBC"))
    }

  def initialize(
      database: Database,
      customers: Seq[Customer],
      restaurants: Seq[Restaurant]
  ): IO[Unit] =
    val setup = DBIO.seq(
      (customersDb.schema ++ restaurantsDb.schema).create,
      insertAll(customersDb, customers),
      insertAll(restaurantsDb, restaurants)
    ).transactionally

    IO.fromFuture(IO(database.run(setup))).void

  private def insertAll[A, T <: Table[A]](
      table: TableQuery[T],
      rows: Seq[A]
  ): DBIO[Unit] =
    if rows.isEmpty then DBIO.successful(())
    else DBIO.seq(table ++= rows)

  def getCustomerById(database: Database, customerId: String): IO[Option[Customer]] =
    val query =
      customersDb
        .filter(_.customerId === customerId)
        .result
        .headOption
    IO.fromFuture(IO(database.run(query)))

  def randomCustomer(database: Database): IO[Option[Customer]] =
    val query =
      customersDb
        .sortBy(_ => SimpleFunction.nullary[Long]("random"))
        .take(1)
        .result
        .headOption
    IO.fromFuture(IO(database.run(query)))  
  
  def updateCustomer(database: Database, customer: Customer): IO[Unit] =
    val query =
      customersDb
        .filter(_.customerId === customer.customerId)
        .update(customer)
    IO.fromFuture(IO(database.run(query))).void

  def deleteCustomer(database: Database, customerId: String): IO[Unit] =
    val query =
      customersDb
        .filter(_.customerId === customerId)
        .delete
    IO.fromFuture(IO(database.run(query))).void

  def insertCustomer(database: Database, customer: Customer): IO[Unit] =
    val query = customersDb += customer
    IO.fromFuture(IO(database.run(query))).void

  def getRestaurantById(database: Database, restaurantId: String): IO[Option[Restaurant]] =
    val query =
      restaurantsDb
        .filter(_.restaurantId === restaurantId)
        .result
        .headOption
    IO.fromFuture(IO(database.run(query)))

  def randomRestaurant(database: Database): IO[Option[Restaurant]] =
    val query =
      restaurantsDb
        .sortBy(_ => SimpleFunction.nullary[Long]("random"))
        .take(1)
        .result
        .headOption
    IO.fromFuture(IO(database.run(query)))

  def updateRestaurant(database: Database, restaurant: Restaurant): IO[Unit] =
    val query =
      restaurantsDb
        .filter(_.restaurantId === restaurant.restaurantId)
        .update(restaurant)
    IO.fromFuture(IO(database.run(query))).void

  def deleteRestaurant(database: Database, restaurantId: String): IO[Unit] =
    val query =
      restaurantsDb
        .filter(_.restaurantId === restaurantId)
        .delete
    IO.fromFuture(IO(database.run(query))).void

  def insertRestaurant(database: Database, restaurant: Restaurant): IO[Unit] =
    val query = restaurantsDb += restaurant
    IO.fromFuture(IO(database.run(query))).void
