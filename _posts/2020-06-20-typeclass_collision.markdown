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
## Problems

coherency
- always drawbacks
- sub typing: abstract over multiple class (with intersection types) but ambiguous implicit, hence STDInstances
- this fixes type classes with overlapping super-types
- problems: how to retrieve the standard instances implicits?
- 2 places for an implicit: companion object or datatype
- package object is ok but new difficulty: does not work with Scala 3

because `import prelude._` and wildcard imports is not always possible because of colusion, sometime people do not want that, in Scala 3.X, import packages do not import implicits

Secondly, this encoding generate a problem when deriving instances. For example:
```scala
implicit def list[A: OrdWithHash]: ...
```
The question is what should we us as a constraint here? biggest constraint possible but this is only ok if you want all capabilities. But in some cases, you don't have them and we still would like to derive a list given only a hash of a. For now this would not work as A requires it all.

One solution is to switch StdInstances to an object, and provide all the implicits to each companion objects, this would like using proxies. This leads us back to the collision issue. 

// Don't put this
Solution: narrow the type so that Scala find only in one place (pretty lot of work)
this does not solve the derivation issue.

Define all standards instances in each companion object, but coherency is biting again. Solution create a derivation for composite that makes sense:
```scala
implicit def hashOrd[A: Hash: Ord]: Ord[A] with Hash[A] = ???
```
We derive the coherent solution with individual pieces, combinatorial explosion possible, but it depends on the number of typeclasses.
For most common cases, we would have the standard instances, for conflicting situations, we would use intersections

To recap:
- spread the individual instances ian their respective companion object
- create a mean to derive intersection types from their terms in package object (all combinations that make sense)
- For the common no need to import everything, only for special cases.


## Recap
- Standard instances for each typeclass in their respective companion object
- coherency can be achieved with intersection types but maybe combinatorial explosion depending on the number of typeclasses
- may get problematic with higher kinded type like List:

```scala
def listInstance[A: Associative with Commutative]: Associative[List[A]] with Commutative[List[A]] 
```
What if A is only Associative? well we would like to derive an Associative of List, with the Associative of A. This turns out to be a huge number of combination multiplied by the number of polymorphic types.

We gonna still use sub-typing, but we will try to derive each individual instances from the intersection types:
```scala
trait AssociativeCoherent {
  implicit def associativeEqual[A: Associative: Equal]: Associative[A] with Equal[A] = 
    new Associative[A] with Equal[A] {
      def combine(l: => A, r: => A): A    = associative0.combine(l, r)
      def checkEqual(l: A, r: A): Boolean = equal0.equal(l, r)
    }
}
```
We can derive the composite given the individual components




reflexive law -> equal(a, a)
symmetry law -> equal(a2, a1) ==> equal(a1, a2)
transivity law -> equal(a1, a2)  && equal(a2, a3) ==> equal(a1, a3)
anti-symmetry -> (a1 <= a2) && (a2 <= a1) ==> (a1 == a2)

Group provides inverse to invert an A to its invert
a |+| inverse(a) === empty
inverse(a) |+| a === empty

—

Closure: (A, A) => A
- (A, A) => Boolean : return true, no exception

Associativity: (A, A) => A
- combine(a, (a, a)) == combine((a, a), a)

Commutative (A, A) => A
- combine(a1, a0) == combine(a0, a1)

Identity is associative, and comes left and right identity (provides combine)

blog about session 5
blog about laws (session 5)
blog about scato encoding and all (session 5)

Equal:
reflexive law
symmetry law
transivity law

a <-> b (a can be substituted by b and vice versa)

## Inverse
```scala
trait Inverse[A] extends Commutative[A] with Identity[A] {
  def inverse(a: A): A
}
// right inverse law
I.combine(a, I.inverse(a)) <-> I.identity
```

## Classic
```scala
type SemiGroup[A] = Associative[A] // with Closure[A]
type CommutativeSemiGroup[A] = SemiGroup[A] with Commutative[A]
type Monoid[A] = SemiGroup[A] with Identity[A]
type CommutativeMonoid[A] = Monoid[A] with Commutative[A]


```