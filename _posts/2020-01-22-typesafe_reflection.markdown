---
layout: post
title:  "Typesafe reflection"
date:   2020-01-22 08:02:48 -0500
brief: 
---

Following the [previous post](/2019/11/30/gadt.html), here is a nice technique to implement typesafe reflection using GADTs.

As you may know, Scala provides us with Phantom types. A Phantom type is parameterized type which the only purpose is to provide type-safety and which is erased at runtime:
```scala
/* A is only present as a type parameter but is not used anywhere else in the structure definition */
case class Foo[A](name: String)
```
This can be used to tag a type with some additional metadata used by the compiler to ensure type-safety:
```scala
object Foo  {
  def int(name: String)   : Foo[Int]     = Foo[Int](name)
  def string(name: String): Foo[String]  = Foo[String](name)
  def bool(name: String)  : Foo[Boolean] = Foo[Boolean](name)
}
```
The problem, however, is that a Phantom type's information gets lost when used in a pattern matching expression:
```scala
def run[A](foo: Foo[A]): (String, A) = {
  (foo.name, foo match {
    // Note that this A here will be lost because of type-erasure
    case Foo[A] => ??? // then what?
  })
}
```
We need a way to hold onto the phantom type and not have it erased at runtime. One way to achieve this is to use an additional type responsible for storing this information along with the implicit mechanism:
```scala
sealed trait Type[A]
object Type {
  implicit case object TInt     extends Type[Int]
  implicit case object TString  extends Type[String]
  implicit case object TBoolean extends Type[Boolean]
}
```
We can then retrieve this information using an implicit `Type`:
```scala
import Type._

def run[A](foo: Foo[A])(implicit t: Type[A]): (String, A) = {
  (foo.name, t match {
    case TInt    => 42
    case TString => "is the meaning of life"
    case Boolean => true
  })
}
```
Note how the compiler can trace the information conveyed by the Phantom type `A`. If the type is a `TInt`, then the compiler expects `A` to be an `Int`. If it is a `TString`, then a `String` is expected, and so on. When using this technique, make sure to seal the trait otherwise the pattern matching won't be exhaustive, and the compiler won't be able to provide you any guarantee.

In this example, we explicitly passed an implicit argument (no pun intended), but we can do better and rewrite the `run` function using the `implicitly` function along with a context-bound:
```scala
def run[A: Type](foo: Foo[A]): (String, A) = {
  val t: Type = implicitly[Type[A]]
  (foo.name, t match {
    // ...
  })
}
```
or even better, tag an existing type with additional information:
```scala
case class Foo[A: Type](name: String) {
  val fooType: A = implicitly[Type[A]]
}
```
This technique can be really useful whenever you want to ensure type-safety with a Phantom type while holding onto the information it provides.