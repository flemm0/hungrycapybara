package org.hungrycapybara.ordersimulator.helper

import org.hungrycapybara.ordersimulator.model.Customer

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

final class CustomerUserbase private (
    private val customers: ArrayBuffer[Customer],
    private val activeIndexes: ArrayBuffer[Int],
    private val activePositions: ArrayBuffer[Int]
):
  def size: Int = synchronized {
    customers.size
  }

  def activeCount: Int = synchronized {
    activeIndexes.size
  }

  def randomActiveCustomer(): Option[Customer] = synchronized {
    randomActiveIndex.map(customers(_))
  }

  def create(customer: Customer): Customer = synchronized {
    val index = customers.size
    customers += customer

    if customer.isActive then
      activePositions += activeIndexes.size
      activeIndexes += index
    else
      activePositions += -1

    customer
  }

  def updateRandomActive(update: Customer => Customer): Option[Customer] = synchronized {
    randomActiveIndex.map { customerIndex =>
      val updated = update(customers(customerIndex))
      customers(customerIndex) = updated

      if !updated.isActive then
        deactivate(customerIndex)

      updated
    }
  }

  def deleteRandomActive(): Option[Customer] = synchronized {
    randomActiveIndex.map { customerIndex =>
      val deleted = customers(customerIndex).copy(isActive = false)
      customers(customerIndex) = deleted
      deactivate(customerIndex)
      deleted
    }
  }

  private def randomActiveIndex: Option[Int] =
    Option.when(activeIndexes.nonEmpty) {
      activeIndexes(Random.nextInt(activeIndexes.size))
    }

  private def deactivate(customerIndex: Int): Unit =
    val position = activePositions(customerIndex)

    if position >= 0 then
      val lastCustomerIndex = activeIndexes.last
      activeIndexes(position) = lastCustomerIndex
      activePositions(lastCustomerIndex) = position
      activeIndexes.remove(activeIndexes.size - 1)
      activePositions(customerIndex) = -1

object CustomerUserbase:
  def seed(count: Int): CustomerUserbase =
    require(count >= 0, "count must be non-negative")

    val customers = ArrayBuffer.empty[Customer]
    val activeIndexes = ArrayBuffer.empty[Int]
    val activePositions = ArrayBuffer.empty[Int]

    customers.sizeHint(count)
    activeIndexes.sizeHint(count)
    activePositions.sizeHint(count)

    var remaining = count
    while remaining > 0 do
      appendInitial(SeedData.randomCustomer(), customers, activeIndexes, activePositions)
      remaining -= 1

    new CustomerUserbase(customers, activeIndexes, activePositions)

  def from(customers: Iterable[Customer]): CustomerUserbase =
    val customerBuffer = ArrayBuffer.empty[Customer]
    val activeIndexes = ArrayBuffer.empty[Int]
    val activePositions = ArrayBuffer.empty[Int]

    customers.foreach { customer =>
      appendInitial(customer, customerBuffer, activeIndexes, activePositions)
    }

    new CustomerUserbase(customerBuffer, activeIndexes, activePositions)

  private def appendInitial(
      customer: Customer,
      customers: ArrayBuffer[Customer],
      activeIndexes: ArrayBuffer[Int],
      activePositions: ArrayBuffer[Int]
  ): Unit =
    val index = customers.size
    customers += customer

    if customer.isActive then
      activePositions += activeIndexes.size
      activeIndexes += index
    else
      activePositions += -1
