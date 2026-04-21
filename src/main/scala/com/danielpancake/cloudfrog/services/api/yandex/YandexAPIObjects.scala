package com.danielpancake.cloudfrog.services.api.yandex

import com.danielpancake.cloudfrog.services.api.APIError

import io.circe.Decoder

object YandexAPIObjects {
  // https://yandex.ru/dev/id/doc/ru/codes/screen-code-oauth#token-response
  final case class IDError(error: String, error_description: String)
  implicit val idErrorConverter: IDError => APIError = err => APIError(err.error, err.error_description)

  // https://yandex.ru/dev/disk/api/reference/response-objects.html#error
  final case class DiskError(error: String, description: String)
  implicit val diskErrorConverter: DiskError => APIError = err => APIError(err.error, err.description)

  // https://yandex.ru/dev/id/doc/ru/codes/screen-code-oauth#token-response
  final case class OAuthToken(
      accessToken: String,
      tokenType: String,
      expiresIn: Int,
      refreshToken: String
  )

  object OAuthToken {
    implicit val decoder: Decoder[OAuthToken] = Decoder.forProduct4(
      "access_token",
      "token_type",
      "expires_in",
      "refresh_token"
    )(OAuthToken.apply)
  }

  // https://yandex.ru/dev/disk/api/reference/response-objects.html#link
  final case class Link(
      href: String,
      method: String = "GET",
      templated: Boolean = false
  )

  // https://yandex.com/dev/disk/api/reference/operations.html
  final case class Status(status: String)

  object Status {
    def toOperationStatus(s: Status): OperationStatus = s.status match {
      case "success"     => OperationStatus.Success
      case "in-progress" => OperationStatus.InProgress
      case "failure"     => OperationStatus.Failure
      case _             => OperationStatus.Unknown
    }
  }

  sealed trait OperationStatus

  object OperationStatus {
    case object Success    extends OperationStatus
    case object InProgress extends OperationStatus
    case object Failure    extends OperationStatus
    case object Unknown    extends OperationStatus
  }
}
