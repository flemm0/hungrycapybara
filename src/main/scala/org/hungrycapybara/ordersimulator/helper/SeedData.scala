package org.hungrycapybara.ordersimulator.helper

import java.time.Instant
import java.util.UUID
import org.hungrycapybara.ordersimulator.model.{
  Customer,
  CustomerLoyaltyTier,
  PriceRange,
  Restaurant
}
import scala.util.Random

object SeedData:
  private val faker = new HungryCapybaraFaker()

  def randomCuisine(): String =
    faker.cuisine().name()

  def randomMenuItemName(cuisine: Option[String] = None): String =
    cuisine
      .map(faker.menuItem().byCuisine)
      .getOrElse(faker.menuItem().byCuisine(randomCuisine()))

  def randomCustomer(): Customer =
    val customerId = UUID.randomUUID().toString
    val signupDate = Instant.now().minusSeconds(Random.nextInt(365 * 24 * 3600))
    val homeCity = faker.address().city()
    val favoriteCuisines =
      LazyList.continually(faker.cuisine().name()).distinct.take(Random.nextInt(3) + 1).toList
    val loyaltyTier = Random
      .shuffle(
        List(
          CustomerLoyaltyTier.Bronze,
          CustomerLoyaltyTier.Silver,
          CustomerLoyaltyTier.Gold,
          CustomerLoyaltyTier.Platinum
        )
      )
      .head
    val averageOrderValue = Random.nextDouble() * 100

    Customer(
      customerId,
      signupDate,
      homeCity,
      favoriteCuisines,
      loyaltyTier,
      averageOrderValue,
      isActive = true
    )

  def randomRestaurant(): Restaurant =
    val restaurantId = UUID.randomUUID().toString
    val name = s"${faker.company().name()} Kitchen"
    val cuisineTypes =
      LazyList.continually(faker.cuisine().name()).distinct.take(Random.nextInt(3) + 1).toList
    val rating = BigDecimal(3.0 + Random.nextDouble() * 2.0)
      .setScale(1, BigDecimal.RoundingMode.HALF_UP)
      .toDouble
    val priceRange = Random
      .shuffle(List(PriceRange.Low, PriceRange.Medium, PriceRange.High, PriceRange.Premium))
      .head

    Restaurant(restaurantId, name, cuisineTypes, rating, priceRange)

  def customers(count: Int): Seq[Customer] =
    (1 to count).map(_ => randomCustomer())

  def restaurants(count: Int): Seq[Restaurant] =
    (1 to count).map(_ => randomRestaurant())
