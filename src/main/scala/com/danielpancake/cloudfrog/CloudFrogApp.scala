package com.danielpancake.cloudfrog

import com.danielpancake.cloudfrog.bot.CloudFrogBot
import com.danielpancake.cloudfrog.config.Config
import com.danielpancake.cloudfrog.services.cache.RedisCache
import com.danielpancake.cloudfrog.services.storage.cloud.YandexDisk
import com.danielpancake.cloudfrog.services.api.yandex.YandexDiskAPI

import canoe.api.TelegramClient

import cats.effect.{ExitCode, IO, IOApp, Resource, Sync}

import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.effect.Log.Stdout._

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import sttp.client4.httpclient.cats.HttpClientCatsBackend
import sttp.client4.WebSocketBackend

object CloudFrogApp extends IOApp {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  def run(args: List[String]): IO[ExitCode] =
    Config
      .load()
      .flatMap {
        case Right(cfg) => Logger[IO].info("Succesfully loaded config!") *> init(cfg)
        case Left(err)  => Logger[IO].error(s"Failed to load config: $err") *> IO(ExitCode.Error)
      }

  def init(config: Config) =
    getResources(config).use {
      case Left(err) => Logger[IO].error(err).as(ExitCode.Error)

      case Right((telegram, redisClient, httpBackend)) => {
        implicit val redis: RedisCommands[IO, String, String] = redisClient

        implicit val backend: WebSocketBackend[IO] = httpBackend
        implicit val yandexAPI: YandexDiskAPI[IO]  = new YandexDiskAPI[IO](config.yandexAPI)

        val yandexDisk = new YandexDisk[IO]
        val redisCache = new RedisCache[IO]

        val cloudFrogBot = new CloudFrogBot(config, telegram, yandexDisk, redisCache)

        Logger[IO].info("Succesfully initialized resources!") *>
          Logger[IO].info("Starting CloudFrogBot...") *>
          cloudFrogBot.run.as(ExitCode.Success)
      }
    }

  def getResources(
      config: Config
  ): Resource[IO, Either[String, (TelegramClient[IO], RedisCommands[IO, String, String], WebSocketBackend[IO])]] = {
    val resources = for {
      telegram    <- TelegramClient[IO](config.bot.token)
      redisClient <- Redis[IO].utf8(config.redis.uri)
      httpBackend <- HttpClientCatsBackend.resource[IO]()
    } yield (telegram, redisClient, httpBackend)

    resources.attempt.map {
      case Left(err)  => Left(s"Failed to initialize resources: $err")
      case Right(res) => Right(res)
    }
  }
}
