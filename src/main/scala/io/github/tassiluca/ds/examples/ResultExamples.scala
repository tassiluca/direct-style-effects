package io.github.tassiluca.ds.examples

import io.github.tassiluca.ds.effects.resultant.{Error, Ok, Result}
import io.github.tassiluca.ds.effects.resultant

object ResultExamples extends App:

  type User = String
  type Address = String

  def getUser(id: String): Result[User] =
    Ok("Mario Rossi")

  def getAddress(user: User): Result[Address] =
    Error("The user doesn't exists")

  def together(): Result[String] =
    resultant:
      val user = getUser("101").?
      val address = getAddress(user).?
      user + address

  println(together())
