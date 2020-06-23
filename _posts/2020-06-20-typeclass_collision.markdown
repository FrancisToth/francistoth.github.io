---
layout: post
title:  "Typeclass collision"
date:   2020-06-20 08:02:48 -0500
brief: 
---

We explained previously how typeclasses can be implemented in Scala. In this post, we will tackle how to organize typeclasses....prevent collision...

## The Problem

Given three typeclasses, how can establish a relationship,...anything that is bounded can be enumerated...
```scala
// We assume there is a companion object for Enumerate providing a sumoner
trait Enumerate[A] {
  def enumerate: Iterable[A]
}
trait Bounded[A] {
  def min: A
  def max: A
}
trait Indexed[A] {
  def indexOf(a: A): Int
  def get(idx: Int): Option[A]
}
```

```scala
// won't compile as there is no relationship between Enumerate and Bounded nor Indexed
// could not find implicit value for evidence parameter of type Enumerable[A]
def doSomething[A: Bounded: Indexed]: Enumerate[A] = Enumerate[A]
```

## Typeclass derivation

We can use derivation...
```scala
object Enumerate {
  // ...
  implicit def `Bounded ~> Enumerate`[A: Bounded]: Enumerate[A] = ???
  implicit def `Indexed ~> Enumerate`[A: Bounded]: Enumerate[A] = ???
}
// ...
def doSomething[A: Bounded: Indexed]: Enumerate[A] = Enumerate[A]
```
The problem however triggers an ambiguous implicit error as Scala does not know which one to choose:
```console
ambiguous implicit values:
 both method Bounded ~> Enumerate in object Enumerable of type [A](implicit evidence$2: App.this.Bounded[A])App.this.Enumerable[A]
 and method Indexed ~> Enumerate in object Enumerable of type [A](implicit evidence$3: App.this.Bounded[A])App.this.Enumerable[A]
 match expected type App.this.Enumerable[A]
```

## Typeclass coherency  


## Solution 1: Prioritization / Scato Encoding

We can solve this using implicit priorization:
```scala
object Enumerable extends EnumerableImplicits0 {
  def apply[A: Enumerable]: Enumerable[A] = implicitly[Enumerable[A]]
}

trait EnumerableImplicits1 {
  implicit def `Bounded ~> Enumerate`[A: Bounded]: Enumerable[A] = ???
}

trait EnumerableImplicits0 extends EnumerableImplicits1 {
  implicit def `Indexed ~> Enumerate`[A: Indexed]: Enumerable[A] = ???
}
```
Solution is ok as this is part of the internals of an API. Not seen from the user application code.
Limitations:
- Some boilerplate for the author
- Could grow and become cumbersome if there are many typeclasses (not the case)
- Some runtime overhead
- inability to say we want a single instance that is the accumulation of capabilities across multiple typeclasses
we'd like to say there exist some structure which is a composition of multiple typeclasses. 
```scala
type All[A] = Bounded[A] with Indexed[A]
```
but this won't work in context bounding
- secondly it is not very modular

- different typeclasses, multiple ways of deriving classes that are higher up in the hierarchy, 

https://www.adelbertc.com/publications/typeclasses-scala17.pdf
- One concern is itssyntactic overhead
- Another concern in Scato is the performance cost of usingimplicit conversions instead of subtyping. Each superclassresolution through a subclass invokes a function call to con-vert the latter to the former, whereas with subtypes no suchcall is needed. Under the right conditions some of thesefunction calls could be inlined either statically with an anno-tation or by the JIT compiler
- Approach with Dotty (39:56)

## Solution 2: Inheritance / Standard Instance (39:56)
- natural approach
- implement all typeclasses instances for standard type, all regrouped in a single class that extends all of typeclasses
- fullset of capabilities
```scala
object StdInstances {
  implicit val IntInstance: Bounded[Int] with Indexed[Int] = ???
}

object Indexed {
  implicit val IntInstance: Indexed[Int] = StdInstanced.IntInstance
}
```
Inject the instance in each companion object
For the user-defined typeclass:
```scala
object Person {
  implicit val PersonInstance: Hash[Person] with Ord[Person] with ... 
}
```
- user already do this
- Instead of using multiple context bounds, we use a single one:
```scala
type IndexedBounded[A] = Indexed[A] with Bounded[A]
def example[A: IndexedBounded](): Unit = ???
// show the desugarized version. THere are not the same
```
one instance that is both indexed and bounded VS two instances (potentially) that individually provided with indexed and bounded

their overlap share the same coherent instance, brings back coherency, no conflicting definition of core 
can list constraint a single time

In Scala3, we can define an operator With
```scala
type With[F[_], G[_]] = [A] =>> F[A] with G[A]
def example[A: Indexed With Bounded]()
```
