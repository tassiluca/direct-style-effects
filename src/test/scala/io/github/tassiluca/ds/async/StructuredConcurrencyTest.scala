package io.github.tassiluca.ds.async

import gears.async.AsyncOperations.sleep
import gears.async.default.given
import gears.async.{Async, Future}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.language.postfixOps

class StructuredConcurrencyTest extends AnyFunSpec with Matchers:

  describe("Structured concurrency"):
    it("ensure all nested computations are contained within the lifetime of the enclosing one"):
      Async.blocking:
        val before = System.currentTimeMillis()
        val result = Async.group:
          val f1 = Future { "hello" }
          val f2 = Future { sleep(2_000); "gears!" }
          f1.await + " " + f2.await
        result shouldBe "hello gears!"
        val now = System.currentTimeMillis()
        now - before should be > 2_000L

    describe("in case of failures"):
      it("if the first nested computation we wait fails with an exception the other is cancelled"):
        Async.blocking:
          var stillAlive = false
          val before = System.currentTimeMillis()
          val f = Future:
            val f1 = Future { throw Error(); "hello" }
            val f2 = Future { sleep(2_000); stillAlive = true }
            f1.await + " " + f2.await // fortunate case in which the one which fails is the one we wait for
          f.awaitResult.isFailure shouldBe true
          val now = System.currentTimeMillis()
          now - before should be < 2_000L
          sleep(3_000)
          stillAlive shouldBe false

      it("if a nested computation fails while we are waiting for another, the enclosing future is not cancelled"):
        Async.blocking:
          var stillAlive = false
          val before = System.currentTimeMillis()
          val f = Future:
            val f1 = Future { throw Error(); "gears!" }
            val f2 = Future { sleep(2_000); stillAlive = true; "hello" }
            f2.await + " " + f1.await // note the inverted order w.r.t. the previous case
          f.awaitResult.isFailure shouldBe true
          val now = System.currentTimeMillis()
          now - before should be >= 2_000L
          stillAlive shouldBe true

      it("but we can achieve cancellation using zip combinator"):
        Async.blocking:
          var stillAlive = false
          val before = System.currentTimeMillis()
          val result = Async.group:
            val f1 = Future { throw Error(); "gears!" }
            val f2 = Future { sleep(2_000); stillAlive = true; "hello" }
            f2.zip(f1).awaitResult
          result.isFailure shouldBe true
          val now = System.currentTimeMillis()
          now - before should be < 2_000L
          sleep(3_000)
          stillAlive shouldBe false

    it("allows racing futures cancelling the slower one when one succeeds"):
      Async.blocking:
        var stillAlive = false
        val before = System.currentTimeMillis()
        val f1 = Future { sleep(1_000); "faster won" }
        val f2 = Future { sleep(2_000); stillAlive = true }
        val result = f1.orWithCancel(f2).awaitResult
        val now = System.currentTimeMillis()
        now - before should (be >= 1_000L and be < 2_000L)
        result.isSuccess shouldBe true
        result.get shouldBe "faster won"
        sleep(3_000)
        stillAlive shouldBe false
