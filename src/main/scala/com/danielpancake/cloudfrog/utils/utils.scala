package com.danielpancake.cloudfrog.utils

import org.apache.commons.io.FilenameUtils

object Utils {

  /** Splits a path into a list of subpaths. Example: /a/b/c/d -> List(/a, /a/b, /a/b/c, /a/b/c/d)
    *
    * @param path
    *   The path to split
    * @return
    *   A list of subpaths
    */
  def splitPathIntoSubpaths(path: String): List[String] =
    path
      .split("/")
      .filter(_.nonEmpty)
      .scanLeft("")((acc, folder) => acc + "/" + folder)
      .tail
      .toList

  /** Splits a path into a tuple of (path to the last folder, filename, extension). Example: /a/b/c/d.txt -> (/a/b/c,
    * d.txt, txt)
    *
    * @param fullPath
    *   The full path to split
    * @return
    *   A tuple of (path to the last folder, filename, extension)
    */
  def obfuscateMiddle(s: String, visible: Int): String =
    if (s.length <= visible * 2) s
    else s.take(visible) + "*" * (s.length - visible * 2) + s.takeRight(visible)

  def splitPathFilenameExt(fullPath: String): (String, String, String) = {
    val ext      = FilenameUtils.getExtension(fullPath)
    val path     = FilenameUtils.getFullPathNoEndSeparator(fullPath)
    val fileName = FilenameUtils.getName(fullPath)
    (path, fileName, ext)
  }
}
