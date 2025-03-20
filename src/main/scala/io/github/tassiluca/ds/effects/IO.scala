package io.github.tassiluca.ds.effects

// The capability to read from the console.
type CanRead = scala.io.StdIn.type

// The capability to print to the console.
type CanWrite = Console.type

class IO[T](val body: (CanRead, CanWrite) ?=> T):

  def run(): T = body(using scala.io.StdIn, Console)

object IO:

  def write(any: Any)(using c: CanWrite): Unit = c.println(any)

  def read(using s: CanRead): String = s.readLine
