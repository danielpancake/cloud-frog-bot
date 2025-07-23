package com.danielpancake.cloudfrog.services.cache

import dev.profunktor.redis4cats.RedisCommands

/** Implements a token cache using Redis
  */
class RedisCache[F[_]](implicit
    val redis: RedisCommands[F, String, String]
) extends TokenCache[F, String, String] {
  def get(key: String): F[Option[String]]      = redis.get(key)
  def set(key: String, value: String): F[Unit] = redis.set(key, value)
  def del(key: String): F[Long]                = redis.del(key)
}
