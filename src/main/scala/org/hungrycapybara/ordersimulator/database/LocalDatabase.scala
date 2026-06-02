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
  def isActive = column[Boolean]("is_active")

  def * = (customerId, signupDate, homeCity, favoriteCuisines, loyaltyTier, averageOrderValue, isActive).mapTo[Customer]

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
  private val randomSql = SimpleFunction.nullary[Double]("random")

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

  def runAction[A](action: DBIO[A])(using database: Database): IO[A] =
    IO.fromFuture(IO(database.run(action)))

  def runTransaction[A](action: DBIO[A])(using database: Database): IO[A] =
    IO.fromFuture(IO(database.run(action.transactionally)))

  def randomCustomerAction: DBIO[Option[Customer]] =
    customersDb
      .sortBy(_ => randomSql)
      .take(1)
      .result
      .headOption

  def insertCustomerAction(customer: Customer): DBIO[Int] =
    customersDb += customer
