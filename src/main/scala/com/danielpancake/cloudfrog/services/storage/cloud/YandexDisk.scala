package com.danielpancake.cloudfrog.services.storage.cloud

import com.danielpancake.cloudfrog.services.api._
import com.danielpancake.cloudfrog.services.api.yandex.YandexDiskAPI
import com.danielpancake.cloudfrog.services.api.yandex.YandexAPIObjects._
import com.danielpancake.cloudfrog.utils.Utils

import cats.effect.Temporal
import cats.implicits._

import scala.concurrent.duration._

/** Implements a cloud storage service for Yandex Disk
  */
class YandexDisk[F[_]: Temporal](implicit api: YandexDiskAPI[F]) extends CloudStorage[F] {
  def authorize(code: String): F[APIResult[String]] =
    api.exchangeCodeForToken(code).flatMap {
      case Right(token) => Temporal[F].pure(APIResult.Success(token.accessToken))
      case Left(err)    => Temporal[F].pure(APIResult.Failure(err))
    }

  def testConnection(accessToken: String): F[APIResult[Nothing]] =
    for {
      statuscode <- api.testConnection(accessToken)
    } yield {
      if (statuscode.isSuccess) {
        APIResult.Success
      } else {
        APIResult.Failure(APIError("Connection failed", s"Status code: $statuscode"))
      }
    }

  def uploadFile(accessToken: String, sourceURL: String, destinationPath: String): F[APIResult[Nothing]] = {
    val (path, _, _) = Utils.splitPathFilenameExt(destinationPath)

    val result: F[APIResult[Nothing]] = (for {
      _          <- if (path.nonEmpty) api.makedirs(accessToken, path).flatMap(Temporal[F].fromEither) else Temporal[F].unit
      uploadLink <- api.uploadFileFromURL(accessToken, sourceURL, destinationPath).flatMap(Temporal[F].fromEither)

      _ <- checkUploadStatus(accessToken, uploadLink.href)(attempts = 10).flatMap {
        case Right(OperationStatus.Success) => Temporal[F].pure(())

        case Right(_)  => Temporal[F].raiseError[Unit](APIError("Upload failed", "Reason unknown"))
        case Left(err) => Temporal[F].raiseError[Unit](err)
      }
    } yield APIResult.Success)

    result.handleErrorWith {
      case err: APIError => Temporal[F].pure(APIResult.Failure(err))
      case err           => Temporal[F].pure(APIResult.Failure(APIError("Upload failed", err.getMessage)))
    }
  }

  private def checkUploadStatus(accessToken: String, linkHref: String)(
      attempts: Int,
      interval: FiniteDuration = 1.second
  ): F[Either[APIError, OperationStatus]] = {
    for {
      statusResult <- api.getOperationStatus(accessToken, linkHref)

      result <- statusResult match {
        case Right(OperationStatus.InProgress) => {
          if (attempts > 0) {
            Temporal[F].sleep(interval) >> checkUploadStatus(accessToken, linkHref)(attempts - 1, interval)
          } else {
            Temporal[F].pure(statusResult)
          }
        }

        case Right(_) => Temporal[F].pure(statusResult)
        case err      => Temporal[F].pure(err)
      }
    } yield result
  }
}
