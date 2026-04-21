package com.danielpancake.cloudfrog.bot

import com.danielpancake.cloudfrog.bot.BotErrors._
import com.danielpancake.cloudfrog.config.Config
import com.danielpancake.cloudfrog.extensions.MediaMessage
import com.danielpancake.cloudfrog.extensions.CanoeMessages._
import com.danielpancake.cloudfrog.messages.Messages
import com.danielpancake.cloudfrog.services.api._
import com.danielpancake.cloudfrog.services.cache.TokenCache
import com.danielpancake.cloudfrog.services.storage.cloud.CloudStorage
import com.danielpancake.cloudfrog.utils.Utils

import canoe.api._
import canoe.methods.{files, messages}
import canoe.models.{Chat, File}
import canoe.models.messages.TextMessage
import canoe.syntax._

import cats.effect.{IO, IOApp, Sync}
import cats.implicits._

import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

class CloudFrogBot(
    config: Config,
    telegramClient: TelegramClient[IO],
    cloudStorage: CloudStorage[IO],
    tokenCache: TokenCache[IO, String, String]
) extends IOApp.Simple {

  def run: IO[Unit] = {
    implicit val client: TelegramClient[IO]            = telegramClient
    implicit val storage: CloudStorage[IO]             = cloudStorage
    implicit val cache: TokenCache[IO, String, String] = tokenCache
    implicit def logger[F[_]: Sync]: Logger[F]         = Slf4jLogger.getLogger[F]

    Bot
      .polling[IO]
      .follow(
        userLogin,
        userLogout,
        getStartCode,
        showStorageURL,
        handleMedia
      )
      .compile
      .drain
  }

  /** User login scenario: sends welcome message and login button
    */
  def userLogin[F[_]: Logger: TelegramClient]: Scenario[F, Unit] = {
    implicit val configImplicit: Config = config
    for {
      msg <- Scenario.expect(textMessage.matching("/start|/login"))
      _   <- Scenario.eval(Logger[F].info(s"User in chat ${msg.chat.id} started login process"))
      _   <- Scenario.eval(msg.chat.send(Messages.welcomeMessage, keyboard = Messages.loginButton))
    } yield ()
  }

  /** User logout scenario: removes token from cache and sends logout message
    */
  def userLogout[F[_]: Logger: TelegramClient](implicit cache: TokenCache[F, String, String]): Scenario[F, Unit] = {
    for {
      msg <- Scenario.expect(textMessage.matching("/reset|/logout"))
      _   <- Scenario.eval(cache.del(msg.chat.id.toString))
      _   <- Scenario.eval(Logger[F].info(s"User in chat ${msg.chat.id} logged out"))
      _   <- Scenario.eval(msg.chat.send(Messages.logoutSuccess))
    } yield ()
  }

  /** Show storage URL scenario: sends link to Yandex.Disk
    */
  def showStorageURL[F[_]: TelegramClient]: Scenario[F, Unit] = {
    for {
      msg <- Scenario.expect(textMessage.matching("/disk"))
      _   <- Scenario.eval(msg.chat.send(Messages.yandexDiskLink.markdown))
    } yield ()
  }

  /** Get start code scenario: checks if code is valid and starts oauth process
    */
  def getStartCode[F[_]: Logger: Sync: TelegramClient](implicit
      cache: TokenCache[F, String, String],
      storage: CloudStorage[F]
  ): Scenario[F, Unit] = {
    val codeFormat = """[a-zA-Z0-9]{16}""".r
    for {
      msg     <- Scenario.expect(textMessage.matching("^/start .+$"))
      _       <- Scenario.eval(Logger[F].info(s"User in chat ${msg.chat.id} started oauth process"))
      rawCode  = msg.text.stripPrefix("/start ").trim

      _ <- if (codeFormat.matches(rawCode))
        oauthProcess(msg.chat, rawCode)
      else
        Scenario.eval(msg.chat.send(s"${Messages.invalidCode}\nCode: ${Utils.obfuscateMiddle(rawCode, 2)}"))
    } yield ()
  }

  /** OAuth process scenario: exchanges code for token and sends result message
    */
  private def oauthProcess[F[_]: Logger: Sync: TelegramClient](
      chat: Chat,
      code: String
  )(implicit redis: TokenCache[F, String, String], storage: CloudStorage[F]): Scenario[F, Unit] = {

    def updateMessage(msg: TextMessage, text: String): Scenario[F, Unit] =
      for {
        _ <- Scenario
          .eval(messages.EditMessageText.direct(msg.chat.id, msg.messageId, text).call)
          .attempt
          .flatMap {
            case Left(err) =>
              for {
                _ <- Scenario.eval(Logger[F].error(s"Error while editing message: $err"))
                _ <- Scenario.eval(msg.chat.send(text))
              } yield ()
            case Right(_) => Scenario.done[F]
          }
      } yield ()

    def oathSuccess(access_token: String) =
      Logger[F].info(s"Successfully exchanged code for token for chat ${chat.id}") *>
        redis.set(chat.id.toString, access_token) *>
        chat.send(Messages.uploadInstructions)

    def oathFailure(err: APIError) =
      Logger[F].error(s"Error while exchanging code for token: $err") *>
        chat.send(s"${Messages.loginFailure} (${err.description})")

    def oathNoToken = chat.send(Messages.oauthFailed)

    for {
      msg <- Scenario.eval(chat.send(Messages.loginProcessing))
      _ <- Scenario
        .eval(storage.authorize(code))
        .flatMap {
          case APIResult.Success(token) =>
            updateMessage(msg, Messages.loginSuccess) *>
              Scenario.eval(oathSuccess(token))

          case APIResult.Failure(err) => Scenario.eval(oathFailure(err))
          case APIResult.Success      => Scenario.eval(oathNoToken)
        }
    } yield ()
  }

  /** Require login scenario: checks if user is logged in and token is valid
    */
  def requireLogin[F[_]: Sync](
      chat: Chat
  )(implicit cache: TokenCache[F, String, String], storage: CloudStorage[F]): Scenario[F, String] = {
    for {
      accessToken <- Scenario.eval(cache.get(chat.id.toString)).flatMap {
        case Some(value) => Scenario.pure(value)
        case None        => Scenario.eval(Sync[F].raiseError(BotError(Text(chat, Messages.loginRequired))))
      }
      _ <- Scenario.eval(storage.testConnection(accessToken)).flatMap {
        case APIResult.Failure(_) => Scenario.eval(Sync[F].raiseError(BotError(Text(chat, Messages.oauthError))))
        case _                    => Scenario.done[F]
      }
    } yield accessToken
  }

  /** Handle media scenario: downloads file and uploads it to Yandex.Disk
    */
  def handleMedia[F[_]: Logger: Sync: TelegramClient](implicit
      cache: TokenCache[F, String, String],
      storage: CloudStorage[F]
  ): Scenario[F, Unit] = {

    /** Notify user that file is being processed
      */
    def notifyProcessing(msg: MediaMessage): Scenario[F, TextMessage] =
      Scenario
        .eval(
          Logger[F].info(s"User in chat ${msg.chat.id} sent a file") *>
            msg.chat.send(Messages.uploadPreparation, replyToMessageId = Some(msg.messageId))
        )

    /** Get file from Telegram API
      */
    def getFile(fileId: String): Scenario[F, File] =
      Scenario
        .eval(files.GetFile(fileId).call)
        .attempt
        .flatMap {
          case Left(e)     => Scenario.eval(Sync[F].raiseError(new Exception(s"Error while getting file: $e")))
          case Right(file) => Scenario.pure(file)
        }

    /** Get full path to file on Yandex.Disk. If file has a name, use it, otherwise use fileUniqueId
      */
    def getFileFullPath(media: MediaMessage, filePath: String): F[String] =
      Sync[F].delay {
        val (path, _, ext) = Utils.splitPathFilenameExt(filePath)
        val fileName       = media.fileName.getOrElse(s"${media.fileUniqueId}.$ext")
        s"$path/$fileName"
      }

    /** Upload file to Yandex.Disk
      */
    def uploadFile(accessToken: String, sourceURL: String, fullPath: String): Scenario[F, APIResult[Nothing]] =
      for {
        uploadF <- Scenario.eval(Sync[F].blocking(storage.uploadFile(accessToken, sourceURL, fullPath)))
        result  <- Scenario.eval(uploadF)
      } yield result

    /** Notify user that file was uploaded
      */
    def notifyUploadResult(msg: TextMessage): Scenario[F, Unit] = {
      Scenario
        .eval(
          Logger[F].info(s"User in chat ${msg.chat.id} uploaded a file") *>
            messages.EditMessageText.direct(msg.chat.id, msg.messageId, Messages.uploadSuccess).call
        )
        .attempt
        .flatMap {
          case Left(err) => Scenario.eval(Logger[F].error(s"Error while editing message: $err"))
          case _         => Scenario.done[F]
        }
    }

    // Main scenario for handling media
    (for {
      media <- Scenario.expect(media)
      token <- requireLogin(media.chat)
      msg   <- notifyProcessing(media)

      file <- getFile(media.fileId).handleErrorWith { case err =>
        Scenario.eval(
          Sync[F].raiseError(BotError(Edit(msg, Messages.badFile), Some(s"Error while getting file: $err")))
        )
      }

      filePath <- file.filePath match {
        case Some(value) => Scenario.pure[F](value)
        case None =>
          Scenario.eval(
            Sync[F].raiseError(BotError(Edit(msg, Messages.internalError), Some("Error while getting file path")))
          )
      }

      sourceURL = s"https://api.telegram.org/file/bot${config.bot.token}/$filePath"
      fullPath  <- Scenario.eval(getFileFullPath(media, filePath))

      _ <- uploadFile(token, sourceURL, fullPath).handleErrorWith { case err =>
        Scenario.eval(
          Sync[F].raiseError(BotError(Edit(msg, Messages.internalError), Some(s"Error while uploading file: $err")))
        )
      }

      _ <- notifyUploadResult(msg)

    } yield ()).handleErrorWith {
      case err: BotError => handleBotError(err)
      case ukn           => Scenario.eval(Logger[F].error(s"Unknown error. Failed to handle. $ukn"))
    }
  }
}
