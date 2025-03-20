package io.github.tassiluca.ds.typecheck

import language.experimental.captureChecking
import scala.caps.Capability

class FileSystem extends Capability

class Logger(using f: FileSystem):
  def log(msg: String): Unit = ???

@main def testLogger =
  given fs: FileSystem = ???

  val l: Logger^{fs} = Logger()
  l.log("Hello, world")

  val xs: LazyList[Int]^{l} = LazyList.from(1).map: i =>
    l.log(s"Processing $i")
    i * i

