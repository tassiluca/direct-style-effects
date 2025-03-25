package io.github.tassiluca.ds.effects

import io.github.tassiluca.ds.effects.EitherConversions.given
import io.github.tassiluca.ds.effects.either.*
import io.github.tassiluca.ds.utils.Runnable

import java.io.PrintWriter
import java.nio.file.Path
import scala.annotation.experimental
import scala.io.Source
import scala.util.{Try, Using}

/** **Unsafe IO capability**. See [[io.github.tassiluca.ds.typecheck.IO]] for a type-safe version. */
trait IO:
  /** Write to an IO stream. */
  def write(content: String)(using CanFail): Unit
  /** Read from an IO stream. */
  def read[T](f: Iterator[String] => T)(using CanFail): T

object IO:
  def write(x: String)(using IO, CanFail): Unit = summon[IO].write(x)

  def read[T](f: Iterator[String] => T)(using IO, CanFail): T = summon[IO].read(f)

  /** Capability generator for console-based IO. */
  def console[R](body: IO ?=> R): Runnable[R] = () =>
    given IO with // actual effect handler
      def write(content: String)(using CanFail): Unit = Console.println(content)
      def read[T](f: Iterator[String] => T)(using CanFail): T = Try(f(scala.io.StdIn.readLine.linesIterator)).?
    body

  /** Capability generator for file-based IO. */
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

@main def consoleIO(): Unit = either:
  val ioEff = IO.console:
    write("Welcome to the parrrot game!")
    write("You say something:")
    val r = read(_.mkString)
    write("I repeat it: " + r)
  ioEff.run()

@main def safeFileIO = either:
  val path = Path.of("test.txt")
  val eff: Runnable[String] = IO.file(path):
    write("Hello, World from IO capability!")
    read(_.mkString)
  val content = eff.run() // the effect is run!
  println(s"The content is: ${content}")

@main def breakingIO = either:
  val path = Path.of("test.txt")
  // The lines are returned as an iterator, no lines are read yet...
  val eff: Runnable[Iterator[String]] = IO.file(path):
    write("Let's break the IO capability!")
    read(lines => lines)
  // ...when we try to execute the effect, the lines are read but the stream is already closed!
  val content: Iterator[String] = eff.run()
  content.next() // throws "IOException: Stream Already Closed"!!!

@main def breakingIO2 = either:
  val path = Path.of("test.txt")
  val laterEff: Runnable[() => String] = IO.file(path):
    write("Let's break the IO capability / 2!")
    read(l => () => l.mkString)
  val result = laterEff.run()
  println(result())
