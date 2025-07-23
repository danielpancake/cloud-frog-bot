package com.danielpancake.cloudfrog.services.storage.cloud

import com.danielpancake.cloudfrog.services.api._
import com.danielpancake.cloudfrog.services.api.yandex.YandexAPIObjects._
import com.danielpancake.cloudfrog.services.api.yandex.YandexDiskAPI

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito._

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.scalatestplus.mockito.MockitoSugar

import sttp.model.StatusCode

class YandexDiskSpec extends AnyFlatSpec with MockitoSugar with ScalaFutures with Matchers {

  implicit val api: YandexDiskAPI[IO] = mock[YandexDiskAPI[IO]]
  val yandexDisk                      = new YandexDisk[IO]

  val oauthToken = OAuthToken("token", "type", 1, "refresh")

  "authorize" should "return successful result for valid code" in {
    when(api.exchangeCodeForToken(anyString())).thenReturn(IO(Right(oauthToken)))

    val call = yandexDisk.authorize("code")
    call.unsafeRunSync() shouldBe APIResult.Success(oauthToken.access_token)
  }

  it should "return failure result for invalid code" in {
    when(api.exchangeCodeForToken(anyString())).thenReturn(IO(Left(APIError("error", "error"))))

    val call = yandexDisk.authorize("code")
    call.unsafeRunSync() shouldBe APIResult.Failure(APIError("error", "error"))
  }

  "testConnection" should "return successful result for valid token" in {
    when(api.testConnection(anyString())).thenReturn(IO(StatusCode(200)))

    val call = yandexDisk.testConnection("token")
    call.unsafeRunSync() shouldBe APIResult.Success
  }

  it should "return failure result for invalid token" in {
    when(api.testConnection(anyString())).thenReturn(IO(StatusCode(401)))

    val call = yandexDisk.testConnection("token")
    call.unsafeRunSync() shouldBe APIResult.Failure(APIError("Connection failed", "Status code: 401"))
  }

  "uploadFile" should "return success for completed upload" in {
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.getOperationStatus(anyString(), anyString())).thenReturn(IO(Right(OperationStatus.Success)))

    val call = yandexDisk.uploadFile("token", "url", "path")
    call.unsafeRunSync() shouldBe APIResult.Success
  }

  {
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Right(Link("href"))))

    it should "return failure for failed upload" in {
      when(api.getOperationStatus(anyString(), anyString())).thenReturn(IO(Right(OperationStatus.Failure)))

      val call = yandexDisk.uploadFile("token", "url", "path")
      call.unsafeRunSync() shouldBe APIResult.Failure(APIError("Upload failed", "Reason unknown"))
    }

    it should "return failure for upload timeout" in {
      when(api.getOperationStatus(anyString(), anyString())).thenReturn(IO(Right(OperationStatus.InProgress)))

      val call = yandexDisk.uploadFile("token", "url", "path")
      call.unsafeRunSync() shouldBe APIResult.Failure(APIError("Upload failed", "Reason unknown"))
    }
  }

  it should "return failure for failed upload status request" in {
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.getOperationStatus(anyString(), anyString())).thenReturn(IO(Left(APIError("error", "error"))))

    val call = yandexDisk.uploadFile("token", "url", "path")
    call.unsafeRunSync() shouldBe APIResult.Failure(APIError("error", "error"))
  }

  it should "return failure for failed upload link request" in {
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Left(APIError("error", "error"))))

    val call = yandexDisk.uploadFile("token", "url", "path")
    call.unsafeRunSync() shouldBe APIResult.Failure(APIError("error", "error"))
  }

  it should "return failure for failed makedirs request" in {
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Left(APIError("error", "error"))))

    val call = yandexDisk.uploadFile("token", "url", "path")
    call.unsafeRunSync() shouldBe APIResult.Failure(APIError("error", "error"))
  }
}
