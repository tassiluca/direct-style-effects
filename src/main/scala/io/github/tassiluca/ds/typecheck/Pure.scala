package io.github.tassiluca.ds.typecheck

import language.experimental.{pureFunctions, captureChecking, saferExceptions}

// x is an **impure** lambda, i.e. **can** capture any capability
def f(x: => Int): Int = x

// x is a **pure** lambda, i.e. **can't** capture any capability
def g(x: -> Int): Int = x

// an impure function capturing the CanThrow[Exception] capability <=> can throw any exception
def h(using c: CanThrow[Exception])(p: String ->{c} Boolean): Boolean =
 p("Hello World unpure function :)")

class Invalid(reason: String) extends Exception(reason)

def requireNonNegative(xs: List[Double]): List[Double] throws Invalid =
  xs.map(x => if x < 0 then throw Invalid("Negative number!") else x)

// return an impure function that can throw an Invalid exception
def squareRoot(using c: CanThrow[Invalid]): List[Double] ->{c} List[Double] =
    xs => requireNonNegative(xs).map(x => Math.sqrt(x))

@main def testPureTypes: Unit =
  try
    f(if false then throw Exception() else 1) // ok

    // g(if true then throw Exception() else 2) // ko!
    //               ^^^^^^^^^^^^^^^^^^^
    // reference CanThrow[Exception] is not included in the allowed capture set {}
    // of an enclosing function literal with expected type () ?-> Int
  catch case e: Exception => println(s"Error: $e")

  try
    h(_ => true) // ok
    h(s => if s.size > 1_000 then throw Exception() else true) // ok
  catch case e: Exception => println(s"Error: $e")

  try
    println:
      requireNonNegative(1.0 :: 2.9 :: -1.7 :: Nil) // ok
  catch case e: Exception => println(s"Error: $e")

  try
    println:
      squareRoot(1.0 :: 2.9 :: 1.7 :: Nil) // ok
  catch case e: Exception => println(s"Error: $e")
