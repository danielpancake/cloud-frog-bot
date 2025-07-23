package com.danielpancake.cloudfrog.services.storage.cloud

import com.danielpancake.cloudfrog.services.api.APIResult

/** A trait for a cloud storage service
  */
trait CloudStorage[F[_]] {
  def authorize(code: String): F[APIResult[String]]
  def testConnection(accessToken: String): F[APIResult[Nothing]]
  def uploadFile(accessToken: String, sourceURL: String, destinationPath: String): F[APIResult[Nothing]]
}
