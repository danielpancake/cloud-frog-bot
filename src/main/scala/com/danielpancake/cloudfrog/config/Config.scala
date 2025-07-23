package com.danielpancake.cloudfrog.config

import cats.effect.IO

final case class BotConfig(token: String)

final case class RedisConfig(uri: String)

final case class YandexAPIConfig(clientId: String, clientSecret: String)

final case class Config(bot: BotConfig, redis: RedisConfig, yandexAPI: YandexAPIConfig)

object Config {
  import pureconfig._
  import pureconfig.generic.auto._

  def load(): IO[Either[String, Config]] =
    IO.delay(ConfigSource.default.load[Config].left.map(_.prettyPrint()))
}
