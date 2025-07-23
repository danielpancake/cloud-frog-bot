package com.danielpancake.cloudfrog.utils

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
  def splitPathFilenameExt(fullPath: String): (String, String, String) = {
    val split    = fullPath.split("/").toList
    val path     = split.init.mkString("/")
    val fileName = split.last
    val ext      = fileName.split("\\.").last

    (
      path,
      fileName,
      if (ext == fileName) "" else ext
    )
  }
}
