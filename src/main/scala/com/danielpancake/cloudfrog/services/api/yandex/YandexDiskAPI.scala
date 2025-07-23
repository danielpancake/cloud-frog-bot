package com.danielpancake.cloudfrog.services.api.yandex

import com.danielpancake.cloudfrog.config.YandexAPIConfig
import com.danielpancake.cloudfrog.services.api.APIError
import com.danielpancake.cloudfrog.services.api.Decoders._
import com.danielpancake.cloudfrog.services.api.yandex.YandexAPIObjects._
import com.danielpancake.cloudfrog.utils.Utils._

import cats.Monad
import cats.implicits._

import io.circe.generic.auto._

import sttp.client4._
import sttp.model.StatusCode

/** A synchronous wrapper for the Yandex.Disk API.
  *
  * @param config
  *   The Yandex.Disk API config
  * @param backend
  *   The HTTP backend to use for requests
  */
class YandexDiskAPI[F[_]: Monad](private val config: YandexAPIConfig)(implicit val backend: WebSocketBackend[F]) {

  /** See https://yandex.ru/dev/id/doc/en/codes/screen-code#token
    */
  def exchangeCodeForToken(code: String): F[Either[APIError, OAuthToken]] = {
    val baseURL = uri"https://oauth.yandex.ru/token"

    val request = basicRequest
      .post(baseURL)
      .body(
        Map(
          "grant_type"    -> "authorization_code",
          "code"          -> code,
          "client_id"     -> config.clientId,
          "client_secret" -> config.clientSecret
        )
      )

    val response = request.send(backend)
    response.map(handleResponse[IDError, OAuthToken])
  }

  /** See https://yandex.com/dev/disk/api/reference/upload-ext.html
    */
  def uploadFileFromURL(accessToken: String, url: String, path: String): F[Either[APIError, Link]] = {
    val baseURL = "https://cloud-api.yandex.net/v1/disk/resources/upload"

    val request = basicRequest
      .post(uri"$baseURL?path=$path&url=$url")
      .header("Authorization", s"OAuth $accessToken")

    val response = request.send(backend)
    response.map(handleResponse[DiskError, Link])
  }

  /** See https://yandex.com/dev/disk/api/reference/operations.html
    */
  def getOperationStatus(accessToken: String, linkHref: String): F[Either[APIError, OperationStatus]] = {
    val request = basicRequest
      .get(uri"$linkHref")
      .header("Authorization", s"OAuth $accessToken")

    val response = request.send(backend)
    response.map(handleResponse[DiskError, Status]).map(_.map(Status.unapply(_)))
  }

  /** See https://yandex.com/dev/disk/api/reference/create-folder.html
    */
  def mkdir(accessToken: String, path: String): F[Either[APIError, Link]] = {
    val baseURL = "https://cloud-api.yandex.net/v1/disk/resources"

    val request = basicRequest
      .put(uri"$baseURL?path=$path")
      .header("Authorization", s"OAuth $accessToken")

    val response = request.send(backend)
    response.map(handleResponse[DiskError, Link]).map {
      case Left(APIError("DiskPathPointsToExistentDirectoryError", _)) => Right(Link(s"$baseURL?path=disk:$path"))

      case Left(err) => Left(err)
      case link      => link
    }
  }

  def makedirs(accessToken: String, path: String): F[Either[APIError, Link]] =
    splitPathIntoSubpaths(path) match {
      case Nil => Monad[F].pure(Left(APIError("DiskPathIsEmptyError", "Path is empty")))
      case head :: tail =>
        tail.foldLeft(mkdir(accessToken, head)) { (acc, path) =>
          acc.flatMap {
            case Right(_) => mkdir(accessToken, path)
            case err      => err.pure[F]
          }
        }
    }

  /** Performs a test request to the Yandex.Disk API to check if the access token is valid.
    */
  def testConnection(accessToken: String): F[StatusCode] = {
    val request = basicRequest
      .get(uri"https://cloud-api.yandex.net/v1/disk/resources?path=disk:/")
      .header("Authorization", s"OAuth $accessToken")

    request.send(backend).map(_.code)
  }
}
