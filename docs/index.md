---
marp: true
theme: uncover
footer: 'Scala 3 Direct Style Effect Management'
paginate: true
style: |
  h1 { font-size: 1.6rem; }
  h2 { font-size: 1.3rem; }
  h3 { font-size: 1.1rem; margin-bottom: 0.2em; }
  h4 { font-size: 1rem; }
  h5 { font-size: 0.9rem; }
  h6 { font-size: 0.85rem; }
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
    font-size: 0.7em;
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
---

<!-- _class: invert -->

# An introduction to _**Direct Style**_ _Effect Management_ in Scala 3

## Taming effects with **Capabilities**

Luca Tassinari

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

The control flow of the program is *explicit* and the program is written as a sequence of instructions that are executed one after the other:

```scala
val user   = fetchUser(id)
val orders = fetchOrders(user)
val total  = calculateTotal(orders)
total
```

**Way** easier to write and reason about!

</div>
</div>

---

<!-- _class: invert -->

### Can we get the advantages Monads give us in terms of effect management using _Direct Style_ rather than _Monadic Style_?

---

<!--
### Can we get the advantages Monads give us in terms of effect management using _Direct Style_ rather than _Monadic Style_?

### **Yes, we can!**

(with some caveats and major limitations that will be discussed later)

--- -->
<!-- 
Two ingredients are needed:

1. **Context functions**, a.k.a. `?=>`
2. **`boundary`** & **`break`**

--- -->

### Boundary & Break

Boundary & break is a mechanism allowing to prematurely break the computation returning a value to the client. From Scala 3.2 onwards, non-local returns are no longer supported and `boundary` and `boundary.break` should be used instead.

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
- `boundary` enriches the `body` with a `Label[T]` via a **context function**, i.e. a function of the form `(T1, ..., Tn) ?=> E` where `T1, ..., Tn` instances are available as _givens_ in `E`. Their values can be `summon`ed or are available as a _contextual argument_:

```scala
package scala.util

object boundary:

  inline def apply[T](inline body: Label[T] ?=> T): T = ...

  def break[T](value: T)(using label: Label[T]): Nothing = ...


// Explicit syntax (just for clarity)

boundary: (label: Label[Option[(T, T)]]) ?=>
  for i <- list1 do
    for j <- list2 do
      if predicate(i, j) then break(Some((i, j)))(using label)
  None
```

---

Using `boundary` and `break` it is possible to implement data types to handle errors "directly" with a short exit path.

```scala
def aggregate(xs: List[Uri]): Either[String, List[String]] = 
  either: // boundary
    xs.map(doRequest(_).?) // `?` break if `doRequest` returns a `Left`, otherwise unwrap the `Right`

def doRequest(endpoint: Uri): Either[String, String] =
  HttpClientSyncBackend().send(basicRequest.get(endpoint)).body
```

<div class="cols">
<div>

Using monadic style:

```scala
  def aggregate(xs: List[Uri]): Either[String, List[String]] =
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
  def aggregate(xs: List[Uri]): Either[String, List[String]] =
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

This is a simple example of _effect_ management using direct style where the _effect_ is the possibility of _failing_.

---

### Modeling effects with capabilities

CAPabilities for RESources and Effects (CAPRESE) is the ongoing research project at the Programming Methods Laboratory @ EPFL whose goal is to _upgrade_ the _Scala type system_ to _track effects_ directly in the program, without pushing them into external (monadic) frameworks.

_Idea_:

- to have an ***effect*** you need the ***capability*** enabling it;
- a capability is just a **value** passed to the function performing the effect, usually _implicitly_.

For example, `CanFail` is the capability enabling the effect of breaking in case of errors:

```scala
def f(using CanFail): Int = ...
```

Marking with `using CanFail` the function make explicit that the function may fail and require to be called in a context where the `CanFail` capability is available. If the function is called without the capability, the compiler will raise an error.

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

Similarly to `CanFail`, the **`CanThrow` capability** is available as an experimental feature to manage in an effectful way the possibility of throwing exceptions.

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

---

#### The suspension effect

As part of the CAPRESE research project, the [Gears]() library has been developed with the goal of providing an experimental tool for _asynchronous programming_, leveraging capabilities to model, in direct style, one of the most important effects: **suspension**.

- `Async` is the capability allowing a computation to suspend while waiting for the result of an asynchronous source.
  - `Async.group` is the `Async` capability generator
    ```scala
    def group[T](body: Async ?=> T)
    ```
- `Async.Spawn` is a special subtype of `Async` (`Async.Spawn <: Async`) that allows to spawn a new concurrent asynchronous computation.
  - `Async.blocking` is the root capability generator that creates an `Async.Sapwn` context blocking the running thread for suspension (usually placed inside the `main` or for testing purposes);
  ```scala
  def blocking[T](body: Async.Spawn ?=> T)
  ```

  $\Rightarrow$ Capabilities are hierarchical

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

  $\Rightarrow$ Composing effects with capabilities is straightforward: just add the required capabilities to the method signature.

---

Concrete service implementation:

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

- `authorBy` and `verifyContent` are executed concurrently - the concurrent execution is explicit opted-in by using a Gears `Future`;
- `Future`s can be _composed_ and _awaited_ for their result;
- **Structured concurrenct model**: every `Async` context has a completion group tracking all computations in a tree structure guaranteeing that when a group terminates all its dangling children are canceled.

---

<!-- _paginate: hold -->

Concrete service implementation:

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

- `Future`s can be _composed_ and _awaited_ for their result;
  -  `zip` operator allows combining the results of two `Future`s in a pair if both succeed, or fail with the first error encountered. Combined with `Async.group` the failure of one determines the cancellation of the other because the cancellation group is shared and once the `zip` completes the group is terminated (thus cancelling the other computation).
  Note: the only way `authorBy` and `verifyContent` can fail, making the `zip` return immediately the failure is by throwing an exception!

---
