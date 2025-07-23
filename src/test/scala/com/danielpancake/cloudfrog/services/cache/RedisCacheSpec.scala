package com.danielpancake.cloudfrog.services.cache

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import dev.profunktor.redis4cats.RedisCommands

import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito._

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.scalatestplus.mockito.MockitoSugar

class RedisCacheSpec extends AnyFlatSpec with MockitoSugar with ScalaFutures with Matchers {

  implicit val redis: RedisCommands[IO, String, String] = mock[RedisCommands[IO, String, String]]

  val cache = new RedisCache[IO]

  "get" should "return cached value" in {
    when(redis.get(anyString())).thenReturn(IO(Some("value")))

    val call = cache.get("key")
    call.unsafeRunSync() shouldBe Some("value")
  }

  it should "return None for non-existent key" in {
    when(redis.get(anyString())).thenReturn(IO(None))

    val call = cache.get("key")
    call.unsafeRunSync() shouldBe None
  }

  "set" should "set value in cache" in {
    when(redis.set(anyString(), anyString())).thenReturn(IO.unit)

    val call = cache.set("key", "value")
    call.unsafeRunSync()
    verify(redis).set("key", "value")
  }
}
