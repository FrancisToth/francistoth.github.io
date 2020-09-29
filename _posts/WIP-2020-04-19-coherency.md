---
layout: post
title:  "Coherence"
date:   2020-04-19 08:02:48 -0500
brief: 
---

In a previous post, we covered how type classes could be implemented in Scala, and then talked about how two implementations (aka instances in the FP jargon) for the same data type could live in the same implicit scope without causing an "ambiguous implicit values" error. In this post, we will tackle the problem of typeclass coherence.

According [wikipedia](https://en.wikipedia.org/wiki/Type_class), type classes _enforce the so-called coherence property, which requires that there should only be one unique choice of instance for any given type._ In other words, no matter where a typeclass is used, it will always have the same behavior. This is enforced in Haskell but not in Scala, du to how they are traditionally encoded:
```scala
trait Associative[A] {
  def combine(a0: A, a1: A): A
}
object Associative { /* ... */ }

object AssociativeImplicits1 {
  implicit val intAssociative: Associative[Int] = _ + _
}

object AssociativeImplicits2 {
  implicit val intAssociative: Associative[Int] = _ * _
}
```
Now given the following line:
```scala
Associative[Int].combine(4, 5)
```
Nothing guarantees that the _sum_ or the _product_ implementation will be used. It all depends on the context:
```scala
import Associative2._
Associative[Int].combine(4, 5) // 20
```
```scala
import Associative1._
Associative[Int].combine(4, 5) // 9
```
So given the traditional encoding, it requires some discipline in order to prevent lack of coherence. Ideally, we would like to avoid relying on the context where a typeclass is used, and keep the behavior deterministic. There is another issue however:

In order to achieve this, we must ensure that no implicit import is required to give access to the implicit typeclass instance.


In other words, we will rely on some objec



It depends on which object (`Associative` and `Associative2`) is present in the context. If `Associative`

 In contrast, type classes 

Let's start with an example:
```scala
// parent typeclass
trait Enumerable[A] {
  def enumerate: Iterable[A]
}
object Enumerable { /* Enumerable typeclass machinery */ }

// child typeclass 1
trait Bounded[A] {
  def min: A
  def max: A
}
object Bounded { /* Bounded typeclass machinery */ }

// child typeclass 2
trait Indexed[A] {
  def indexOf(a: A): Int
  def get(idx: Int): Option[A]
}
object Indexed { /* Indexed typeclass machinery */ }

// ...
def needsEnumerable[A: Enumerable](): Unit = ???
def example[A: Indexed: Bounded](): Unit   = needsEnumerable[A]()
//                                                              ^
// could not find implicit value for evidence parameter of type 
// io.github.francistoth.coherency.Coherency.Enumerable[A]
```






In a previous post, we talked about how type classes could be implemented in Scala but did not really talk about the limitations of this approach. This encoding comes directly from Haskell, and is the one used by most of Functional libraries like Cats.

## Recap
As a refresher, here is how a `Functor` is encoded using this approach:
```scala
// type class
trait Functor[F[_]] {
  def map[A, B](fa: F[A])(f: A => B): F[B]
}
object Functor {
  // summoner
  def apply[F[_]: Functor] = implicitly[Functor[F]]

  trait Export {
    // Ops class
    implicit class FunctorOps[F[_]: Functor, A](fa: F[A]) {
      def map[B](f: A => B): F[B] =
        Functor[F].map(fa)(f)
    }
  }
}
```
Then we can create a data type for which we need a functor to be available:
```scala
// This data type is the direct equivalent of Option[A]
sealed trait Maybe[+A] { self =>
  def fold[B](ifPresent: A => B, fallback: B): B =
    self match {
      case Maybe.Just(a) => ifPresent(a)
      case Maybe.Empty   => fallback
    }
}
object Maybe {
  def apply[A](a: A): Maybe[A] =
    if (a == null) Empty else Just(a)

  case class Just[A](a: A) extends Maybe[A]
  case object Empty        extends Maybe[Nothing]

  // instance example
  implicit val maybeFunctor: Functor[Maybe] = new Functor[Maybe] {
    override def map[A, B](fa: Maybe[A])(f: A => B): Maybe[B] =
      fa match {
        case Maybe.Just(a) => Maybe.Just(f(a))
        case Maybe.Empty   => Maybe.Empty
      }
  }
}
```
The instance could be in the companion object of the type class itself or of the data type. Both will be checked at compile time by Scala in order to satisfy the requirements of the following snippet:
```scala
object Main extends App with Functor.Export {
  val none: Maybe[Int] = Maybe.Empty
  val some: Maybe[Int] = Maybe(42)

  println(none.map(_ + 1).fold(identity, 0)) // 0
  println(some.map(_ + 1).fold(identity, 0)) // 43
}
```

## Limitations

This approach works very well for Haskell, but has some limitations when ported to Scala. The main issue is the lack of coherency leading sometime to errors related to ambiguous implicits. As explained [here](https://francistoth.github.io/2020/04/11/typeclasses.html), and [here](https://francistoth.github.io/2020/04/11/newtypes.html), if multiple typeclasses for the same data type happen to be in the same implicit scope, Scala may end up with "amibguous"

while resolving an implicit argument, Scala ends up with multiple implicit for the same 





