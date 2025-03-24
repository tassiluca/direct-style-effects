---
marp: true
theme: uncover
footer: 'Scala 3 Direct Style Effect Management'
paginate: true
style: |
  h1 { font-size: 1.6rem; }
  h2 { font-size: 1.3rem; }
  h3 { font-size: 1.1rem; margin-bottom: 0.2em; }
  h4 { font-size: 0.9rem; margin-bottom: 0.2em; }
  p { 
    font-size: 0.6rem;
    line-height: 1.2;
    letter-spacing: 0.02em;
  }
  ul, ol {
    margin-left: 1em;
  }
  li {
    margin-bottom: 0.5em;
    font-size: 0.6rem;
    line-height: 1.2;
    letter-spacing: 0.02em;
  }
  strong {
    color: #0366D6;
  }
  h1 em {
    color: #4fca6c;
  }
  em {
    color: #36ab51;
  }
  code {
    font-size: 0.65em;
  }
  a {
    color: #0366D6;
    text-decoration: none;
  }
  .columns {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 1rem;
    align-items: center;
  }
  .columns img {
    width: 100%;
    height: auto;
  }
  .full-image {
    display: flex;
    justify-content: center;
    align-items: center;
    height: calc(100% - 50px);
  }
  .full-image img {
    max-width: 90%;
    max-height: 90%;
    object-fit: contain;
  }
  .cols {
    display: flex;
    gap: 16px;
  }
  .cols > div {
    flex: 1;
    padding: 0 10px;
    border-right: 1px solid #ccc;
  }
  .cols > div:last-child {
    border-right: none;
  }
  .smaller li, .smaller p, .smaller code, .smaller pre {
    font-size: 0.5rem !important;
  }
  .center-list ul {
    list-style: none;
    padding: 0;
    margin: 0 auto;
    width: fit-content;
  }
  .center-list li {
    text-align: center;
  }
  table {
    font-size: 0.6rem;
    letter-spacing: 0.02em;
    border-collapse: collapse;
    width: 100%;
  }
---

<!-- _class: invert -->

# _**Direct Style**_ _Effect Management_ in Scala 3

## Taming effects with **Capabilities**

Luca Tassinari

<div class="smaller">

25/03/2025

</div>

---

## What **Direct Style** is?

<div class="cols">
<div>

#### Monadic Style

The control flow is governed by `flatMap` operation.

```scala
for
  user   <- fetchUser(id)
  orders <- fetchOrders(user)
  total  <- calculateTotal(orders)
yield total
```

</div>
<div>

#### Direct Style

The control flow of the program is *explicit* and the program is written as a sequence of instructions that are executed one after the other, resembling imperative code, while still being in a _functional framework_ (with all its benefits):

```scala
val user   = fetchUser(id)
val orders = fetchOrders(user)
val total  = calculateTotal(orders)
total
```

Way easier to write and reason about!

</div>
</div>

---

<!-- _class: invert -->
<div class="center-list">

### Can we get the benefits Monads give us in terms of effect management using _Direct Style_ rather than _Monadic Style_?

* ### **Yes (we will\*)!**

<br>

  * \* some limitations are still being addressed

</div>

---

### Boundary & Break

Boundary & break is a mechanism allowing to prematurely break the computation returning a value to the client. From Scala 3.2 onwards, non-local returns are no longer supported and `boundary` and `break` should be used instead.

Using non-local returns:

```scala
def findFirstMatchingPair[T](list1: List[T], list2: List[T])(predicate: (T, T) => Boolean): Option[(T, T)] =
  for i <- list1 do
    for j <- list2 do
      if predicate(i, j) then return Some((i, j))
  //                          ~~~~~~~~~~~~~~~~~~~
  // Non local returns are no longer supported; use `boundary` and `boundary.break` in `scala.util` instead
  None
```

Using `boundary` and `break`:

```scala
def findFirstMatchingPair[T](list1: List[T], list2: List[T])(predicate: (T, T) => Boolean): Option[(T, T)] =
  boundary:
    for i <- list1 do
      for j <- list2 do
        if predicate(i, j) then break(Some((i, j)))
    None
```

---

### Context Functions

- `break` is a method requiring a `Label[T]` context;
- `boundary` enriches the `body` with a `Label[T]` via a **context function**, i.e., a function of the form <br> `(T1, ..., Tn) ?=> E` where `T1, ..., Tn` instances are available as _givens_ in `E`. Their values can be summoned or are available as a _contextual argument_:

```scala
package scala.util

object boundary:

  inline def apply[T](inline body: Label[T] ?=> T): T = ...

  def break[T](value: T)(using label: Label[T]): Nothing = ...

// Explicit desugared version using a contextual argument
boundary: (label: Label[Option[(T, T)]]) ?=>
  for i <- list1 do
    for j <- list2 do
      if predicate(i, j) then break(Some((i, j)))(using label)
  None
```

