package org.hungrycapybara.ordersimulator.helper

import org.hungrycapybara.ordersimulator.model.{PriceRange, Restaurant}

class RestaurantDatabaseSuite extends munit.FunSuite:
  test("tracks restaurants when creating") {
    val database = RestaurantDatabase.from(List(restaurant("restaurant-1")))

    assertEquals(database.size, 1)

    database.create(restaurant("restaurant-2"))

    assertEquals(database.size, 2)
  }

  test("updates a random restaurant in place") {
    val database = RestaurantDatabase.from(List(restaurant("restaurant-1")))

    val updated = database.updateRandom { existing =>
      existing.copy(name = "Updated Kitchen", rating = 4.8)
    }

    assertEquals(updated.map(_.restaurantId), Some("restaurant-1"))
    assertEquals(updated.map(_.name), Some("Updated Kitchen"))
    assertEquals(updated.map(_.rating), Some(4.8))
  }

  private def restaurant(restaurantId: String): Restaurant =
    Restaurant(
      restaurantId = restaurantId,
      name = "Sample Kitchen",
      cuisineTypes = List("Thai"),
      rating = 4.5,
      priceRange = PriceRange.Medium
    )
