package com.danielpancake.cloudfrog.extensions

import canoe.models.messages._
import canoe.models.Chat
import canoe.syntax.Expect

final case class MediaMessage(
    messageId: Int,
    chat: Chat,
    date: Int,
    fileId: String,
    fileUniqueId: String,
    fileName: Option[String]
) extends TelegramMessage

object CanoeMessages {
  val media: Expect[MediaMessage] = {
    case m: AudioMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.audio.fileId, m.audio.fileUniqueId, m.audio.title)
    case m: DocumentMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.document.fileId, m.document.fileUniqueId, m.document.fileName)
    case m: PhotoMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.photo.last.fileId, m.photo.last.fileUniqueId, None)
    case m: VideoMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.video.fileId, m.video.fileUniqueId, None)
    case m: VideoNoteMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.videoNote.fileId, m.videoNote.fileUniqueId, None)
    case m: VoiceMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.voice.fileId, m.voice.fileUniqueId, None)
  }
}
