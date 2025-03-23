package io.github.tassiluca.ds.typecheck

import language.experimental.captureChecking
import scala.caps.{Capability, cap, consume}

trait Writer extends Capability:
  def write(x: String): Unit

def write(x: String)(using w: Writer): Unit = w.write(x)

object Writer:
  def console[T](body: Writer ?=> T): () ->{body} T = () =>
    given Writer = println(_)
    body

@main def testWriterCapability: Unit =
  // ((w: Writer) ?=> () ->{w} Unit)
  val eff = (w: Writer) ?=> () => write("Hello, world")
  val writer = Writer.console: Writer ?=>
    write("Hello, world")
    write("Yay!")
    // Error: local reference Writer leaks into outer capture set {Writer}28978V of type
    // parameter T of method console in object Writer
    // eff
  writer()