---

By using `boundary` and `break` it is possible to implement data types to handle errors "directly" with a short exit path more easily than with monads.

```scala
def aggregate(xs: List[Uri]): Either[Error, List[Response]] = 
  either: // boundary
    xs.map(doRequest(_).?) // `?` break if `doRequest` returns a `Left`, otherwise unwrap the `Right`

def doRequest(endpoint: Uri): Either[Error, Response] =
  HttpClientSyncBackend().send(basicRequest.get(endpoint)).body
```

<div class="cols">
<div>

Using monadic style:

```scala
  def aggregate(xs: List[Uri]): Either[Error, List[Response]] =
    xs.foldLeft[Either[String, List[String]]](Right(List.empty)): 
      (acc, uri) =>
        for
          results <- acc
          response <- doRequest(uri)
        yield results :+ response
```
</div>

<div>

Could be simplified using Cats `traverse`, yet there remains considerable complexity behind it...

```scala
  def aggregate(xs: List[Uri]): Either[Error, List[Response]] =
    import cats.implicits.toTraverseOps
    // "Given a function which returns a G effect, thread 
    // this effect through the running of this function on 
    // all the values in F, returning an F[B] in a G context."
    //
    //    def traverse[G[_]: Applicative, A, B](
    //      fa: F[A]
    //    )(f: A => G[B]): G[F[B]]
    xs.traverse(doRequest)
```

</div>
</div>

---

Functions requiring the `CanFail` type can promptly break the computation upon encountering an error. On the calling side, the label is defined using the `either` boundary.

```scala
type CanFail = Label[Left[String, Nothing]]

def getUser(id: UserId)(using CanFail): User =
    val user = userBy(id)
    if verifyUser(user) then user else fail("Incorrect user")
    // `fail` is a shorthand for `break(Left("Incorrect user"))`

def getPayment(user: User)(using CanFail): PaymentMethod =
  paymentMethodOf(user) match
    case Some(a) if verifyMethod(a) => a
    case Some(_) => fail("The payment method is not valid")
    case _ => fail("Missing payment method")

def paymentData(id: UserId) = either:
  val user = getUser(id)
  val address = getPayment(user)
  (user, address)
```

This is a simple example of _effect_ management in direct style, where the _effect_ is the possibility of _failure_.

---

### Modeling effects with capabilities

