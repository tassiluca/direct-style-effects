package io.github.tassiluca.ds.typecheck

import language.experimental.captureChecking
import scala.caps.{Capability, cap}

trait Runnable[T]:
  def run(): T

trait Writer extends Capability:
  def write(x: String): Unit

def write(x: String)(using w: Writer^{cap}): Unit = w.write(x)

object Writer:

  def console[T](body: (Writer^{cap}) ?=> T): Runnable[T]^{body} = () =>
    given Writer = println(_)
    body

@main def testWriterCapability: Unit =
  // local reference contextual$1 from (using contextual$1: io.github.tassiluca.ds.typecheck.Writer^):
  // box () ->{contextual$1} Unit leaks into outer capture set of type parameter T of method console in object Writer
  val r = Writer.console:
    write("Hello, world")
    () => write("Hello, world")
  r.run()
