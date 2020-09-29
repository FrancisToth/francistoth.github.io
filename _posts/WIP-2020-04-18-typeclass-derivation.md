---
layout: post
title:  "Typeclass derivation"
date:   2020-04-19 08:02:48 -0500
brief: 
---

In a previous post, we covered how typeclasses could be implemented in Scala, and then talked about how two implementations (aka instances in the FP jargon) for the same data type could live in the same implicit scope without causing an "ambiguous implicit values" error. In this post, we will tackle the problem of ~~typeclass derivation~~ coherency.

## Typeclass Derivation
```scala
implicit def `Ord ~> Equal`[A: Ord]: Equal[A] =
  (a0, a1) => Ord[A].compare(a0, a1) eq Ordering.Equals
``

```scala
trait Enumerate[A] {
  def enumerate: Iterable[A]
}
object Enumerate {
  implicit def `Bounded ~> Enumerate`[A: Bounded]: Enumerate[A] = ???
  implicit def `Indexed ~> Enumerate`[A: Bounded]: Enumerate[A] = ???
}
trait Bounded[A] {
  def min: A
  def max: A
}
trait Indexed[A] {
  def indexOf(a: A): Int
  def get(idx: Int): Option[A]
}

def doSomething[A: Bounded: Indexed]: Enumerate[A] = Enumerate[A]
```
We can use implicit priorities
```scala
trait EnumerateImplicits1 {
  implicit def `Bounded ~> Enumerate`[A: Bounded]: Enumerate[A] = ???
}

trait EnumerateImplicits0 extends EnumerateImplicits1 {
  implicit def `Indexed ~> Enumerate`[A: Indexed]: Enumerate[A] = ???
}

implicit val boundInt: Bounded[Int] = ???
// derivation
Enumerate[Int].enumerate
//def doSomething[A: Bounded: Indexed]: Enumerate[A] = Enumerate[A]
```
```scala
// if derivation are in the same Enumerate object, this will trigger an error (what if we maintain the trait???). That's why priorities are required
implicit val boundInt  : Bounded[Int] = ???
implicit val indexedInt: Indexed[Int] = ???
// derivation
Enumerate[Int].enumerate
//def doSomething[A: Bounded: Indexed]: Enumerate[A] = Enumerate[A]
```
Solution is ok as this is part of the internals of an API. Not seen from the user application code.

## Relationships between typeclasses
- Ideally we'd like to describe the combination of multiple capabilities using a single type.
- implicit priorities 
- scato encoding
- Using subtyping instead of derivation and encode standard instances (no import but mega class for each data type).
- We use a single context bound instead a of multiple one

## Limitations
- Check the stdinstance trait
- mixin the package object, BUT
- Everything is one trait (stdinstance) to make them accessible
- Usually we use the companion object (of the datatype or the typeclass) to put the implicit instance
- Putting everything in stdinstance, makes them outside the implicit scope
- We could mix the trait in the package object which may be a problem in Scala 3. Users will have to include zio.prelude._
- That's cool, but this may create collision with what the user uses.
- This also may slow down the compilation
- In some cases wildcards are specifically disallowed
- Scala 3 will not bring in the implicits when mixing the traits in a package
=> Additional burden
- Other problem related to derivation
- a datatype must have all the capabilities to be derived
list[A: OrdWithHash] requires A to have both capabilities to derive OrdWithHash[List[A]]
what if you only have one of those capabilities?
We could create an object for std and implement implicits in each typeclass which delegate the work to std
this brings back std into implicit scope. But this brings the collision problem back
- we could narrow the type so that the composite is only found in one place but this is quiet painful
- it does not help with derivation. We want to derive the most powerful thing from the least powerful thing

Solution:
- we go back to define instances in the ocmpanion object
- what about coherency now? We should create a derivation for composition that make sense
```scala
object Coherency3 {
  trait Bounded[A]
  trait Indexed[A]

  // mix problem? combinatorial explosion?
  // we derive a more powerful from a least powerful
  implicit def boundedIndexed[A: Indexed: Bounded]: Indexed[A] with Bounded[A] = ???

  def foo(ls: List[Bounded with Indexed]): Unit = ???

  // needs to generate all combinations
  // In fact there is no solution for coherency, but this one is not too bad
  // 
}

// question: what should be the constraint we put on A?
// If you want to provide the biggest typclass as a result, you will to require the same set of capabilities on A
// If you want to derive the whole kitchen sink from the whole kitchen sink that's fine
// In some cases you don't have it, only a piece of it, and still would like to derive a List (ex: List[A: Hash] -> Hash[List[A]])
def list[A: OrdWithHash]: OrdWithHash[List[A]] = new Ord[List[A] with Hash[List[A]]] {
    // ...
}

```
Solution:
- turn stdinstances into an object
- go to each typeclass, implement delegator (implicit closureInt: Closure[Int] = STDInstance.Int)
- this brings back STDInstances back in the implicit scope however, collisions may happen again!
- solution is based on typing these implicitly and make sure we don't duplicate the instances (associative, composite etc...) but does require a lot of work. Does not help with derivation (most powerful thing from the least one)
- Solution: define instances in companion objects just like before. What about coherency problem? Create a derivation for composite that makes sense:
```scala
implicit def hashOrd[A: Hash: Ord]: Ord[A] with Hash[A] = ???
// If you have Hash and Ord, even if they are independent, I can derive one that is capable of deriving it from both.
// This brings back coherence, and absence of collision (which is solved with intersection types).
// library author pain for the user
```
for any combination that makes sense, in terms of the individual components parts, we derive the coherent solution ("The mix problem"). There could be a combinatorial explosion but it could be kept small.

RECAP OF THE PROBLEM

```scala
def listInstance[A: Associative : Commutative]()
```
translates to
```scala
def listInstance[A](implicit 
  a: Associative[A],
  c: Commutative[A]
) = Closure[A].combine(a)
```
The problem is what happens when there is an implementation of `Closure` for each of these guys. Because `a` and `c` may point to two different instances, they may not implement `combine` the same way, leading to a lack of coherence.

In order to solve this, we can rely on trait prioritization, or model the typeclasses differently, like for example using sub-typing.

This would enables things such asÈ
```scala
def listInstance[A: Associative with Commutative]()
```
which basically says there is only one instance of this type satisfying the laws we need. It works well with monomorphic type but this becomes quiet tricky with polymorphic types however.

```scala
def listInstance[A: Associative with Commutative](): Associative[List[A]] with Commutative[List[A]]
```
What if `A` is only Associative and Commutative? This leads to a huger number of derivations.
Coherency: single type, and derive the intersection types from individual components (see last session). This pushes you to use sub-typing to model the typeclass relationships. Also allows us that for standard types, the implicits are in the data type's companion object. Brings coherency using single types.

derive the intersection types from individual components require many derivations. We therefore need to minimize the number of typeclasses.


## Higher kinded generics