package io.github.tassiluca.ds.examples

import io.github.tassiluca.ds.effects.{CanFail, either}
import io.github.tassiluca.ds.effects.IO

object DrawNumberGame extends App:
  import IO.*
  import scala.util.boundary
  import boundary.break

  enum Result:
    case Won, High, Low, Lost

  private def logic(draw: Int)(using IO, CanFail): Result =
    write("Gimmee your number: ")
    val number = read(_.mkString.toInt)
    if number > draw then Result.High else if number < draw then Result.Low else Result.Won

  def play(attempts: Int = 10, draw: Int)(using IO, CanFail): Result =
    boundary:
      for _ <- 0 until attempts do
        val result = logic(draw)
        if result == Result.Won then break(Result.Won) else write(result.toString)
      Result.Lost

@main def playGame =
  import scala.util.Random
  val res = either:
    val game = IO.console(DrawNumberGame.play(draw = Random.nextInt(100)))
    game.run()
  println(res)
