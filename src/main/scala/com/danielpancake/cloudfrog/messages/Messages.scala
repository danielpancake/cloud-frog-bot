package com.danielpancake.cloudfrog.messages

import com.danielpancake.cloudfrog.config.Config

import canoe.api.models.Keyboard
import canoe.models.{InlineKeyboardButton, InlineKeyboardMarkup}

object Messages {
  val welcomeMessage = """
    |Hi there! Welcome to Cloud Upload Bot 👋🐸
    |
    |I'm here to help automatically upload your photos, videos, voice messages or other media you send to me to your Yandex.Disk storage.
    |
    |To get started, please log in with Yandex by clicking the button below 👇
    |""".stripMargin

  val loginRequired   = "You are not logged in with Yandex. Please /login first."
  val loginWithYandex = "Login with Yandex"
  val loginProcessing = "Processing your login request..."
  val loginFailure    = "Login failed. 😢 Please try again later."
  val loginSuccess    = "You have successfully logged in with Yandex! 🎉"

  val invalidCode = "Invalid code. Please try again later."
  val oauthFailed = "OAuth failed. Please try again later."
  val oauthError  = "Authorization error. Please try /login again."

  val uploadInstructions =
    "Let's begin! Please send me any media you want to upload to Yandex.Disk. Send /disk to get a link to your Yandex.Disk storage."

  val logoutSuccess = "You have successfully logged out! 👋"

  val uploadPreparation = "Preparing for uploading..."
  val uploadSuccess     = "Uploaded successfully! 🎉"

  val internalError = "Internal error. Please try again later."
  val badFile       = "Something went wrong or the file exceeds 20MB. Please try again later."

  val yandexDiskLink = "See: [Yandex\\.Disk](https://disk.yandex.com/client/disk)"

  def loginButton(implicit config: Config): Keyboard.Inline = {
    val oauthUrl = s"https://oauth.yandex.ru/authorize?response_type=code&client_id=${config.yandexAPI.clientId}"

    Keyboard.Inline(
      InlineKeyboardMarkup.singleButton(
        InlineKeyboardButton.url(loginWithYandex, oauthUrl)
      )
    )
  }
}
