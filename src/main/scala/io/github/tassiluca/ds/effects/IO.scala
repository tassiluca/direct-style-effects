package io.github.tassiluca.ds.effects

import io.github.tassiluca.ds.effects.EitherConversions.given
import io.github.tassiluca.ds.effects.either.*
import io.github.tassiluca.ds.utils.Runnable

import java.io.PrintWriter
import java.nio.file.Path
import scala.annotation.experimental
import scala.io.Source
import scala.util.Using

trait IO:
  def write(content: String)(using CanFail): Unit
  def read[T](f: Iterator[String] => T)(using CanFail): T

object IO:
  /** Effect. */
  def write(x: String)(using IO, CanFail): Unit = summon[IO].write(x)

  /** Effect. */
  def read[T](f: Iterator[String] => T)(using IO, CanFail): T = summon[IO].read(f)

  /** Capability generator. */
  def console[R](body: IO ?=> R): Runnable[R] = () =>
    given IO with // actual effect handler
      def write(content: String)(using CanFail): Unit = Console.println(content)
      def read[T](f: Iterator[String] => T)(using CanFail): T = f(scala.io.StdIn.readLine.linesIterator)
    body

  /** Capability generator. */
  def file[R](path: Path)(body: IO ?=> R): Runnable[R] = () =>
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

@main def testIO(): Unit =
  val ioEff = IO.console:
    either:
      write("Welcome to the parrrot game!")
      write("You say something:")
      val r = read(_.mkString)
      write("I repeat it: " + r)
  ioEff.run()

@main def breakingIO = either:
  val path = Path.of("test.txt")
  // The lines are returned as an iterator, no lines are read yet...
  val eff: Runnable[Iterator[String]] = IO.file(path):
    read(identity)
  // ...when we try to execute the effect, the lines are read but the stream is already closed!
  val content = eff.run().next() // throws "IOException: Stream Already Closed"!!!
  println(content)
