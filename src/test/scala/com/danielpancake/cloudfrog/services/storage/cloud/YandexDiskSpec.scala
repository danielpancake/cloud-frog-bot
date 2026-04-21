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

  private def fixture(): (YandexDiskAPI[IO], YandexDisk[IO]) = {
    val api = mock[YandexDiskAPI[IO]]
    implicit val implicitApi: YandexDiskAPI[IO] = api
    val disk = new YandexDisk[IO]
    (api, disk)
  }

  val oauthToken = OAuthToken("token", "bearer", 3600, "refresh")

  "authorize" should "return successful result for valid code" in {
    val (api, disk) = fixture()
    when(api.exchangeCodeForToken(anyString())).thenReturn(IO(Right(oauthToken)))

    disk.authorize("code").unsafeRunSync() shouldBe APIResult.Success(oauthToken.accessToken)
  }

  it should "return failure result for invalid code" in {
    val (api, disk) = fixture()
    when(api.exchangeCodeForToken(anyString())).thenReturn(IO(Left(APIError("error", "error"))))

    disk.authorize("code").unsafeRunSync() shouldBe APIResult.Failure(APIError("error", "error"))
  }

  "testConnection" should "return successful result for valid token" in {
    val (api, disk) = fixture()
    when(api.testConnection(anyString())).thenReturn(IO(StatusCode(200)))

    disk.testConnection("token").unsafeRunSync() shouldBe APIResult.Success
  }

  it should "return failure result for invalid token" in {
    val (api, disk) = fixture()
    when(api.testConnection(anyString())).thenReturn(IO(StatusCode(401)))

    disk.testConnection("token").unsafeRunSync() shouldBe APIResult.Failure(APIError("Connection failed", "Status code: 401"))
  }

  "uploadFile" should "return success for completed upload" in {
    val (api, disk) = fixture()
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.getOperationStatus(anyString(), anyString())).thenReturn(IO(Right(OperationStatus.Success)))

    disk.uploadFile("token", "url", "dir/file.txt").unsafeRunSync() shouldBe APIResult.Success
  }

  it should "return failure for failed upload" in {
    val (api, disk) = fixture()
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.getOperationStatus(anyString(), anyString())).thenReturn(IO(Right(OperationStatus.Failure)))

    disk.uploadFile("token", "url", "dir/file.txt").unsafeRunSync() shouldBe APIResult.Failure(APIError("Upload failed", "Reason unknown"))
  }

  it should "return failure for upload timeout" in {
    val (api, disk) = fixture()
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.getOperationStatus(anyString(), anyString())).thenReturn(IO(Right(OperationStatus.InProgress)))

    disk.uploadFile("token", "url", "dir/file.txt").unsafeRunSync() shouldBe APIResult.Failure(APIError("Upload failed", "Reason unknown"))
  }

  it should "return failure for failed upload status request" in {
    val (api, disk) = fixture()
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.getOperationStatus(anyString(), anyString())).thenReturn(IO(Left(APIError("error", "error"))))

    disk.uploadFile("token", "url", "dir/file.txt").unsafeRunSync() shouldBe APIResult.Failure(APIError("error", "error"))
  }

  it should "return failure for failed upload link request" in {
    val (api, disk) = fixture()
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Left(APIError("error", "error"))))

    disk.uploadFile("token", "url", "dir/file.txt").unsafeRunSync() shouldBe APIResult.Failure(APIError("error", "error"))
  }

  it should "return failure for failed makedirs request" in {
    val (api, disk) = fixture()
    when(api.makedirs(anyString(), anyString())).thenReturn(IO(Left(APIError("error", "error"))))

    disk.uploadFile("token", "url", "dir/file.txt").unsafeRunSync() shouldBe APIResult.Failure(APIError("error", "error"))
  }

  it should "skip makedirs for root-level files" in {
    val (api, disk) = fixture()
    when(api.uploadFileFromURL(anyString(), anyString(), anyString())).thenReturn(IO(Right(Link("href"))))
    when(api.getOperationStatus(anyString(), anyString())).thenReturn(IO(Right(OperationStatus.Success)))

    disk.uploadFile("token", "url", "file.txt").unsafeRunSync() shouldBe APIResult.Success
    verify(api, never()).makedirs(anyString(), anyString())
  }
}
