package com.danielpancake.cloudfrog.utils

import com.danielpancake.cloudfrog.utils.Utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UtilsSpec extends AnyFlatSpec with Matchers {
  "splitPathIntoSubpaths" should "split a path into a list of subpaths" in {
    Utils.splitPathIntoSubpaths("/a/b/c/d") should be(List("/a", "/a/b", "/a/b/c", "/a/b/c/d"))
  }

  it should "split a path into a list of subpaths even if it starts with a slash" in {
    Utils.splitPathIntoSubpaths("a/b/c/d") should be(List("/a", "/a/b", "/a/b/c", "/a/b/c/d"))
  }

  it should "ignore empty subpaths" in {
    Utils.splitPathIntoSubpaths("/a///b/c/d") should be(List("/a", "/a/b", "/a/b/c", "/a/b/c/d"))
  }

  it should "handle empty paths" in {
    Utils.splitPathIntoSubpaths("") should be(List())
  }

  "splitPathFilenameExt" should "split a path into a tuple of (path to the last folder, filename, extension)" in {
    Utils.splitPathFilenameExt("/a/b/c/d.txt") should be(("/a/b/c", "d.txt", "txt"))
  }

  it should "handle paths without extensions" in {
    Utils.splitPathFilenameExt("/a/b/c/d") should be(("/a/b/c", "d", ""))
  }

  it should "handle empty paths" in {
    Utils.splitPathFilenameExt("") should be(("", "", ""))
  }

  it should "handle root path without throwing" in {
    Utils.splitPathFilenameExt("/") should be(("/", "", ""))
  }
}
