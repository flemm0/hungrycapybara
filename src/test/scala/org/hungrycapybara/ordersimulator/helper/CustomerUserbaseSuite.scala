package org.hungrycapybara.ordersimulator.helper

import org.hungrycapybara.ordersimulator.model.{Customer, CustomerLoyaltyTier}

import java.time.Instant

class CustomerUserbaseSuite extends munit.FunSuite:
  test("tracks active customers when creating and deleting") {
    val userbase = CustomerUserbase.from(List(customer("customer-1"), customer("customer-2", isActive = false)))

    assertEquals(userbase.size, 2)
    assertEquals(userbase.activeCount, 1)

    userbase.create(customer("customer-3"))

    assertEquals(userbase.size, 3)
    assertEquals(userbase.activeCount, 2)

    val deleted = userbase.deleteRandomActive()

    assert(deleted.exists(!_.isActive))
    assertEquals(userbase.size, 3)
    assertEquals(userbase.activeCount, 1)
  }

  test("updates an active customer in place") {
    val userbase = CustomerUserbase.from(List(customer("customer-1")))

    val updated = userbase.updateRandomActive { existing =>
      existing.copy(homeCity = "Oakland", averageOrderValue = 42.50)
    }

    assertEquals(updated.map(_.customerId), Some("customer-1"))
    assertEquals(updated.map(_.homeCity), Some("Oakland"))
    assertEquals(updated.map(_.averageOrderValue), Some(42.50))
    assertEquals(userbase.activeCount, 1)
  }

  private def customer(customerId: String, isActive: Boolean = true): Customer =
    Customer(
      customerId = customerId,
      signupDate = Instant.parse("2025-01-01T00:00:00Z"),
      homeCity = "San Francisco",
      favoriteCuisines = List("Thai"),
      loyaltyTier = CustomerLoyaltyTier.Gold,
      averageOrderValue = 25.0,
      isActive = isActive
    )