[CAPabilities for RESources and Effects (CAPRESE) &#x2197;](https://abgruszecki.github.io/publication/capturing-types/capturing-types.pdf) is the ongoing research project at the Programming Methods Laboratory @ EPFL whose goal is to _upgrade_ the _Scala type system_ to _track effects_ directly in the program, without pushing them into external (monadic) frameworks.

_Idea_:

- to have an ***effect*** you need the ***capability*** enabling it;
- a capability is just a **value** passed to the function performing the effect, usually _implicitly_.

For example, `CanFail` is the capability enabling the effect of breaking in case of errors:

```scala
def f(using CanFail): Int = ...
```

Marking a function with `CanFail` explicitly indicates that the function may fail and must be called in a context where the `CanFail` capability is available. If the function is called without the capability, the compiler will raise an error.

---

**The `CanFail` capability**

```scala
/** The CAPABILITY enabling to break the computation 
  * returning a [[Left]] with an useful string-encoded message.
  */
type CanFail = Label[Left[String, Nothing]]

object either:

  /** Defines the boundary for the [[Either]] returning computation, whose [[body]] is given in input. */
  inline def apply[L, R](inline body: Label[Left[L, Nothing]] ?=> R): Either[L, R] =
    boundary(Right(body))

  /** Quickly break to the enclosing boundary with a [[Left]] filled with [[l]]. 
    * This is the EFFECT.
    */
  inline def fail[L, R](l: L)(using Label[Left[L, R]]): Nothing = break(Left(l))

  extension [L, R](e: Either[L, R])
    /** @return this [[Right]] value or break to the enclosing boundary with the [[Left]] value. 
      * This is the EFFECT.
      */
    inline def ?(using Label[Left[L, Nothing]]): R = e match
      case Right(value) => value
      case Left(value) => break(Left(value))
```

---

Similarly to `CanFail`, the **`CanThrow` capability** is available as an _experimental_ feature to manage in an effectful way the possibility of throwing exceptions.

```scala
// CanThrow capability is an experimental feature and can be enabled with the following import
import language.experimental.saferExceptions

class DivisionByZero extends Exception

// Or equivalently:
// def div(n: Int, m: Int): Int throws DivisionByZero
def div(n: Int, m: Int)(using CanThrow[DivisionByZero]): Int = m match
  case 0 => throw DivisionByZero() // the effect
  case _ => n / m

try
  // the compiler makes available the CanThrow capability as given instance inside the try block
  div(10, 0)
catch case _: DivisionByZero => "Division by zero"

// If you attempt to call `div` outside the `try` block, the compiler will raise an error
div(10, 0)
//      ^^^
// Error: The capability to throw exception is missing.
```

The `CanThrow` capability behaves similarly to Java checked exceptions, but it is more flexible, allowing them to be thrown in higher-order functions.

---

#### The suspension effect

As part of the CAPRESE project, the [Gears &#x2197;](https://github.com/lampepfl/gears) library has been developed with the goal of providing support for _asynchronous programming_, leveraging capabilities to model, in direct style the **suspension** effect.

  ![h:300](./res/cap-hierarchy.svg)


- `Async` is the capability allowing a computation to suspend while waiting for the result of an asynchronous source.
- `Async.Spawn` is a special subtype of `Async` (`Async.Spawn <: Async`) that allows to spawn a new concurrent asynchronous computation.
  
  $\Rightarrow$ **Capabilities form a hierarchical structure.**

---

You can get the `Async` and `Async.Spawn` capabilities via:

- `Async.blocking` 
  - is the root capability generator;
  - it executes asynchronous computation `body` on currently running thread. The thread will suspend when the computation waits;
  - usually placed inside the `main` or for testing purposes;

```scala
def blocking[T](body: Async.Spawn ?=> T): T
```

- `Async.group`: runs `body` inside a spawnable context where it is allowed to spawn concurrently runnable Futures. It requires an implicit `Async` instance to be in scope;

```scala
def group[T](body: Async.Spawn ?=> T)(using Async): T
```

Gears supports both JVM and Native platforms: on the JVM, it is built on top of Virtual Threads, and on Scala Native, it leverages delimited continuations.

---

Example: a service allowing storing posts, performing checks on the content and author before the actual storage performed concurrently.

```scala
/** The repository in charge of storing and retrieving blog posts. */
trait PostsRepository:
  def save(post: Post)(using Async, CanFail): Post
  def exists(postTitle: Title)(using Async, CanFail): Boolean
  def load(postTitle: Title)(using Async, CanFail): Option[Post]

/** The service exposing a set of functionalities to interact with blog posts. */
trait PostsService:
  def create(authorId: AuthorId, title: Title, body: Body)(using Async, CanFail): Post
  def get(title: Title)(using Async, CanFail): Option[Post]
```

- `Async` capability allows methods to suspend while waiting for the result;
- `CanFail` capability allows methods to break the computation returning an error message in case of failure.

  $\Rightarrow$ **Composing effects with capabilities is straightforward**: just add the required capabilities to the method signature.

---

<div class="smaller">

```scala
class PostsServiceImpl extends PostsService:

  override def create(authorId: AuthorId, title: Title, body: Body)(using Async, CanFail): Post =
    if context.repository.exists(title) then fail(s"A post entitled $title already exists")
    val (post, author) = Async.group:
      val content = Future(verifyContent(title, body))
      val author = Future(authorBy(authorId))
      content.zip(author).awaitResult.?
    context.repository.save(Post(author, post._1, post._2, Date()))

  /* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
  private def authorBy(id: AuthorId)(using Async): Author = ...

  /* Some local computation that verifies the content of the post is appropriate. */
  private def verifyContent(title: Title, body: Body)(using Async): PostContent = ...
```

* `authorBy` and `verifyContent` are executed concurrently - the concurrent execution is explicit opted-in by using a Gears `Future`;
* **Structured concurrency model**: every `Async` context has a completion group tracking all computations in a tree structure guaranteeing that when a group terminates all its dangling children are canceled.
* `Future`s can be _composed_ and _awaited_ for their result;
  -  `zip` operator allows combining the results of two `Future`s in a pair if both succeed, or fail with the first error encountered. Combined with `Async.group` the failure of one determines the cancellation of the other because the cancellation group is shared and once the `zip` completes the group is terminated.

</div>

---

A comparison

| Feature | Gears Futures | Scala monadic Futures |
|:--------|:--------------|:----------------------|
| Structured concurrency | ✅ | ❌ |
| Cancellation support | ✅ | ❌ |
| Referential transparency | ⚠ `Future`'s aren't, but `Task`s are delayed referential transparent `Future`s. | ❌ Once declared, they are spawned. |
| Concurrency model | By default the code is serial. If you want to opt-in concurrency you have to explicitly use a Future or Task. | By default, the code is concurrent. |

Other notable features of Gears (here not covered):

- other composition operators, like `or`, `orWithCancel`, `awaitAll`;
- `Channel`s for communication between concurrent computations;

<div class="smaller">

You can find a detailed insight and some examples [here &#x2197;](https://tassiluca.github.io/direct-style-experiments/).

</div>

---

#### Capabilities leakage problem

`IO` capability to manage input/output operations:

```scala
/** The capability enabling writing and reading from a source. */
trait IO:
  /** IO Write effect operation. */
  def write(content: String)(using CanFail): Unit
  /** IO read effect operation. */
  def read[T](f: Iterator[String] => T)(using CanFail): T

object IO:

  def write(x: String)(using IO, CanFail): Unit = summon[IO].write(x)
  def read[T](f: Iterator[String] => T)(using IO, CanFail): T = summon[IO].read(f)

  /** File IO capability generator. */
  def file[R](path: Path)(body: IO ?=> R): Runnable[R] = () =>
    given IO with // effect handler
      def write(content: String)(using CanFail): Unit =
        Using(PrintWriter(path.toFile)): writer =>
          writer.write(content)
        .?
      def read[T](f: Iterator[String] => T)(using CanFail): T =
        Using(Source.fromFile(path.toFile)): source =>
          f(source.getLines())
        .?
    body
```

---

**Problem**:

```scala
@main def breakingIO = either:
  val path = Path.of("test.txt")
  // The file lines are returned as an iterator; no lines are read at this point...
  val eff: Runnable[Iterator[String]] = IO.file(path):
    read(identity)
  // ...when we try to execute the effect, the lines are read but the stream is already closed!
  val content = eff.run().next() // throws "IOException: Stream Already Closed"!!!
  println(content)
```

- the `IO.file` body returns an `Iterator[String]` that is lazily evaluated
  - _Consequence_: the lines are read only after the file has been closed, which results in an `IOException` being thrown;
  - the same happens if, for example, a lambda is returned, as shown here:
    ```scala
    IO.file(path):
      () => read(_.mkString)
    ```
- **No compilation error** is raised, the error is raised only at runtime!

---

<!--

The idea to solve this issue is that the Scala type system needs to be updated to keep track of capabilities lifetime and this is done by what is called **capture checking**, which is essentially a new phase of the scala type checker that
leverage on new capturing types to catch the leakage of capabilities.

A capturing type is of the form...

-->

### Capture checking

To solve this issue `IO` needs to be marked as a _capability_ and the return type of the `file` method must keep track of the capability lifetime by using a **capturing type** of the form:

`T^{cap1, cap2, ..., capN}` 

where `T` is a regular type and `{cap1, cap2, ..., capN}` is called **capture set** and it represents the set of capabilities that the type `T` can capture or reference.

- if the capture set is empty the type is **pure**, otherwise it is considered **impure**;
- `cap` is the **universal capability** which is the most sweeping capability from which all other capabilities are derived;
- When a class `C` extends the `Capability` trait, it is the same as if the type of `C` is `C^{cap}`

```scala
trait IO extends Capability:
  def write(content: String)(using CanFail): Unit
  def read[T](f: Iterator[String] => T)(using CanFail): T

object IO:
  def file[R](path: Path)(body: IO ?=> R): Runnable[R]^{body} = /* same as before */
```

---

Now, the compiler recognizes we are trying to assign to the type variable `R` a capturing type carrying the universal capability, which is forbidden and a compilation error is raised:

```scala
@main def breakingIO = either:
  val path = Path.of("test.txt")
  val eff = IO.file(path):
    () => read(_.mkString)
  //~~~~~~~~~~~~~~~~~~~~~~
  // Compilation error: local reference IO leaks into outer capture 
  // set of type parameter R of method file in object IO
  val content = eff.run()
```

However, in some cases, the compiler is still unable to detect the leakage of capabilities. For instance, in the example above, where an `Iterator[String]` is returned. The Scala 3 standard library has not yet been fully covered by the capture checking mechanism.

---

### Recap

- Direct style frameworks are simpler to use and reason about than monadic style;
- It is possible to manage effects in direct style using **capabilities**, like `CanFail`, `CanThrow`, `IO`, `Async`, etc;
  - Gears is an experimental library that allows to model the suspension effect in direct style providing continuation-based structured concurrency;
- **Capture checking** is needed to ensure that the capabilities are not leaked outside the scope where they are intended to be used.
  - Type system will distinguish between _pure_ and _impure_ types, where impure types can capture capabilities, while pure types cannot.
  - Currently, capture checking has been already applied to parts of the Standard Library and the Scala 3 compiler. The Caprese project is working to extend this feature to the whole Scala 3 ecosystem.

---

### References

- [Martin Odersky, Aleksander Boruch-Gruszecki, Edward Lee, Jonathan Brachthäuser, Ondřej Lhoták, Scoped Capabilities for Polymorphic Effects](https://arxiv.org/abs/2207.03402)
- [Gears project library](https://github.com/lampepfl/gears)
- [Capture checking](https://dotty.epfl.ch/docs/reference/experimental/cc.html)
- [`CanThrow` capability](https://dotty.epfl.ch/docs/reference/experimental/canthrow.html)
