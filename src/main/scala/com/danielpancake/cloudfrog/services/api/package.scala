package com.danielpancake.cloudfrog.services.api

// Unified error response for all APIs to convert to
case class APIError(error: String, description: String)
    extends Exception(s"APIError($error, $description)") {
  override def toString: String = s"APIError($error, $description)"
}

// Unified result response pass to upper layers
sealed trait APIResult[+T]
object APIResult {
  case class Success[T](result: T) extends APIResult[T]

  case class Failure(err: APIError) extends APIResult[Nothing] {
    override def toString: String = s"Failure(${err.error}, ${err.description})"
  }

  case object Success extends APIResult[Nothing]
}
