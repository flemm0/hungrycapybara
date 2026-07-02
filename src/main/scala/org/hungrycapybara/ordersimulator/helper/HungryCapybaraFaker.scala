package org.hungrycapybara.ordersimulator.helper

import net.datafaker.Faker
import net.datafaker.providers.base.{AbstractProvider, BaseProviders}
import java.util.Locale

private class CuisineProvider(faker: BaseProviders) extends AbstractProvider[BaseProviders](faker):
  private val key = "cuisines"
  private val dataUrl = Option(getClass.getClassLoader.getResource("cuisines.yaml"))
    .getOrElse(throw new IllegalStateException("Could not find cuisines.yaml on the classpath"))

  faker.addUrl(Locale.ENGLISH, dataUrl)

  def name(): String =
    resolve(s"$key.names")

private class MenuItemProvider(faker: BaseProviders) extends AbstractProvider[BaseProviders](faker):
  private val key = "menu_items"
  private val dataUrl = Option(getClass.getClassLoader.getResource("cuisines.yaml"))
    .getOrElse(throw new IllegalStateException("Could not find cuisines.yaml on the classpath"))

  faker.addUrl(Locale.ENGLISH, dataUrl)

  def fallback(): String =
    resolve(s"$key.fallback")
  
  def byCuisine(cuisineName: String): String =
    val cuisineKey = cuisineName
      .toLowerCase(Locale.ENGLISH)
      .replaceAll("[^a-z0-9]+", "_")
      .stripPrefix("_")
      .stripSuffix("_")
    
    try resolve(s"$key.by_cuisine.$cuisineKey")
    catch case _: Exception => fallback()

private class HungryCapybaraFaker extends Faker:
  def cuisine(): CuisineProvider =
    getProvider(classOf[CuisineProvider], (faker: BaseProviders) => new CuisineProvider(faker))

  def menuItem(): MenuItemProvider =
    getProvider(classOf[MenuItemProvider], (faker: BaseProviders) => new MenuItemProvider(faker))
