package io.github.tassiluca.ds.effects

import scala.util.Random

type Sample[A] = Random ?=> A

def intSample(using r: Random): Int = r.nextInt()

def doubleSample(using r: Random): Double = r.nextDouble()

object Sample:

  def run[A](sample: Sample[A])(using random: Random = scala.util.Random): A =
    given Random = random
    sample

  inline def apply[A](inline body: Random ?=> A): Sample[A] = body

@main def testSample(): Unit =
  val sample: Sample[Int] = Sample:
    val i = intSample
    i
  println(Sample.run(sample))
