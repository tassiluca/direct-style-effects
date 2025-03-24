package io.github.tassiluca.ds.examples

import scala.annotation.experimental

@experimental
object SaferExceptions extends App:

  import language.experimental.saferExceptions

  class DivisionByZero extends Exception

  // Or equivalently:
  //    def div(n: Int, m: Int): Int throws DivisionByZero
  def div(n: Int, m: Int)(using CanThrow[DivisionByZero]): Int = m match
    case 0 => throw DivisionByZero()
    case _ => n / m

  class ValidationError(msg: String) extends Exception(msg)

  def requireAllPositive(xs: List[Int]): List[Int] throws ValidationError =
    xs.map(x => if x >= 0 then x else throw ValidationError(s"$x does not satisfy the positive requirement"))

  println:
    try
      // the compiler generates an accumulated capability as follows:
      // erased given CanThrow[DivisionByZero] = compiletime.erasedValue
      div(10, 0)
    catch case _: DivisionByZero => "Division by zero"

  // println:
  //   div(10, 1) // ERROR! Missing CanThrow[DivisionByZero] capability

  println:
    try
      requireAllPositive(1 :: 10 :: -10 :: Nil)
    catch case e: ValidationError => s"Validation error: ${e.getMessage}"

//  val values = (10, 1) :: (5, 2) :: (4, 2) :: (5, 1) :: Nil
//  println:
//    try values.map(div) // map is the regular List.map implementation!
//    catch case _: DivisionByZero => "Division by zero"
