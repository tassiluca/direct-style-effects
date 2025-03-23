package io.github.tassiluca.ds.examples

import io.github.tassiluca.ds.effects.optional
import io.github.tassiluca.ds.effects.optional.?

import scala.util.boundary
import scala.util.boundary.{Label, break}

object BoundaryExamples extends App:

  // Non local returns are no longer supported; use `boundary` and `boundary.break` instead
  def deprecatedFirstIndex[T](xs: List[T], elem: T): Int =
    for (x, i) <- xs.zipWithIndex do if x == elem then return i
    -1

  def firstIndex[T](xs: List[T], elem: T): Int =
    boundary:
      for (x, i) <- xs.zipWithIndex do if x == elem then break(i)
      -1

  def functionalFirstIndex[T](xs: List[T], elem: T): Int =
    xs.zipWithIndex.find((x, _) => x == elem).map(_._2).getOrElse(-1)

  def firstColumn[T](xss: List[List[T]]): Option[List[T]] =
    optional:
      xss.map(_.headOption.?)

  // Non local returns are no longer supported; use `boundary` and `boundary.break` instead
  def deprecatedFindFirstMatchingPair[T](list1: List[T], list2: List[T])(predicate: (T, T) => Boolean): Option[(T, T)] =
    for i <- list1 do
      for j <- list2 do
        if predicate(i, j) then return Some((i, j))
    None

  def findFirstMatchingPair[T](list1: List[T], list2: List[T])(predicate: (T, T) => Boolean): Option[(T, T)] =
    boundary: (label: Label[Option[(T, T)]]) ?=>
      for i <- list1 do
        for j <- list2 do
          if predicate(i, j) then break(Some((i, j)))(using label)
      None

  def functionalFindFirstMatchingPair[T](list1: List[T], list2: List[T])(predicate: (T, T) => Boolean): Option[(T, T)] =
    list1.flatMap(i => list2.map(j => (i, j))).find(predicate(_, _))

  println:
    findFirstMatchingPair(10 :: 20 :: 30 :: Nil, 100 :: 90 :: 30 :: Nil)(_ == _)
