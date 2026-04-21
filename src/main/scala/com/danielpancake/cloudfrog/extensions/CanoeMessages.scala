package com.danielpancake.cloudfrog.extensions

import canoe.models.messages.{AnimationMessage, AudioMessage, DocumentMessage, PhotoMessage, StickerMessage, VideoMessage, VideoNoteMessage, VoiceMessage}
import canoe.models.Chat
import canoe.syntax.Expect

final case class MediaMessage(
    messageId: Int,
    chat: Chat,
    date: Int,
    fileId: String,
    fileUniqueId: String,
    fileName: Option[String]
)

object CanoeMessages {
  val media: Expect[MediaMessage] = {
    case m: AnimationMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.animation.fileId, m.animation.fileUniqueId, m.animation.fileName)
    case m: AudioMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.audio.fileId, m.audio.fileUniqueId, m.audio.title)
    case m: DocumentMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.document.fileId, m.document.fileUniqueId, m.document.fileName)
    case m: PhotoMessage if m.photo.nonEmpty =>
      val photo = m.photo.last
      MediaMessage(m.messageId, m.chat, m.date, photo.fileId, photo.fileUniqueId, None)
    case m: VideoMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.video.fileId, m.video.fileUniqueId, None)
    case m: VideoNoteMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.videoNote.fileId, m.videoNote.fileUniqueId, None)
    case m: StickerMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.sticker.fileId, m.sticker.fileUniqueId, None)
    case m: VoiceMessage =>
      MediaMessage(m.messageId, m.chat, m.date, m.voice.fileId, m.voice.fileUniqueId, None)
  }
}
