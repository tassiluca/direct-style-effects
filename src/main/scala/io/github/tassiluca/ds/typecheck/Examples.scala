package io.github.tassiluca.ds.typecheck

import java.io.FileOutputStream
import language.experimental.captureChecking
// import language.experimental.pureFunctions

def usingLogFile[T](op: FileOutputStream^ => T): T =
  val logFile = FileOutputStream("log")
  val result = op(logFile)
  logFile.close()
  result

@main def okUse =
  usingLogFile: f =>
    f.write("Hello, world".getBytes)

@main def stillOkUse =
  val result = usingLogFile: f =>
    List(1, 2, 3, 4).map: x =>
      f.write(x)
      x * x
  println(result)

@main def koUse =
  // usingLogFile: f =>
  //  () => f.write("I'm deffered! I cannot work!".getBytes)
  // this as well should not compile!
  val res: LazyList[Int] = usingLogFile: f =>
    LazyList(1, 2, 3).map: x =>
      f.write(x)
      x * x
  res.take(100).foreach(println)

@main def testF: Unit =
  val pure: () -> Int = () => 20
  println(pure())
