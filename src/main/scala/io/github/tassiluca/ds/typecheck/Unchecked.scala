package io.github.tassiluca.ds.typecheck

import java.io.FileOutputStream
import scala.language.experimental.captureChecking

class FileSystem

class Logger(fs: FileSystem^):
  def log(s: String): Unit = 
    val fos = FileOutputStream("log.txt", true)
    fos.write(s.getBytes)
    fos.close()

  def test(fs: FileSystem^) =
    val l: Logger^{fs} = Logger(fs)
    l.log("hello world!")
    val xs: LazyList[Int]^{l} =
      LazyList.from(1)
        .map { i =>
          l.log(s"computing elem # $i")
          i * i
        }
    xs
