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

  private def parseLoyaltyTier(value: String): CustomerLoyaltyTier =
    value match
      case "Bronze"   => CustomerLoyaltyTier.Bronze
      case "Silver"   => CustomerLoyaltyTier.Silver
      case "Gold"     => CustomerLoyaltyTier.Gold
      case "Platinum" => CustomerLoyaltyTier.Platinum
      case _           => CustomerLoyaltyTier.Bronze

  private def parsePriceRange(value: String): PriceRange =
    value match
      case "Low"     => PriceRange.Low
      case "Medium"  => PriceRange.Medium
      case "High"    => PriceRange.High
      case "Premium" => PriceRange.Premium
      case _          => PriceRange.Medium

  def randomCuisine(): String =
    faker.cuisine().name()

  def randomMenuItemName(cuisine: Option[String] = None): String =
    cuisine
      .map(faker.menuItem().byCuisine)
      .getOrElse(faker.menuItem().byCuisine(randomCuisine()))

  def randomPromotionOfferType(): String =
    faker.promotion().offerType()

  def randomPromotionTrigger(): String =
    faker.promotion().trigger()

  def randomSearchQuery(): String =
    faker.searchQuery().term()

  def randomSessionDeviceType(): String =
    faker.sessionMetadata().deviceType()

  def randomSessionAppVersion(): String =
    faker.sessionMetadata().appVersion()

  def randomSessionEntryPoint(): String =
    faker.sessionMetadata().entryPoint()

  def randomServiceAreaCity(): String =
    faker.serviceArea().city()

  def randomLoyaltyTier(): CustomerLoyaltyTier =
    parseLoyaltyTier(faker.profileDistribution().loyaltyTierWeighted())

  def randomPriceRange(): PriceRange =
    parsePriceRange(faker.profileDistribution().priceRangeWeighted())

  def randomCustomer(): Customer =
    val customerId = UUID.randomUUID().toString
    val signupDate = Instant.now().minusSeconds(Random.nextInt(365 * 24 * 3600))
    val homeCity = randomServiceAreaCity()
    val favoriteCuisines =
      LazyList.continually(faker.cuisine().name()).distinct.take(Random.nextInt(3) + 1).toList
    val loyaltyTier = randomLoyaltyTier()
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
    val priceRange = randomPriceRange()

    Restaurant(restaurantId, name, cuisineTypes, rating, priceRange)

  def customers(count: Int): Seq[Customer] =
    (1 to count).map(_ => randomCustomer())

  def restaurants(count: Int): Seq[Restaurant] =
    (1 to count).map(_ => randomRestaurant())
