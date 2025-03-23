package io.github.tassiluca.ds.typecheck

import io.github.tassiluca.ds.effects.EitherConversions.given
import io.github.tassiluca.ds.effects.either.*
import io.github.tassiluca.ds.effects.{CanFail, either}
import io.github.tassiluca.ds.utils.Runnable

import java.io.PrintWriter
import java.nio.file.Path
import java.util.concurrent.Callable
import scala.annotation.experimental
import scala.caps.Capability
import scala.io.Source
import scala.language.experimental.captureChecking
import scala.util.Using

trait IO extends Capability:
  def write(content: String)(using CanFail): Unit
  def read[T](f: Iterator[String] => T)(using CanFail): T

object IO:
  /** Effect. */
  def write(x: String)(using IO, CanFail): Unit = summon[IO].write(x)

  /** Effect. */
  def read[T](f: Iterator[String] => T = _.mkString)(using IO, CanFail): T = summon[IO].read(f)

  /** Capability generator. */
  def console[R](body: IO ?=> R): Runnable[R]^{body} = () =>
    given IO with // actual effect handler
      def write(content: String)(using CanFail): Unit = Console.println(content)
      def read[T](f: Iterator[String] => T)(using CanFail): T = f(scala.io.StdIn.readLine.linesIterator)
    body

  /** Capability generator. */
  def file[R](path: Path)(body: IO ?=> R): Runnable[R]^{body} = () =>
    given IO with // actual effect handler
      def write(content: String)(using CanFail): Unit =
        Using(PrintWriter(path.toFile)): writer =>
          writer.write(content)
        .?
      def read[T](f: Iterator[String] => T)(using CanFail): T =
        Using(Source.fromFile(path.toFile)): source =>
          f(source.getLines())
        .?
    body

import IO.*

@main def testIO: Unit =
  // ((w: Writer) ?=> () ->{w} Unit)
  //  val eff = (w: Writer) ?=> () => write("Hello, world")
  val ioEff = IO.console:
    either:
      write("Welcome to the parrrot game!")
      for _ <- 0 until 10 do
        write("You say something:")
        val r = read(_.mkString)
        write("I repeat it: " + r)
  ioEff.run()

//@main def breakingIO = either:
//  val path = Path.of("test.txt")
//  val eff = IO.file(path): IO ?=>
//    () => read(_.mkString)
//  val content = eff.run()
//  println(content)
