package org.hungrycapybara.ordersimulator.helper

import net.datafaker.Faker
import net.datafaker.providers.base.{AbstractProvider, BaseProviders}
import java.util.Locale

private class CuisineProvider(faker: BaseProviders) extends AbstractProvider[BaseProviders](faker):
  private val key = "cuisines"
  private val cuisinesUrl = Option(getClass.getClassLoader.getResource("cuisines.yaml"))
    .getOrElse(throw new IllegalStateException("Could not find cuisines.yaml on the classpath"))

  faker.addUrl(Locale.ENGLISH, cuisinesUrl)

  def name(): String =
    resolve(s"$key.names")

private class HungryCapybaraFaker extends Faker:
  def cuisine(): CuisineProvider =
    getProvider(classOf[CuisineProvider], (faker: BaseProviders) => new CuisineProvider(faker))
