package org.hungrycapybara.ordersimulator.helper

import org.hungrycapybara.ordersimulator.model.Restaurant

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

final class RestaurantDatabase private (private val restaurants: ArrayBuffer[Restaurant]):
  def size: Int = synchronized {
    restaurants.size
  }

  def randomRestaurant(): Option[Restaurant] = synchronized {
    randomRestaurantIndex.map(restaurants(_))
  }

  def create(restaurant: Restaurant): Restaurant = synchronized {
    restaurants += restaurant
    restaurant
  }

  def updateRandom(update: Restaurant => Restaurant): Option[Restaurant] = synchronized {
    randomRestaurantIndex.map { index =>
      val updated = update(restaurants(index))
      restaurants(index) = updated
      updated
    }
  }

  private def randomRestaurantIndex: Option[Int] =
    Option.when(restaurants.nonEmpty) {
      Random.nextInt(restaurants.size)
    }

object RestaurantDatabase:
  def seed(count: Int): RestaurantDatabase =
    require(count >= 0, "count must be non-negative")

    val restaurants = ArrayBuffer.empty[Restaurant]
    restaurants.sizeHint(count)

    var remaining = count
    while remaining > 0 do
      restaurants += SeedData.randomRestaurant()
      remaining -= 1

    new RestaurantDatabase(restaurants)

  def from(restaurants: Iterable[Restaurant]): RestaurantDatabase =
    new RestaurantDatabase(ArrayBuffer.from(restaurants))
