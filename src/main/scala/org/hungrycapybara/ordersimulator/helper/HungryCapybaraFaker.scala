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

private class PromotionProvider(faker: BaseProviders) extends AbstractProvider[BaseProviders](faker):
  private val key = "promotions"
  private val dataUrl = Option(getClass.getClassLoader.getResource("promotions.yaml"))
    .getOrElse(throw new IllegalStateException("Could not find promotions.yaml on the classpath"))

  faker.addUrl(Locale.ENGLISH, dataUrl)

  def offerType(): String =
    resolve(s"$key.offer_types")

  def trigger(): String =
    resolve(s"$key.triggers")

private class SearchQueryProvider(faker: BaseProviders) extends AbstractProvider[BaseProviders](faker):
  private val key = "search_queries"
  private val dataUrl = Option(getClass.getClassLoader.getResource("search_queries.yaml"))
    .getOrElse(throw new IllegalStateException("Could not find search_queries.yaml on the classpath"))

  faker.addUrl(Locale.ENGLISH, dataUrl)

  def term(): String =
    resolve(s"$key.terms")

private class SessionMetadataProvider(faker: BaseProviders) extends AbstractProvider[BaseProviders](faker):
  private val key = "session_metadata"
  private val dataUrl = Option(getClass.getClassLoader.getResource("session_metadata.yaml"))
    .getOrElse(throw new IllegalStateException("Could not find session_metadata.yaml on the classpath"))

  faker.addUrl(Locale.ENGLISH, dataUrl)

  def deviceType(): String =
    resolve(s"$key.device_types")

  def appVersion(): String =
    resolve(s"$key.app_versions")

  def entryPoint(): String =
    resolve(s"$key.entry_points")

private class HungryCapybaraFaker extends Faker:
  def cuisine(): CuisineProvider =
    getProvider(classOf[CuisineProvider], (faker: BaseProviders) => new CuisineProvider(faker))

  def menuItem(): MenuItemProvider =
    getProvider(classOf[MenuItemProvider], (faker: BaseProviders) => new MenuItemProvider(faker))

  def promotion(): PromotionProvider =
    getProvider(classOf[PromotionProvider], (faker: BaseProviders) => new PromotionProvider(faker))

  def searchQuery(): SearchQueryProvider =
    getProvider(classOf[SearchQueryProvider], (faker: BaseProviders) => new SearchQueryProvider(faker))

  def sessionMetadata(): SessionMetadataProvider =
    getProvider(classOf[SessionMetadataProvider], (faker: BaseProviders) => new SessionMetadataProvider(faker))
