---
layout: post
title:  "Composition"
date:   2020-09-20 08:02:48 -0500
brief: 
---

**An efficient software design is one providing the ability for its components to be separated and recombined without introducing unexpected behaviors**. This topic has been tackled over and over in the past and a set of software design principles (such as the [SOLID principles](https://en.wikipedia.org/wiki/SOLID)) and best practices (like the [GOF patterns](https://en.wikipedia.org/wiki/Design_Patterns)) eventually came up. Despite their value, these tend to confuse many software developers however. Taken separately, they may indeed sound incomplete and often fail to convey what ties them all together. 

**The purpose of a good design is to ease the introduction of new business requirements or the modification of existing ones, while preserving certain properties**. Component reusability is paramount to achieve this but is not enough Additional guarantees regarding the resulting combination of two components must also be provided. In this post we'll tackle the different aspects which makes an API composable.

## Definition

Composition is first and foremost about combining smaller blocks into bigger ones. This suggests that a composable API provides primitive blocks and operators enabling their combinations into blocks of the same type. Let's look at an example:
```scala
// A Stream which once run, apply process on each element emitted by the stream
case class Stream[A, B](process: A => B) {
  def map[C](f: B => C): Stream[A, C] = 
    Stream(f.compose(process))
}


```



Let's look at an example:

```scala
trait UserDao { def save(user: User): Unit }

class UserService(val dao: UserDao) {
  def save(User user): Unit = {
    // Some validation logic
    dao.save(user)
  }
}
// ...
val service1: UserService = new UserService(dao1)
val service2: UserService = new UserService(dao2)

val user = User(42)
service1.save(user)
service2.save(user)
```
The `UserService` can be reused with different implementations of the `UserDao` trait but what can we guarantee about the resulting combination? Given `save`'s signature, we cannot guarantee much at compile time really. If `dao1`'s implementation happens to throw an exception or return a `null` when saving a `User`, this won't be captured until the program is run.
```scala
service1.save(user);
```
Except by looking at how `save` is implemented, there is actually no way for the reader to figure out how the program will behave once it is run. This issue happens to be critical in regards to our ability to compose and reason about the code. If you think about language in general, complex and abstract concepts are always the result of combining simpler ideas that are well understood. For example, we don't need to explain how a car works every time we want to talk about one. Instead, we build on top of that concept to compose more sophisticated ideas (eg. a car dealer). **This is the fundamental principle of abstraction**.

## Local Reasoning

If a function can be reasoned about through its signature only, it can be abstracted over and composed with other small blocks into bigger ones.


In other words, a function's signature should be transparent enough to convey how it behaves at runtime once called This concept is refer to as **Local Reasoning**. Concretely, it refers to the idea that the inputs and outputs of a function should be captured respectfully by its arguments list and returned type.
```scala
def foo(i: Int): String = 
  "Got " + i
```
In this example, `foo` has one input, the integer received in argument, and one output, the string being returned. What about the following function?
```scala
def bar(i: Int): String = {
  println("bar has been called!")
  "Got " + i
}
```
At runtime, `bar` takes one input (the integer received in argument) and produces two outputs, the string being returned **and** the one displayed on the console. However `bar`'s signature does not mention anything about the second output. This is referred to as a **side-effect**, that is an output which is not captured anywhere in a function's signature, and which is hidden from the reader. As a consequence, `bar` is header to reason about than `foo`. 

This is a very simple example but if `bar` happens to be a complex function relying on other functions to run, the number of possible outputs may be exponential, and reasoning about it locally, impossible. So **local reasoning** as opposed to **global reasoning** does not require a full picture of a program's internals to get an understanding of what's going on at runtime.

## From Statements to Values

There is one problem however. How can we capture the output produced by the `println` statement in `bar`? One first approach would be to change the `bar`'s signature like following:
```scala
def bar(i: Int): (String, Unit) = {
  val result = println("bar has been called!")
  ("Got " + i, result)
}
``` 
`Unit` does not tell much however. It's not descriptive enough to convey that a `String` is output on the console. We could also return the `String` being output:
```scala
def bar(i: Int): (String, String) = {
  val result = "bar has been called!"
  println(result)
  ("Got " + i, result)
}
``` 
but then how can we make the difference between a `String` output on the console and one that is simply returned? In fact the problem lies in the nature of the `println` statement. In contrast with values, statements are only declared to perform a side-effect, and cannot be returned by a function. To get this ability, statements have to be converted to values:
```scala
case class PutStrLn(value: String)
// ...
def bar(i: Int): (String, PutStrLn) =
  ("Got " + i, PutStrLn("bar has been called!"))
```
From now on, `bar`'s signature captures all its inputs and outputs. It describes a function which takes an `Int` and outputs a `String` along with another one printed out on the console. You'll surely noticed that we've changed the program's behavior however. `"bar has been called!"` is indeed no longer displayed on the console at runtime. To fix this, let's introduce another function responsible for performing any side effect required by our program:
```scala
// Description of the program (the 'what')
val (_, putStrLn) = bar(42) // the first output is discarded for now

// Execution of the program (the 'how')
def run(program: PutStrLn): Unit = 
  println(program.value)

// Run the program
run(putStrLn)
```
`run` is responsible for executing the side-effects described/represented by the value it takes in argument, and draws a line between the description of a program (the 'what') and its execution (the 'how').


The benefit of this approach may not look obvious at first but shines as soon as we get into more complex scenarios.

introduce composition through the example?

## 

```scala
sealed trait Stream[+A] { self =>
  import Stream._

  final def run: A = self match {
    case More(k) => k().run
    case Done(v) => v
  }
}

object Stream {
  case class More[+A](k: () => Stream[A]) extends Stream[A]
  case object Done                        extends Stream[Nothing]
}

object Example {

  
}
```



```scala
sealed trait Stream[+A] { self =>
  type Next

  def more(): Next
}
object Stream {
  case class Cons[A, T <: Stream[A]](head: A, tail: T) extends Stream[A] {
    type Next = (A, T)
    def more(): Next = (head, tail)
  }

  case object End extends Stream[Nothing] { self =>
    type Next = (Unit, End.type)
    def more(): Next = ((), self)
  }
}




  def more(): (A, Next[A]) = self match {
    case More(h, t) => (h, t)
    case More(h, End) => (h, End)
  }

```




Doing so provides us with an interesting property:




```scala
trait UserDao { 
  // Let's assume this operation is blocking for now
  def save(user: User): Result 
}

sealed trait Result
object Result {
  case object Saved extends Result
  case class Error(th: Throwable) extends Result
}
// ...
val result1 = service1.save(user),
val result2 = service2.save(user)

val user = User(42)
val results: Vector[Result] = Vector(result1, result2)
```
If we think about it, `results` could be refactored in different ways:
```scala
val results: Vector[Result] = Vector(service1.save(user), result2)
val results: Vector[Result] = Vector(result1, service2.save(user))
```


If you have been using functional programming for some time, you may know this concept as "Referential Transparency"



In other words, it's about composing small blocks into more sophisticated ones while preserving some guarantees.









## Modularity Vs Composition

- <span style="color:#88B8F7">**Modularity**</span> is about isolating modules with well-defined interfaces that can be used in a variety of contexts.
- <span style="color:#88B8F7">**Composition**</span> is the ability to combine two modules into one while providing guarantees about the resulting behavior




Effect vs Composition

Purity -> important, but the talk is really about composition. Statement as values / Data-Structures,

define effect system
This article however is not about interpreters nor effect systems, but about how these data-structures can be combined with each others.



- primitives
- operators
- constructors

encoding: initial/declarative or final/executive (interpreters are provided) + advantages vs inconvenient


examples: Loggers, Expr...
