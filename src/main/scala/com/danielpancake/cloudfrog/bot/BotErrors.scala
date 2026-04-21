package com.danielpancake.cloudfrog.bot

import cats.implicits._

import canoe.api._
import canoe.methods.messages.EditMessageText
import canoe.models.Chat
import canoe.syntax._

import org.typelevel.log4cats.Logger
import canoe.models.messages.TextMessage

object BotErrors {

  /** A wrapper for errors that occur while interacting with the bot. Meant to provide to send error messages to the
    * user
    *
    * @param method
    *   The method of reporting the error
    * @param description
    *   An optional description of the error (for logging)
    */
  case class BotError(method: HandlingMethod, description: Option[String] = None)
      extends Exception(description.getOrElse(s"BotError($method)"))

  /** The method of reporting the error: sending a new message, editing an existing one, replying to a message, or only
    * logging the error
    */
  sealed trait HandlingMethod
  case class Text(chat: Chat, message: String)             extends HandlingMethod
  case class Reply(msgReply: TextMessage, message: String) extends HandlingMethod
  case class Edit(msgEdit: TextMessage, message: String)   extends HandlingMethod
  case object OnlyLog                                      extends HandlingMethod

  /** Handle a bot error by sending a message to the user, editing an existing message, replying to a message, or only
    * logging the error
    *
    * @param err
    *   The error to handle
    * @return
    *   A scenario that handles the error
    */
  def handleBotError[F[_]: Logger: TelegramClient](err: BotError): Scenario[F, Unit] = {
    for {
      _ <- err.description.fold(Scenario.done[F])(desc => Scenario.eval(Logger[F].error(desc)))
      _ <- err.method match {
        case Text(chat, msg) => Scenario.eval(chat.send(msg)).void
        case OnlyLog         => Scenario.done[F]

        case Reply(msgReply, msg) =>
          Scenario.eval(msgReply.chat.send(msg, replyToMessageId = Some(msgReply.messageId))).void

        case Edit(msgEdit, msg) =>
          for {
            _ <- Scenario.eval(EditMessageText.direct(msgEdit.chat.id, msgEdit.messageId, msg).call).attempt.flatMap {
              // Fallback to sending a new message if editing the message fails
              case Left(err) =>
                for {
                  _ <- Scenario.eval(Logger[F].error(s"Error while editing message: $err"))
                  _ <- Scenario.eval(msgEdit.chat.send(msg))
                } yield ()
              case Right(_) => Scenario.done[F]
            }
          } yield ()
      }
    } yield ()
  }
}
