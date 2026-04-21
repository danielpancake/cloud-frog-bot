package com.danielpancake.cloudfrog.services.api

import com.danielpancake.cloudfrog.services.api._
import com.danielpancake.cloudfrog.services.api.Decoders._
import com.danielpancake.cloudfrog.services.api.yandex.YandexAPIObjects._

import io.circe.generic.auto._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client4.Response
import sttp.model.StatusCode

class DecodersSpec extends AnyFlatSpec with Matchers {
  val jsonIdError1 = """{"error": "invalid_grant", "error_description": "Invalid code"}"""
  val jsonIdError2 = """{"error": "invalid_grant"}"""
  val jsonIdError3 =
    """{"error": "invalid_grant", "error_description": "Invalid code", "error_uri": "https://yandex.ru"}"""

  "decodeError" should "decode proper IDError to APIError" in {
    decodeError[IDError](jsonIdError1) shouldBe APIError("invalid_grant", "Invalid code")
  }

  it should "fail to decode IDError with missing field" in {
    decodeError[IDError](jsonIdError2) shouldBe APIError(
      s"Failed to decode error response: $jsonIdError2",
      "Attempt to decode value on failed cursor: DownField(error_description)"
    )
  }

  it should "decode proper IDError to APIError even with extra fields" in {
    decodeError[IDError](jsonIdError3) shouldBe APIError("invalid_grant", "Invalid code")
  }

  val jsonDiskError = """{"error": "invalid_grant", "description": "Invalid code"}"""

  "decodeError" should "decode proper DiskError to APIError" in {
    decodeError[DiskError](jsonDiskError) shouldBe APIError("invalid_grant", "Invalid code")
  }

  val jsonOAuthToken =
    """{"access_token": "AQAAAAA", "token_type": "bearer", "expires_in": 31536000, "refresh_token": "AQAAAAA"}"""

  val jsonLink =
    """{"href": "https://cloud-api.yandex.net/v1/disk/resources?path=app%3A%2F%2F%2Ftest%2Ftest", "method": "GET", "templated": false}"""

  "decodeBody" should "decode proper OAuthToken" in {
    decodeBody[OAuthToken](jsonOAuthToken) shouldBe Right(
      OAuthToken(
        "AQAAAAA",
        "bearer",
        31536000,
        "AQAAAAA"
      )
    )
  }

  it should "decode proper Link" in {
    decodeBody[Link](jsonLink) shouldBe Right(
      Link(
        "https://cloud-api.yandex.net/v1/disk/resources?path=app%3A%2F%2F%2Ftest%2Ftest",
        "GET",
        false
      )
    )
  }

  val sampleResponseSuccess = Response[Either[String, String]](Right(jsonOAuthToken), StatusCode.Ok)
  val sampleResponseError   = Response[Either[String, String]](Right(jsonIdError1), StatusCode.Ok)

  "handleResponse" should "handle successful json" in {
    handleResponse[IDError, OAuthToken](sampleResponseSuccess).isRight shouldBe true
  }

  it should "handle error json" in {
    handleResponse[IDError, OAuthToken](sampleResponseError).isLeft shouldBe true
  }
}
