---
layout: post
title:  "Typeclasses"
date:   2020-04-09 08:02:48 -0500
brief: 
---

Typeclasses are a very common topic when tackling Functional Programming and is what enables abstraction over similar (and possibly unrelated) data-structures.

## Traditional approach

In Object-Oriented Programming, this is usually achieved using an interface or an abstract class.
```scala
trait Json
trait JSonSerializable {
  def toJson: Json
}

def serialize[A <: JsonSerializable](a: A): Json = 
  a.toJson

class Person extends JSonSerializable {
  override def toJson: Json = ???
}
```
Despite being pretty simple to understand, this approach comes with several issues. First, it implies that `A`'s hierarchy can be modified which is not always possible. Secondly, if we have control over `A`, we'll have to bloat `Person` with some JSON specific code.

## On the road to typeclasses

Ideally, we would like to maintain a clean separation between our domain classes and what can be done with them. One approach would consist in creating a type responsible for JSON serialization:
```scala
trait JSonSerializer[A] {
  def toJson(a: A): Json
}
class Person
```
This trait could be then implemented for all the types we need to serialize:
```scala
val personJsonSerializer: JSonSerializer[Person] = ???

def serialize[A](a: A, js: JSonSerializer[A]): Json = 
  js.toJson(a)
```
This is much better as now the different concerns are properly separated. This approach is actually the one used by typeclasses. Scala does not provide typeclasses out of the box, but these can be emulated using implicits:
```scala
// Defined in JSonSerializer.scala
trait JSonSerializer[A] {
  def toJson(a: A): Json
}
object JSonSerializer {
  implicit val personJsonSerializer: JSonSerializer[Person] = ???  
}

// Defined in another file
def serialize[A](a: A)(implicit js: JSonSerializer[A]) =
  js.toJson(a)

serialize(new Person)
```
This works without doing any import thanks to how the compiler looks for implicits.

## Implicit lookup
As a recap, when it encounters a function call requiring an implicit argument, the compiler will look for an implicit definition (value or method) _**having the same type**_ than the missing argument. This lookup is performed in three different scopes, in the following order:
- _**Local definitions**_: any implicit definition present at the call-site
- _**imports**_: any implicit definition provided by an import
- _**companion objects**_: any implicit definition present in the companion objects of the types implied by the missing argument (in this case `Person` and `JSonSerializer`).

The first implicit definition satisfying the requirements will be used. In case of any ambiguity (that is whenever more than one candidate are found in the same scope), the compilation results in an error.

_**BEST PRACTICE**_: Implicit definitions should always be done in the companion objects. This prevents the user from performing any import.<br/>

## Final encoding

Back to our problem, let's get rid of some boilerplate using context bounding:
```scala
// this is syntactic sugar for
// def serialize[A](a: A)(implicit js: JSonSerializer[A])
def serialize[A: JSonSerializer](a: A) =
  implicitly[JSonSerializer[A]].toJson(a)
```
To improve this further, we could create a summoner:
```scala
object JSonSerializer {
  // summoner
  def apply[A: JSonSerializer]: JSonSerializer[A] =
    implicitly[JSonSerializer[A]]
  // ...
}

def serialize[A: JSonSerializer](a: A) =
  // Under the hood, this actually makes a call to apply
  // JSonSerializer.apply[A].toJson(a)
  JSonSerializer[A].toJson(a)
```
Finally, we could enrich any type `A` for which an implicit `JSonSerializer` is defined with a function `toJson`:
```scala
trait JsonSerializerSyntax {
  implicit class JsonSerializerOps[A: JSonSerializer](a: A) {
    def toJson: Json = JSonSerializer[A].toJson(a)
  }
}
```
This trait could be then mixed within a package object at the root of the project's hierarchy. This gives access to all the feature provided by the library without knowing anything about its internals:
```scala
// io.github
package object francistoth extends JsonSerializerSyntax
```
```scala
import io.github.francistoth._

(new Person).toJson
```
One question you might ask is how to define two implementations of the same typeclass for the same data type. We'll actually cover that in the next post so stay tuned. Meanwhile, you can find the code [here](https://github.com/FrancisToth/francistoth.github.io/blob/master/src/main/scala/io/github/francistoth).

Thanks to [Justin Heyes Jones](https://github.com/justinhj), [Calvin L. Fernandes](https://github.com/calvinlfer), and [Nader Ghanbari](https://github.com/naderghanbari) for helping me writing this post.
