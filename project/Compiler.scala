import sbt._

object Compiler {
  lazy val options = Seq(
    "-deprecation",
    "-Werror",
    "-Wdead-code",
    "-Wextra-implicit",
    "-Wnumeric-widen",
    "-Wunused",
    "-Wvalue-discard",
    "-Xlint",
    "-Xlint:-byname-implicit",
    "-Xlint:-implicit-recursion",
    "-unchecked"
  )
}
