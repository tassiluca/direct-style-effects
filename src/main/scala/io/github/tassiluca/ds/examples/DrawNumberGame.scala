package io.github.tassiluca.ds.examples

@main def testExfiltratingIO: Unit =
  import io.github.tassiluca.ds.effects.{IO, CanWrite, CanRead}
  import IO.*
  val future = IO:
    () => write("Hello, world")
  future.run()()

object DrawNumberGame extends App:
  import io.github.tassiluca.ds.effects.{IO, CanWrite, CanRead}
  import IO.*
  import scala.util.boundary
  import boundary.break

  enum Result:
    case Won, High, Low

  private def logic(draw: Int)(using CanWrite, CanRead): Result =
    write("Gimmee your number: ")
    val number = read.toInt
    if number > draw then Result.High else if number < draw then Result.Low else Result.Won

  val attempts = 10
  val draw = 10
  println:
    boundary:
      IO:
        (0 until attempts).foreach: _ =>
          val result = logic(draw)
          if result == Result.Won then break(result) else write(result)
      .run()
