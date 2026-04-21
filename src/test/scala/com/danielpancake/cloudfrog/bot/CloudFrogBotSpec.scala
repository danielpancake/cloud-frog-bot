package com.danielpancake.cloudfrog.bot

import canoe.api._
import canoe.methods.Method
import canoe.methods.messages.SendMessage
import canoe.models._
import canoe.models.messages.TextMessage

import cats.effect.{IO, Ref}
import cats.effect.unsafe.implicits.global

import com.danielpancake.cloudfrog.config._
import com.danielpancake.cloudfrog.services.api._
import com.danielpancake.cloudfrog.services.cache.TokenCache
import com.danielpancake.cloudfrog.services.storage.cloud.CloudStorage

import fs2.Stream

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.anyString
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class CloudFrogBotSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  private val testChatId: Long = 42L

  private val testConfig = Config(
    BotConfig("test_token"),
    RedisConfig("redis://localhost:6379"),
    YandexAPIConfig("client_id", "client_secret")
  )

  private val testChat: Chat =
    PrivateChat(id = testChatId, username = None, firstName = Some("Test"), lastName = None)

  private def makeTextMessage(text: String, msgId: Int = 1): TextMessage =
    TextMessage(messageId = msgId, chat = testChat, date = 0, text = text)

  private def makeUpdate(text: String): Update =
    MessageReceived(updateId = 1L, message = makeTextMessage(text))

  private def stubClient(sentTexts: Ref[IO, List[String]]): TelegramClient[IO] =
    new TelegramClient[IO] {
      private val stubMsg = makeTextMessage("stub", msgId = 99)
      def execute[Req, Res](request: Req)(implicit M: Method[Req, Res]): IO[Res] = {
        val io: IO[Any] = request match {
          case r: SendMessage => sentTexts.update(_ :+ r.text).as(stubMsg)
          case _              => IO.pure(Right(stubMsg): Either[Boolean, TextMessage])
        }
        io.map(_.asInstanceOf[Res])
      }
    }

  private def runScenario(updates: List[Update], scenario: Scenario[IO, Unit]): Unit =
    Stream.emits(updates)
      .covary[IO]
      .through(pipes.messages)
      .through(scenario.pipe)
      .compile
      .drain
      .unsafeRunSync()

  "userLogin" should "send welcome message on /start" in {
    val sentRef = Ref.unsafe[IO, List[String]](Nil)
    implicit val client: TelegramClient[IO] = stubClient(sentRef)
    implicit val logger: Logger[IO]         = Slf4jLogger.getLogger[IO]

    val bot = new CloudFrogBot(testConfig, client, mock[CloudStorage[IO]], mock[TokenCache[IO, String, String]])
    runScenario(List(makeUpdate("/start")), bot.userLogin[IO])

    sentRef.get.unsafeRunSync() should not be empty
  }

  it should "send welcome message on /login" in {
    val sentRef = Ref.unsafe[IO, List[String]](Nil)
    implicit val client: TelegramClient[IO] = stubClient(sentRef)
    implicit val logger: Logger[IO]         = Slf4jLogger.getLogger[IO]

    val bot = new CloudFrogBot(testConfig, client, mock[CloudStorage[IO]], mock[TokenCache[IO, String, String]])
    runScenario(List(makeUpdate("/login")), bot.userLogin[IO])

    sentRef.get.unsafeRunSync() should not be empty
  }

  "userLogout" should "delete token from cache and confirm on /logout" in {
    val sentRef = Ref.unsafe[IO, List[String]](Nil)
    implicit val client: TelegramClient[IO] = stubClient(sentRef)
    implicit val logger: Logger[IO]         = Slf4jLogger.getLogger[IO]

    val mockCache = mock[TokenCache[IO, String, String]]
    when(mockCache.del(anyString())).thenReturn(IO.pure(1L))

    val bot = new CloudFrogBot(testConfig, client, mock[CloudStorage[IO]], mockCache)

    implicit val cache: TokenCache[IO, String, String] = mockCache
    runScenario(List(makeUpdate("/logout")), bot.userLogout[IO])

    verify(mockCache).del(testChatId.toString)
    sentRef.get.unsafeRunSync() should not be empty
  }

  it should "delete token from cache and confirm on /reset" in {
    val sentRef = Ref.unsafe[IO, List[String]](Nil)
    implicit val client: TelegramClient[IO] = stubClient(sentRef)
    implicit val logger: Logger[IO]         = Slf4jLogger.getLogger[IO]

    val mockCache = mock[TokenCache[IO, String, String]]
    when(mockCache.del(anyString())).thenReturn(IO.pure(1L))

    val bot = new CloudFrogBot(testConfig, client, mock[CloudStorage[IO]], mockCache)

    implicit val cache: TokenCache[IO, String, String] = mockCache
    runScenario(List(makeUpdate("/reset")), bot.userLogout[IO])

    verify(mockCache).del(testChatId.toString)
  }

  "getStartCode" should "call authorize and store token on valid 16-symbols code + success" in {
    val sentRef = Ref.unsafe[IO, List[String]](Nil)
    implicit val client: TelegramClient[IO] = stubClient(sentRef)
    implicit val logger: Logger[IO]         = Slf4jLogger.getLogger[IO]

    val mockCache   = mock[TokenCache[IO, String, String]]
    val mockStorage = mock[CloudStorage[IO]]
    when(mockStorage.authorize(anyString())).thenReturn(IO.pure(APIResult.Success("access_token")))
    when(mockCache.set(anyString(), anyString())).thenReturn(IO.pure(()))

    val bot = new CloudFrogBot(testConfig, client, mockStorage, mockCache)

    implicit val cache: TokenCache[IO, String, String] = mockCache
    implicit val storage: CloudStorage[IO]             = mockStorage
    runScenario(List(makeUpdate("/start 2mezh4bgri2y7ixq")), bot.getStartCode[IO])

    verify(mockStorage).authorize("2mezh4bgri2y7ixq")
    verify(mockCache).set(testChatId.toString, "access_token")
  }

  it should "call authorize but not cache token on authorization failure" in {
    val sentRef = Ref.unsafe[IO, List[String]](Nil)
    implicit val client: TelegramClient[IO] = stubClient(sentRef)
    implicit val logger: Logger[IO]         = Slf4jLogger.getLogger[IO]

    val mockCache   = mock[TokenCache[IO, String, String]]
    val mockStorage = mock[CloudStorage[IO]]
    when(mockStorage.authorize(anyString()))
      .thenReturn(IO.pure(APIResult.Failure(APIError("invalid_client", "Bad code"))))

    val bot = new CloudFrogBot(testConfig, client, mockStorage, mockCache)

    implicit val cache: TokenCache[IO, String, String] = mockCache
    implicit val storage: CloudStorage[IO]             = mockStorage
    runScenario(List(makeUpdate("/start 1234567")), bot.getStartCode[IO])

    verify(mockStorage).authorize("1234567")
    verify(mockCache, never()).set(anyString(), anyString())
    sentRef.get.unsafeRunSync() should not be empty
  }

  it should "not invoke storage when message does not match the expected pattern" in {
    val sentRef = Ref.unsafe[IO, List[String]](Nil)
    implicit val client: TelegramClient[IO] = stubClient(sentRef)
    implicit val logger: Logger[IO]         = Slf4jLogger.getLogger[IO]

    val mockCache   = mock[TokenCache[IO, String, String]]
    val mockStorage = mock[CloudStorage[IO]]

    val bot = new CloudFrogBot(testConfig, client, mockStorage, mockCache)

    implicit val cache: TokenCache[IO, String, String] = mockCache
    implicit val storage: CloudStorage[IO]             = mockStorage
    runScenario(List(makeUpdate("/start not-a-code")), bot.getStartCode[IO])

    verifyNoInteractions(mockStorage)
  }
}
