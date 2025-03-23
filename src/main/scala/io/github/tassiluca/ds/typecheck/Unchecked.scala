package io.github.tassiluca.ds.typecheck

import java.io.FileOutputStream
import scala.language.experimental.captureChecking

//def usingLogFile[T](op: FileOutputStream^ => T): T =
//  val logFile = FileOutputStream("log")
//  val result = op(logFile)
//  logFile.close()
//  result
//
//@main def test(): Unit =
//  val later = usingLogFile { (file: FileOutputStream^) => () => file.write(0) }
//  later() // crash
