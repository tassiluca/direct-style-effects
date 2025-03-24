package io.github.tassiluca.ds.examples

// a comparison between monadic and direct style...
object ShowcasingDirectStyle:

  object Monadic:
    import scala.concurrent.duration.Duration
    import scala.concurrent.{Await, ExecutionContext, Future}

    def transform[E, T](xs: Seq[Future[Either[E, T]]])(using ExecutionContext): Future[Either[E, Seq[T]]] =
      import cats.implicits.*
      Future.sequence(xs) // Future[Seq[Either[E, T]]
        .map(_.sequence) // equivalent to: _.traverse(identity)

    def transform2[E, T](xs: Seq[Future[Either[E, T]]])(using ExecutionContext): Future[Either[E, Seq[T]]] =
      val initial: Future[Either[E, List[T]]] = Future.successful(Right(List.empty[T]))
      xs.foldRight(initial): (future, acc) =>
        for
          f <- future
          a <- acc
        yield a.flatMap(lst => f.map(_ :: lst))

    @main def useMonadicTransform(): Unit =
      given ExecutionContext = ExecutionContext.global
      val xs = Seq(Future(Right("I")), Future(Right("Love")), Future(Right("Monads")))
      val result = transform(xs) // `transform2` works as well
      println(Await.result(result, Duration.Inf))
      val ys = Seq(Future(Right("...")), Future(Left("Nope!")))
      val result2 = transform(ys) // `transform2` works as well
      println(Await.result(result2, Duration.Inf))

  object Direct:
    import gears.async.default.given
    import gears.async.{Async, Future}
    import io.github.tassiluca.ds.effects.either
    import io.github.tassiluca.ds.effects.either.?

    def transform[E, T](xs: Seq[Future[Either[E, T]]])(using Async.Spawn): Future[Either[E, Seq[T]]] =
      Future:
        either:
          xs.map(_.await.?)

    @main def useDirectTransform(): Unit = Async.blocking:
      val xs = Seq(Future(Right("I")), Future(Right("Love more")), Future(Right("Direct Style")))
      val result = transform(xs)
      println(result.await)
      val ys = Seq(Future(Right("...")), Future(Left("Nope!")))
      val result2 = transform(ys)
      println(result2.await)
