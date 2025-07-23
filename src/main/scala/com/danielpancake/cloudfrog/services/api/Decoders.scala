package com.danielpancake.cloudfrog.services.api

import io.circe._
import io.circe.parser._

import sttp.client4.Response

/** JSON and API error decoders
  */
object Decoders {
  def decodeError[E: Decoder](error: String)(implicit converter: E => APIError): APIError =
    decode[E](error).fold(
      err => APIError(s"Decoding error of ${error.getClass.getName}: $error", err.getMessage),
      err => err
    )

  def decodeBody[T: Decoder](body: String): Either[APIError, T] =
    decode[T](body).fold(
      err => Left(APIError(s"Decoding error of ${body.getClass.getName}: $body", err.getMessage)),
      res => Right(res)
    )

  def handleResponse[E: Decoder, T: Decoder](
      response: Response[Either[String, String]]
  )(implicit converter: E => APIError): Either[APIError, T] =
    response.body.fold(err => Left(decodeError[E](err)), decodeBody[T])

  def handleErrorResponse[E: Decoder](
      response: Response[Either[String, String]]
  )(implicit converter: E => APIError): APIError =
    response.body
      .fold(
        err => APIError(s"Unexpected response with status code ${response.code}", err),
        res => decodeError[E](res)
      )
}
