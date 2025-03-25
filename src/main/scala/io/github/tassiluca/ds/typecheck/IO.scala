package io.github.tassiluca.ds.typecheck

import io.github.tassiluca.ds.effects.EitherConversions.given
import io.github.tassiluca.ds.effects.either.*
import io.github.tassiluca.ds.effects.{CanFail, either}
import io.github.tassiluca.ds.utils.Runnable

import java.io.PrintWriter
import java.nio.file.Path
import scala.annotation.experimental
import scala.caps.Capability
import scala.io.Source
import scala.language.experimental.captureChecking
import scala.util.{Try, Using}

/** **Safe IO capability**. */
trait IO extends Capability:
  /** Write to an IO stream. */
  def write(content: String)(using CanFail): Unit
  /** Read from an IO stream. */
  def read[T](f: Iterator[String]^ => T)(using CanFail): T

object IO:
  def write(x: String)(using IO, CanFail): Unit = summon[IO].write(x)

  def read[T](f: Iterator[String]^ => T = _.mkString)(using IO, CanFail): T = summon[IO].read(f)

  /** Capability generator for console-based IO. */
  def console[R](body: IO ?=> R): Runnable[R]^{body} = () =>
    given IO with // actual effect handler
      def write(content: String)(using CanFail): Unit = Console.println(content)
      def read[T](f: Iterator[String]^ => T)(using CanFail): T = Try(f(scala.io.StdIn.readLine.linesIterator)).?
    body

  /** Capability generator for file-based IO. */
  def file[R](path: Path)(body: IO ?=> R): Runnable[R]^{body} = () =>
    given IO with // actual effect handler
      def write(content: String)(using CanFail): Unit =
        Using(PrintWriter(path.toFile)): writer =>
          writer.write(content)
        .?
      def read[T](f: Iterator[String]^ => T)(using CanFail): T =
        Using(Source.fromFile(path.toFile)): source =>
          f(source.getLines())
        .?
    body

import IO.*

@main def catchedIOLeak = either:
  val path = Path.of("test.txt")
  val laterEff = IO.file(path):
    write("Hello, World from IO capability!") // ok!
    read(_.mkString) // ok!
    // read(l => l) //, or equivalently: read(identity)
    // ^^^^^^^^^^^^
    // local reference l leaks into outer capture set of type parameter T of method read in object IO

    // () => write("Hello, World from IO capability!")
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // local reference IO leaks into outer capture set of type parameter R of method file in object IO
  val result = laterEff.run()
  println(result)
