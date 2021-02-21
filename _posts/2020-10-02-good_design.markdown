---
layout: post
title:  A Good Design
date:   2020-10-02 08:51:48 -0500
brief:
---

In the [last post](https://francistoth.github.io/2020/09/22/composition.html), we have illustrated different patterns one can use to write composable software. Having practical knowledge of how composition works is important, understanding its fundamentals even more. In this post we'll take some steps back and describe these. As we'll see it, these fundamentals remain relevant no matter the paradigm chosen to address a particular problem.

Why another post about coding best practices? There are already indeed plenty of article about these covering topics such as the [SOLID principles](https://en.wikipedia.org/wiki/SOLID) or the [GOF patterns](https://en.wikipedia.org/wiki/Design_Patterns). However these can be intimidating and confusing mostly because of their number and sometime their contradictions. The problem is that just like any other principles, these are subject to interpretation and sometime zealous application. In fact, fully understanding their essence requires a lot experience and hours and hours of practice.

In this post we'd like to provide the three main fundamental concepts one should always keep in mind when writing maintainable business software no matter the paradigm. As we'll see it, all the principles you may have heard about align in some way with these.

## Recognizing the problem

No matter the project we work on, it's critical to recognize that the bigger the system, the harder it is to reason about it. Picturing the state of a software becomes at some point impossible, and this may lead a codebase to a point where no body wants to change it because of the lack of guarantees regarding what may happen at runtime. How could we get maximum insurance that things will go well once the software has been deployed in production, provided that it interacts with potentially unstable endpoints such as a file-system or a remove server?

No matter how hard we try, there are aspects we won't ever have control over. This is an important fact of life, don't bang your head against the wall regarding things you cannot change anyway. When it comes to software, we can guarantee a significant level of quality provided that some constraints are satisfied. If you get a server with very few disk space, there is not much you can do if the software archive requires more. On the other hand, if deployment expectations are met, you can guarantee that a function will always behave as expected to a certain extent. Let's see how to maximize these guarantees as far as software is concerned.

## Bringing sanity back

Coding is like having a civilized and gentle conversation with the future maintainer of the code (who may be yourself). It is about showing your ability to express concepts in a simple and intelligible way. Properly conveying ideas requires these to be organized and structured, so that each of these can be understood separately. If the reader needs to look into how a function is implemented to understand its purpose, what it does, what it requires and what it outputs, this is a clear sign that something went wrong in the design process. Let's take an example:

```scala
def incByOne(i: Int): Int = 
  i + 1

val a = incByOne(incByOne(1))
val b = incByOne(2)
val c = 3
```
`incByOne` does exactly what it claims. It takes an `Int` and increments it. The reader does not even need to look at how this is all done, and can solely rely on the function's signature. The beauty of an expression such as `incByOne(2)` is that it can be replaced by the value it produces (`3`) without changing the program's output in any way. More over, expressions such as `incByOne(2)`, `3` or `incByOne(incByOne(1))` are all equivalent, which means that refactoring can be safely done as long as we always replace an expression by one of its  equivalent. This is referred to as the [**Substitution model**](https://en.wikipedia.org/wiki/Substitution_model). It provides the ability to substitute an expression by the value it produces without changing the program's behavior.

Aside from refactoring capabilities, the **Substitution model** also provides with a very interesting property called **Local Reasoning**. It let the user reason about a function only in terms of its signature. This is a key principle in software design as it lets us abstract over a component without looking at how it's implemented.

A good analogy for this is language. In common language, one does not need to explain how a car works every time one needs to be mentioned. The word "car" conveys a set of underlying ideas about its nature which lets us combine it with other words to express more complex concepts (eg. a sport car). In other words, **Local Reasoning** is the fundamental principle of **abstraction**. The question then is when does **Local Reasoning** breaks?

```scala
def foo(number: String): Int = 
  number.toInt

def bar(input: String): Int = { 
  println("running bar")
  number.toInt
}
```
Let's think about the inputs and outputs of these two functions. Just like `incByOne`, `foo` describes a function having one input (a `String`) and one output (an `Int`). `bar` on the other hand takes one input (a `String`), and returns one output (an `Int`) but also outputs another `String` in the console which is not mentioned anywhere in `bar`'s signature. This makes `bar` impossible to reason about if only considering its signature. What about the **Substitution model**?

```scala
val a = bar("42")
val prog1 = (a, a)         
/* 
 * output1: (42, 42)
 * output2: "running bar" is printed in the console
 */
val prog2 = (bar(), bar())
/* 
 * output1: (42, 42)
 * output2: "running bar" is printed twice in the console
 */
```
Because of `bar`'s nature, it's impossible to substitute `bar("42")` by `42` without affecting the program's behavior. The **Substitution Model** is therefore broken and so is **Local Reasoning**. As a consequence, it's impossible to abstract over `bar` and combine it with other components without mentally picturing the program's state at runtime. `bar` is rather simple but think about a component relying on many functions such as `bar`. At some point, it'll become impossible to say what this component exactly requires and/or outputs. In this example, we focused on how hidden outputs can break **Local Reasoning**, but this also happens whenever a function requires a hidden input:
```scala
val logHeader = "Debug: "

def bar(input: String): Int = { 
  println(logHeader + "running bar")
  number.toInt
}
```
More generally, **Local Reasoning** is broken whenever a function requires something that is not part of its argument list, and/or it outputs somethings that is not captured by its returned type. Any of these situations lead to what is commonly called a **side-effect**.

## Local Reasoning VS Side-Effect

No matter the program you write, **side-effects** will always be required whether to get some data from the user or to store some in a database or else. So the question is how to reconcile **side-effects** with **Local Reasoning**. The answer to this question relies on another fundamental principle: **The separation of the _what_ and the _how_**.
```scala
def program(): Unit = {
  val name = readLine()
  println("Hi " + name + "!")
}
```
This program contains two side-effects. `readLine()` performs a read from the console while `println()` outputs a string on it. The problem with these instructions is that it's impossible to reflect what they may do once run in `program()`'s signature. This is due to their nature, both being **statements**. **Statements** are instructions being called only to run their side-effects. In contrast with values, we cannot substitute an expression being a **statement** by the value it produces. So one approach is to model these using simple values:
```scala
case class IO[A](run: () => A) {
  def andThen[B](f: A => IO[B]): IO[B] =
    IO(f(value))
}
```
`IO` represents a lazy instruction which one executed produces an `A`. This is captured by `run`. The way this `A` is produced may required performing a side-effect. This is not ideal, but this brings us forward as now we can express the program described above like this:
```scala
def putStrLn(s: String): IO[Unit]   = IO(println(s))
def getStrLn           : IO[String] = IO(readLine)

// Declaration of the program (the `what`)
val program: IO[Unit] =
  getStrLn.andThen(name =>
    putStrLn("Hi " + name + "!")
  )

// Execution of the program (the `how`)
program.run()
```
Whenever `run` is called, the program performs any side-effect it may require to produced the final value. Note we use `andThen` to sequence one `IO` with another one. This encoding led to a complete separation of a program's  description (the _what_) and its execution (the _how_). Secondly, `program` can now be reasoned about locally and leverage the **Substitution model**:
```scala
val p1: IO[String] = getStrLn 

p1.run()
// is equivalent to
getStrLn.run()
```
There is one problem though. Knowing that `p1` is an `IO[String]` does not really tell us what will happen once `run` is called. We only know that doing so will perform a side-effect resulting amon others in a `String`. This has important consequences in terms of testing, as in the end of the day, it's impossible to know what an `IO[String]` will result in without running it:
```scala
// Using scala-test
"StrLn" should "read an input from the console" in {
  val actual: IO[String] = getStrLn
  actual should be getStrLn // ???
}
```
The problem is that a function, which is the type of `run`, cannot be inspected just like any other common value. `IO` is too vague and sounds a bit like cheating. Let's take another approach and model our domain properly:
```scala
// We keep `Console` invariant in A for the sake of simplicity
sealed trait Console[A] { self =>
  def andThen[B](f: A => Console[B]): Console[B] =
   Console.AndThen(self, f)
}
object Console {
  case class PutStrLn(s: String) extends Console[Unit]
  case object GetStrLn           extends Console[String]

  case class AndThen[A, B](
    console: Console[A], 
    f: A => Console[B]
  ) extends Console[B]

  def putStrLn(s: String): Console[Unit]   = PutStrLn(s)
  def getStrLn           : Console[String] = GetStrLn
}

val program: Console[Unit] =
  getStrLn.andThen(name =>
    putStrLn("Hi " + name + "!")
  )
```
`program` is now a data-structure chaining all the necessary steps to perform what needs to be done. This encoding (referred to as **declarative encoding**) provides additional benefits as described in the [previous post](https://francistoth.github.io/2020/09/22/composition.html). From now on, we can inspect a `Console[_]` and ensure it is composed of the right sequence (just like a `List[_]`):
```scala
// Using scala-test
"StrLn" should "read an input from the console" in {
  val actual: Console[String] = getStrLn
  actual should be GetStrLn
}
```
What about executing the program? For that purpose we can write an interpreter, as described [here](https://francistoth.github.io/2020/09/22/composition.html):
```scala
def run[A](console: Console[A]): A = console match {
  case PutStrLn(string) => println(string)
  case GetStrLn         => readLine()
  case AndThen(c, f)    => f(run(c))
}
```
`run` goes through each layer of the program and perform any side-effect required to execute it. Note that `run` is not stack-safe and would blow up with infinite recursive programs (optimizing this will be the topic of another post).

So how do we reconcile **side-effects** with **Local Reasoning**? By keeping them separated. We ensure that our domain model stays side-effect free and locally reasonable until we get to the point it needs to be interpreted. From that point, we loose all the benefits of the **Substitution model** and **Local Reasoning**. This tells us something about the architecture of an application following these principles. As it's impossible to keep code sanity beyond its execution, **side-effects** should be performed at the edges of our architecture. The core of the application remains easy to maintain while its interpretation is up to whatever uses it. This is the principle described in the [Onion](https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/) and the [Hexagonal architecture](https://en.wikipedia.org/wiki/Hexagonal_architecture_(software)). Secondly, because it is agnostic of how it is executed, the same business logic can be used in different contexts and is up to multiple interpretation. We could for example write a test-specific interpreter, a prod-specific interpreter or even one producing an optimized version of the initial data-structure to maximize performances at runtime.

## To the infinity and beyond

**Local Reasoning** combined with **a clean separation between the declaration and the execution** of a program enables you to introduce another very interesting property in a software: **Composition**. **Local Reasoning** Combining two components that can be locally reasoned about goes back indeed to combine their respective requirements and outputs:
```scala
val x: Console[Unit] = 
  getStrLn.andThen(name => 
    putStrLn("Hi " + name + "!")
  )
```
The nature of the combination depends on the operator being used. `andThen` for example combines two `Console` in a sequential way and discards any intermediate result (`String` provided by `getStrLn`) to only keep the final one (`Unit` provided by `putStrLn`).

Another way to combine two `Console` could be `zip` and `zipWith`:
```scala
sealed trait Console[A] { self =>
  // ...
  def zip[B](that: Console[B]): Console[(A, B)] = 
    zipWith(self, that)((_, _))

  def zipWith[B, C](that: Console[B])(f: (A, B) => C): Console[C] =
    Console.ZipWith(self, that, f)
}
object Console {
  // ...
  case class ZipWith[A, B, C](
    left: Console[A], right: Console[A], 
    f: (A, B) => C
  ) extends Console[C]
}
// ...
val parRead: Console[(String, String)] = 
  getStrLn.zip(getStrLn)
```
The power of **Local Reasoning** in regards of **Composition** is that we can reason about the result of a combination without even knowing what it is composed of. This is pure **abstraction**!

We covered different composition patterns in the [last post](https://francistoth.github.io/2020/09/22/composition.html) and won't reiterate but here is a brief digest.

Overall composition is concretely built upon three main concepts:
- **primitives** : which model simple solutions (`PutStrLn`, `GetStrLn`)
- **constructors** : which build solutions (`putStrLn`, `getStrLn`)
- **operators** : which transform/combine solutions into other solutions (`AndThen`, `ZipWith`)

As explained [here](https://degoes.net/articles/functional-design) and [here](https://medium.com/bigdatarepublic/writing-functional-dsls-for-business-domains-1bccc5d3f62b), **primitives** should be modeled such that they cover the complete solution space and avoid any overlap in their capabilities. If properly done, their number should end up being minimal (as much they can be). 

When it comes to encode these terms, two encodings can be used:
- **declarative encoding**:  where _every **constructor** and **operator** of the model is expressed as pure data in a recursive tree structure_.
- **executive encoding**: where _every constructor and operators of the model is expressed in terms of its execution_.

As explained in the [last post](https://francistoth.github.io/2020/09/22/composition.html), each of these has pros and cons, but the **declarative encoding** should be favored whenever possible (especially in greenfield projects).

## Wrap Up

In this post we covered three fundamental principles one can use to write robust and maintainable software:
- **Local Reasoning**: To make components easy to reason about and to abstract over
- **Separation of the _what_ and the _how_**: To keep side-effects management at the edges of the architecture and keep the core reusable in different contexts (test, prod or else).
- **Composition**: To ensure that a model can always be separated and recombined to change or build solutions for problems it was not necessarily designed for in the first place.

As you can see, this is not really about the paradigm but more about these three fundamentals. However, it is true that Functional Programming leads you naturally to adopt them through [Referential Transparency](https://en.wikipedia.org/wiki/Referential_transparency) and [Lazy Evaluation](https://en.wikipedia.org/wiki/Lazy_evaluation) among others. So one advice from a programmer to another, don't get intimidated by some of the obscure aspects of Functional Programming and keep these three principles in mind instead :)

Thank you for reading.

Note: The content of this blog post will be presented at the following conferences:
- CodeMesh
- Meetup

## References
- [What is an Effect?](https://www.inner-product.com/posts/what-is-an-effect/) - Adam Rosien
- [A Beginner-Friendly Tour through Functional Programming in Scala](https://degoes.net/articles/easy-monads) - John DeGoes

## Workshops
- [The Art of Functional Design](https://www.eventbrite.com/e/functional-design-by-john-de-goes-tickets-105437751152) - John DeGoes
- [Domain Modeling Made Functional Online Workshop](https://www.eventbrite.it/e/domain-modeling-made-functional-online-workshop-tickets-108100012046?aff=erelexpmlt) - Scott Wlaschin
