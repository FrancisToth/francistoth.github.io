---
layout: post
title:  Functional Design
date:   2021-02-02 07:00:00 -0500
brief:
---

> This is a long due post following the talks given recently at [Dawscon](https://youtu.be/OE_rRu7Uv_E), [CodeMesh](https://www.youtube.com/watch?v=0sXxQyi0UTA&feature=emb_logo), and Scala Toronto about Functional Design (slides are available [here](/talks/)).

Considering the amount of material available today, Software Design is rather intimidating. When it comes to best practices, one can get overwhelmed quickly and end up with no idea about how to tackle a given problem. Indeed, these guidelines can be vague, or too specific, if not contradictory sometime.

The problem is that it's impossible to get all these ideas right without properly understanding their essence. Unfortunately, this is something that tends to be forgotten when teaching design. Too often, we tend to overwhelm people with dozens and dozens of guidelines without actually conveying what ties them all together.

Coding is like having a civilized and gentle conversation with the future reader of the code. It is about expressing concepts in an intelligible way, to ultimately convince the reader about your solution's correctness. Properly conveying ideas requires these to be organized and structured, so that each of them can be fully understood separately. In some way, this exercise is very similar to what is done when writing a speech or an e-mail.

## The Path to Abstraction

```scala
def incByOne(i: Int): Int = ???
```
```scala
scala> val x = incByOne(0)
x: Int = 1
scala> val y = incByOne(x)
x: Int = 2
```
As it seems and according its signature, `incByOne` is a function responsible for incrementing the `Int` it is provided with incremented by one. This function could be implemented in different ways, using bitwise operators or simply the `+` function, but that's not really relevant here. In fact, all we care about is that `incByOne` **does what it claims to do**.

In some cases though, `incByOne`'s implementation may matter. Especially if given a specific argument, it ends up producing an unexpected result:
```scala
scala> incByOne(-1)
java.lang.IllegalArgumentException: KABOOM
  ... 32 elided

incByOne(42)
This is the meaning of life!
res0: Int = 42
```
This situation has two consequences. First, it's no longer possible to call `incByOne` and be 100% sure about what it will produce without looking at its internals. In other words, `incByOne` can no longer be **reasoned about** without opening it up and guessing what will be produced at runtime. Secondly, refactoring capabilities get lost:
```scala
val a = incByOne(10)

// prog1 cannot be used in place of prog2 and vice-versa
val prog1 = a
val prog2 = incByOne(10)
```
As `incByOne(10)` may produce an unexpected result, we cannot replace it by `a` and guarantee that once executed, the program will produce **the exact same output** than before the refactoring.

## Bringing sanity back

Let's now compare the two following functions and think about their respective inputs and outputs:

{{< scala >}}
{{< scala2 >}}
```scala
def foo(number: Int): Boolean = 
  number == 42

def bar(number: Int): Boolean = {
  println("Checking number")
  number == 42
}
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
def foo(number: Int): Boolean = 
  number == 42

def bar(number: Int): Boolean =
  println("Checking number")
  number == 42
```
{{< /scala3 >}}
{{< /scala >}}
According their signature `foo` and `bar` both take an `Int` and produce a `Boolean`. However, calling `bar` results also in printing out `"Checking number"` on the console. Note that this extra output is **not captured anywhere** in `bar`'s signature.

This second output is called a **side-effect**. In practice, a **side-effect** is created whenever a function:
- **requires some input which is not part of its argument list**,
- and/or **produces an output which is not captured by its result type**.

In other words, a **side-effect** is produced when a function interacts with its environment other that through its arguments or its returned type. As explained earlier, this has important consequences in terms of refactoring capabilities but not only. Compared to `bar` and `incByOne`, functions such as `foo` have a very interesting property called **Local Reasoning**.

>**Local Reasoning** enables a reader to make sense of a function without looking at how it's implemented.

This is a key principle in Software Design and is what enables a component to be abstracted over without knowing about its internals. A good analogy for this is language. In common language, we do not need to explain how a car works every time one needs to be mentioned. The word _car_ can actually be used to define/compose more sophisticated concepts (such as a _sports car_) and express ourselves in a more concise and meaningful way.

Back to Software Design, it is common to see codebases having reached a level of complexity preventing their maintainers to do any change without risking major breakdowns. The problem is that beyond a certain point, it is impossible to picture how a program behaves at runtime and be 100% confident about its output without relying on proper abstractions. In other words, **if we cannot reason about a word/function's definition, there is no way we can abstract over it to express a higher level concept**.

## Back to real world

**Local Reasoning** is a critical concept but there is one issue though. It
prevents using exceptions, null values, and any statement in general (`println`, `readLine`...) as these all result in some side-effect or output that cannot be captured by a function's signature.

However, **side-effects** are a necessary evil. Indeed, these are always needed whether to get some data from the user, load a configuration, access a database or else. So **Local Reasoning** is pretty cool on paper, but when it comes to real-world use cases, finding a middle ground is required:

{{< scala >}}
{{< scala2 >}}
```scala
import scala.io.StdIn.readLine

def welcome(): Unit = {
  val name = readLine()
  println("Hi " + name + "!")
}
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
import scala.io.StdIn.readLine

def welcome(): Unit =
  val name = readLine()
  println("Hi " + name + "!")
```
{{< /scala3 >}}
{{< /scala >}}
As you probably guess, this program contains two side-effects:
- `readLine()` which performs a read from the console
- `println()` which outputs a string on it

Unfortunately, the inputs and the outputs of these functions cannot be captured in any way. This is due to the nature of `readLine()` and `println()` which are **statements**. **Statements** are units of execution being run for their side-effects only. For this reason, they are eager, non-deterministic (as anything could happen at runtime), and cannot be replaced by the value they produce. There's not much we can do about this, but there should be a way to delay their execution, and represent them so that they are locally reasonable.

## Classical approach

Usually, this kind of problem is solved by introducing a dependency:
{{< scala >}}
{{< scala2 >}}
```scala
trait Console {
  def putStrLn(s: String): Unit
  def getStrLn(): String
}

def program(console: Console): Unit = {
  val name = console.getStrLn()
  console.putStrLn("Hi " + name + "!")
}
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
trait Console:
  def putStrLn(s: String): Unit
  def getStrLn(): String

def program(console: Console): Unit =
  val name = console.getStrLn()
  console.putStrLn("Hi " + name + "!")
```
{{< /scala3 >}}
{{< /scala >}}
This approach makes `program` a bit safer to use because we have now control over how side-effects are performed, but calling `putStrLn` or `getStrLn()` may still result in a side-effect, preventing them to be reasoned about locally. Another downside is that this approach is a poor way to manage dependencies in general as these have to be provided up-front. Applied to a more complex program, this strategy may result indeed in a lack of flexibility and in providing an ever growing context any time we need to use `program`:

{{< scala >}}
{{< scala2 >}}
```scala
def complexProgram(module1: Module1, ..., mn: ModuleN): Unit = {
  val a = foo(module1, ..., mn)
  val b = bar(module2, ..., mn)
  fooBar(a, b, module1, module2 ..., mn)
}
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
def complexProgram(module1: Module1, ..., mn: ModuleN): Unit =
  val a = foo(module1, ..., mn)
  val b = bar(module2, ..., mn)
  fooBar(a, b, module1, module2 ..., mn)
```
{{< /scala3 >}}
{{< /scala >}}

## From Statements to Values

Another approach is to bring these statements back to the world of values:
{{< scala >}}
{{< scala2 >}}
```scala
import scala.io.StdIn.readLine

class IO[A](val run: () => A)

object IO {
  def apply[A](run: => A): IO[A] = 
    new IO(() => run)

  def putStrLn(s: String): IO[Unit]   = IO(println(s))
  def getStrLn           : IO[String] = IO(readLine())
}
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
import scala.io.StdIn.readLine

class IO[A](val run: () => A)

object IO:
  def apply[A](run: => A): IO[A] = 
    new IO(() => run)

  def putStrLn(s: String): IO[Unit]   = IO(println(s))
  def getStrLn           : IO[String] = IO(readLine())
```
{{< /scala3 >}}
{{< /scala >}}
`IO` models a lazy instruction (`run`) which once executed produces an `A`. This enables the conversion of statements such as `println` into values, and to delay the resulting side-effects produced during execution. In order to model the previous program, we would however need a way to sequence two `IO` which  can be done using the `andThen` operator:

{{< scala >}}
{{< scala2 >}}
```scala
class IO[A](val run: () => A) {
  def andThen[B](f: A => IO[B]): IO[B] =
    IO(f(run()).run())
}
// ...
import IO._

// description of the program ('the what')
val welcome: IO[Unit] =
  getStrLn.andThen(name =>
    putStrLn("Hi " + name + "!")
  )
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
class IO[A](val run: () => A):
  def andThen[B](f: A => IO[B]): IO[B] =
    IO(f(run()).run())
// ...
import IO._

// description of the program ('the what')
val welcome: IO[Unit] =
  getStrLn.andThen(name =>
    putStrLn("Hi " + name + "!")
  )
```
{{< /scala3 >}}
{{< /scala >}}
`andThen` pipes the result of an `IO` to a function producing another `IO` (you may know this combinator as `flatMap`). Using `andThen`, we can now express the previous program and substitute `welcome` by its definition without affecting the program's final output:
```scala
// prog1 and prog2 are indeed equivalent
val prog1 = welcome
val prog2 = getStrLn.andThen(name => putStrLn("Hi " + name + "!")
```
But `welcome` is just a value, and cannot do much on its own. It's a simple data-structure **describing** what we'd like to do. To materialize this description, we have to call `run`:
```scala
// execution of the program (the 'how')
welcome.run()
```
Whenever `run` is called, the program performs any side-effect required to produce the final value. This encoding leads to a complete separation of a program's description (the _what_) and its execution (the _how_). As long as `run()` is not called, we keep the guarantees provided by local reasoning along with its super-powers, and have control over **WHEN** side-effects are performed.

This tells us something about when and where side-effects should be executed. As nothing can be guaranteed beyond the execution of a side-effect, we should design the program so that it's always the last thing we do. Once we get to that point, we lose **Local Reasoning** and have reached the **edges** of the program.

## Hexagonal Architecture

Let's take a quick detour and talk about what we mean by **edges**. As we've seen it earlier **Local Reasoning** gets compromised whenever side-effects come into play. In order to keep the code locally reasonable, we therefore delay the moment when side effects are executed until they are absolutely needed. This practice has actually been "preached" since a long time ago. In general, a business application can be divided in two main sections:
- The **business logic or the core**, which is prone to change a lot
- and the **infrastructures** relying on it, which are pretty static

{{< image div_style="" img_style="display: block; margin: 0 auto; background-color: transparent; width: 50%;" src="/talks_data/20210115_functionaldesign/images/design_3.png">}}

The **infrastructures** (such as a testing, a file or a database layer) all depend on the **core** and reside therefore at the edges of the program's architecture, while the core is completely agnostic about how it is used (by leveraging [inversion of control](https://en.wikipedia.org/wiki/Inversion_of_control)). This approach has different names ([Hexagonal architecture](https://en.wikipedia.org/wiki/Hexagonal_architecture_(software)), [Onion architecture](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/), [Ports and Adapters](https://www.kennethlange.com/ports-and-adapters/)) but overall the goal is always the same: **Keep what changes the most (the core) independent of what uses it (the infrastructures)**.

As it is more common to modify the business logic of an application than its infrastructures, it is paramount to prevent the core from being polluted with any aspects related to its context of usage or execution. This guarantees that the same business logic can be re-used in multiple contexts (eg: unit testing, integration testing, production, ...) without modifying it.

## Revisiting IO

Let's look back at our example. `welcome` is locally reasonable, but it  has a direct dependency on its execution details (represented by `run`). Ideally, we'd like to invert this dependency so that the business logic described by `welcome` can be re-used to create programs interacting with different environment such as a console, a web server, or anything else.

Secondly this approach shows some limits in the testing phase:

```scala
// Using scala-test
"StrLn" should "read an input from the console" in {
  val actual: IO[String] = ???
  actual.run() should be getStrLn.run() // ???
}
```
With the current implementation, two `IO` cannot be compared without executing their respective side-effects, which brings us back to square one. Let's see if we can solve this problem using a different encoding:
{{< scala >}}
{{< scala2 >}}
```scala
sealed trait IO[A] { self =>
  def andThen[B](f: A => IO[B]): IO[B] =
    IO.AndThen(self, f)
}
object IO {
  case class PutStrLn(s: String) extends IO[Unit]
  case object GetStrLn           extends IO[String]

  case class AndThen[A, B](
    io: IO[A], 
    f: A => IO[B]
  ) extends IO[B]

  def putStrLn(s: String): IO[Unit]   = PutStrLn(s)
  def getStrLn           : IO[String] = GetStrLn
}

// ...
val welcome: IO[Unit] =
  getStrLn.andThen(name =>
    putStrLn("Hi " + name + "!")
  )
// AndThen(GetStrLn, name => PutStrLn("Hi " + name + "!"))
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
enum IO[A]:
  self =>

  def andThen[B](f: A => IO[B]): IO[B] =
    AndThen(self, f)

  case PutStrLn(s: String) extends IO[Unit]
  case GetStrLn            extends IO[String]
  case AndThen[A, B](io: IO[A], f: A => IO[B]) extends IO[B]

object IO:
  def putStrLn(s: String): IO[Unit]   = PutStrLn(s)
  def getStrLn           : IO[String] = GetStrLn

// ...
val welcome: IO[Unit] =
  getStrLn.andThen(name =>
    putStrLn("Hi " + name + "!")
  )
// AndThen(GetStrLn, name => PutStrLn("Hi " + name + "!"))
```
{{< /scala3 >}}
{{< /scala >}}

In this encoding, each instruction of our API is represented by a pure  data-structure. The definition of `welcome` stays the same, but this time it is represented by a recursive tree structure which can be inspected, traversed and even optimized if needed. Secondly, instead of embedding the evaluation function `run` into `IO`, we define it aside:

{{< scala >}}
{{< scala2 >}}
```scala
def run[A](program: IO[A]): A = 
  program match {
    case GetStrLn      => scala.io.StdIn.readLine()
    case PutStrLn(s)   => println(s)
    case AndThen(c, f)   =>
      // not stack safe!!
      val io = f(run(c))
      run(io)
  }
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
def run[A](program: IO[A]): A = 
  program match
    case GetStrLn      => scala.io.StdIn.readLine()
    case PutStrLn(s)   => println(s)
    case AndThen(c, f)   =>
      // not stack safe!!
      val io = f(run(c))
      run(io)
```
{{< /scala3 >}}
{{< /scala >}}

From a testing perspective, this approach provides a solution to the problem described earlier. Indeed, all we require now is an additional test-specific evaluation function:

{{< scala >}}
{{< scala2 >}}
```scala
// program'state
case class State(inputs: List[String], outputs: List[String] = List.empty) {

  def popInput(default: String): (State, String) = 
    (copy(inputs = inputs.tail), inputs.headOption.getOrElse(default))

  def pushOutput(s: String): (State, Unit) =
    (copy(outputs = outputs :+ s), ())
}

// test-specific evaluation function / interpreter
def testRun[A](program: IO[A], state: State): (State, A) =
  program match {
    case GetStrLn     => state.popInput("Inputs exhausted!")
    case PutStrLn(s)  => state.pushOutput(s)
    case AndThen(io, f) =>
      // not stack safe!!
      val (state0, a) = testRun(io, state)
      testRun(f(a), state0)
  }
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
// program'state
case class State(inputs: List[String], outputs: List[String] = List.empty):
  def popInput(default: String): (State, String) = 
    (copy(inputs = inputs.tail), inputs.headOption.getOrElse(default))

  def pushOutput(s: String): (State, Unit) =
    (copy(outputs = outputs :+ s), ())

// test-specific evaluation function / interpreter
def testRun[A](program: IO[A], state: State): (State, A) =
  program match
    case GetStrLn     => state.popInput("Inputs exhausted!")
    case PutStrLn(s)  => state.pushOutput(s)
    case AndThen(io, f) =>
      // not stack safe!!
      val (state0, a) = testRun(io, state)
      testRun(f(a), state0)
```
{{< /scala3 >}}
{{< /scala >}}

`run` and `testRun` go through each layer of the program provided and perform any side-effect required to produce the final value. Note these implementations are not stack-safe and would blow up with infinite recursive programs. This can be fixed using Trampolining but that will be the topic of another blog post. In any case, thanks to this approach, comparing two `IO` is now trivial:

```scala
val actual   = testRun(welcome, State(List("Bob")))
actual shouldBe (State(List.empty, List("Hi Bob!")), ())
```

Let's take some steps back and look at where interpreters fit in the **Hexagonal Architecture**. Each interpreter is specific to the layer / context where it is used, maintains a direct dependency towards the core, and therefore resides at the edges of the architecture like shown on this diagram:

{{< image div_style="" img_style="display: block; margin: 0 auto; background-color: transparent; width: 50%;" src="/talks_data/20210115_functionaldesign/images/design_4.png">}}

Note that this approach also opens the door for optimization. We could for example write an interpreter which only purpose is to translate some business logic to an optimized evaluation function that maximizes performances at runtime, or one that optimizes the resulting data-structure into something more manageable. The Sky is the limit.

## Composition

Despite being different, these two encodings happen to have many similarities. Indeed, both approaches model the domain in terms of **primitives**, **constructors** and **operators**. This is something we've already covered in a [previous post](https://francistoth.github.io/2020/09/22/composition.html), but it would be awkward not to mention this here.  In any case, these building blocks are what enables us to introduce the third principle of **Functional Design** which is **Composition**.

If we think about it, designing software goes back to create small simple blocks and to combine these using operators to build bigger blocks. This is the essence of **Composition**. However, this cannot be achieved if we cannot abstract over these blocks. Hence why **Local Reasoning** and **Purity** are so critical.

**Local Reasoning**, **Purity** and **Composition** are therefore the three fundamentals we should look for when writing Software, as these pillars will allow an API to be decomposed and recomposed in order to support new business requirements or allow us/you/one to modify & simplify existing business requirements whilst minimizing complexity.

## To infinity and beyond

The example we took is rather simple. Let's add some spice and think about how error recovery could be implemented. In order to achieve this, we would need two additional primitives:

{{< scala >}}
{{< scala2 >}}
```scala
sealed trait IO[+A] { self =>
  // ...
  def fail(th: Throwable): IO[A] = IO.fail(th)
  def retry(n: Int): IO[A]       = IO.Retry(self, n)
}
object IO {
  case class Fail(th: Throwable)         extends IO[Nothing]
  case class Retry[A](io: IO[A], n: Int) extends IO[A]
  // ...
  def fail[A](th: Throwable)     : IO[A] = Fail(th)
  def retry[A](io: IO[A], n: Int): IO[A] = Retry(io, n)
}

def run[A](io: IO[A]): Try[A] =
  io match {
      // ...
    case Fail(th)     => Failure(th)
    case Retry(io, n) =>
      run(io) match {
        case Success(a)            => Success(a)
        case Failure(th) if n <= 1 => run(fail(th))
        case Failure(th)           => run(Retry(io, n - 1))
      }
  }
```
{{< /scala2 >}}
{{< scala3 >}}
```scala
enum IO[+A]:
  self =>
  // ...
  case Fail(th: Throwable)      extends IO[Nothing]
  case Retry(io: IO[A], n: Int) extends IO[A]

object IO:
  def fail[A](th: Throwable): IO[A]      = Fail(th)
  def retry[A](io: IO[A], n: Int): IO[A] =  Retry(io, n)

import scala.util.{Try, Success, Failure}
import IO._
def run[A](io: IO[A]): Try[A] =
  io match
      // ...
    case Fail(th)     => Failure(th)
    case Retry(io, n) =>
      run(io) match
        case Success(a)            => Success(a)
        case Failure(th) if n <= 1 => run(fail(th))
        case Failure(th)           => run(Retry(io, n - 1))
```
{{< /scala3 >}}
{{< /scala >}}

With these new building blocks in our tool belt, we can now express more sophisticated program such as this one:

```scala
val welcome: IO[Unit] = getStrLn.andThen(login =>
  if(login != "admin")
    fail(new InvalidLoginException())
  else
    putStrLn("Hi " + name + "!")
)
```

> Note the type of `run`. It returns a `Try[A]` which captures all the outputs `run` can produce. `Try[A]` is referred to as an effect, which in contrast with a _side-effect_ is expected by the caller of `run`. In other words, the difference between an _effect_ and a _side-effect_ is its expected nature.

Now let's think about how would we describe the same program using a more classical or imperative approach. We would probably need a for-loop, a try-catch, a bunch of if-blocks, and end up with a program that is 20 lines long with no way to reuse the logic we've just created. **Functional Design** enables us to do exactly that and to express more powerful constructs with minimal changes.

## Costs

You may wonder about the complexity of the encoding. Keep in mind that in real-world scenarios, instead of re-inventing the wheel, one would rely on existing libraries such as [ZIO](https://zio.dev/) and [Cats Effect](https://typelevel.org/cats-effect/). These would provide you with all the basic machinery rto express the business logic along with providing you optimized interpreters and concurrency constructs as well.

Secondly, another common question is the number of allocations required by the description of a program. Indeed, this requires some allocations in order to be created, but this is irrelevant as the cost of instantiating a data-structure is negligible compared to how it is used at runtime. In other words, the performance of a program encoded like above mostly depend on how it is executed. The more optimized the interpreter, the more efficient is the program. From that perspective, libraries such as the ones mentioned earlier are usually comparable to existing solutions available today if not more efficient (Although it always depends on the use-case).

## Wrap-Up

As we've seen it, **Local Reasoning** is critical to leverage abstraction in a codebase. However it prevents a program from doing anything meaningful such as writing in a file or getting some data from the console, as these lead to perform **side-effects**. This can be mitigated by delaying the execution of **side-effects** until these are absolutely needed using a **declarative** or **executable** encoding. Finally we talked about **Composition** which ensures a model can always be composed and recomposed to introduce new business requirements easily or modify existing ones.

It's important to mention that overall this is not really about the paradigm used to design a program but more about these three fundamentals. Having said that, Functional Programming leads you naturally to adopt them through [Referential Transparency](https://en.wikipedia.org/wiki/Referential_transparency) and [Lazy Evaluation](https://en.wikipedia.org/wiki/Lazy_evaluation) among others. Unfortunately, Functional Programming is usually taught mostly using concepts that may be intimidating, but this does not have to be done like this. By keeping this small subset of concepts in mind, you can quickly ramp up and be productive.

## Where to go next?

The Functional Programming course provided by John DeGoes on [Patreon](https://www.patreon.com/jdegoes/posts) is a good place to start, and is where I've learnt a lot regarding the concepts described in this post.

Secondly, there is the [Zionomicon](https://www.zionomicon.com/) which is [ZIO](https://zio.dev/)'s bible, and [Essential Effect](https://essentialeffects.dev/) by Adam Rosien, which is more focused on [Cats Effect](https://typelevel.org/cats-effect/). The combination of these two can give you a good picture of how Functional Programming is today done in Scala. I would also recommend [Functional Programming with Scala](https://www.manning.com/books/functional-programming-in-scala) aka the "Red book" which is good but a bit rough for new comers (Please note that there has been a lot of innovation in this space so you might be looking at outdated content especially when it comes to certain later chapters).

Finally, [F# for fun and profit](https://fsharpforfunandprofit.com/) is also a good resource. It's not Scala but the concepts described are relevant to most of languages, and Scott Wlaschin (its author) is an incredible teacher, who worked among others on integrating [DDD using a Functional approach](https://pragprog.com/titles/swdddf/domain-modeling-made-functional/).

## Thanks

I'd like to thank [John De Goes](https://github.com/jdegoes), [Calvin Lee Fernandes](https://www.linkedin.com/in/calvin-lee-fernandes/) along with the Spartan community for their support, help and friendship, and of course, you for reading :)