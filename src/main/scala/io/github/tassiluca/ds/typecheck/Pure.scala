package io.github.tassiluca.ds.typecheck

import language.experimental.{pureFunctions, captureChecking, saferExceptions}
import unsafeExceptions.canThrowAny

// x is an **impure** lambda, i.e. **can** capture any capability
def f(x: => Int): Int = x

// x is a **pure** lambda, i.e. **can't** capture any capability
def g(x: -> Int): Int = x

def h(using c: CanThrow[Exception])(x: String ->{c} Boolean): Unit =
 if x("Hello World pure functions :)") then println("Yes") else println("No")

class Invalid extends Exception

def nonNegative(x: Int)(using CanThrow[Invalid]): Int =
  if x < 0 then throw Invalid() else x

def square(using c: CanThrow[Invalid]): Int ->{c} Int = x => nonNegative(x) * x

@main def testPureTypes: Unit =
  try
    f(if false then throw Exception() else 1) // ok
    // reference (canThrow$1 : CanThrow[Exception]) is not included in the allowed capture set {}
    // of an enclosing function literal with expected type () ?-> Int
    // g(if true then throw Exception() else 2) // NOOO!!
    h(_ => true) // ok
    h(s => if s.size > 1_000 then throw Exception() else true) // ok
    nonNegative(1) // ok
    square(-1)
  catch case e: Exception => println(s"Ough: $e")
