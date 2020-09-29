



- Model Equal and Order
- Hierarchy?
  * implicits work accross type hierarchy: You can pick the most powerful typeclass, and use it as it was any of the weakest ones. Nice way to use sub-typing to express relationships between abstractions. (BTW show how implicits are resolved when subtyping typeclasses)

  * Drawbacks: diamond problem with typeclasses sharing the same hierarchy

- Derivation
```scala
  implicit def OrdEqual[A: Ord]: Equal[A] = 
    (a0, a1) => Ord[A].compare(a0, a1) eq Ordering.Equals
```
- **Problem**: Multiple ways to derive a typeclass requires implicit priorization. This involves creates traits, and the further you get into the hierarcy, the lower is the priority
- requires a single location where those derivations are done

implicits: Discoveribality, no import (companion object of the typeclass or the data type)

patterns of composition. A function composes its inputs into an output
```scala
def f[A](a0: A, a1: A): A = ???
```
```scala
// Also called a Magma
trait Closure[A] { // Totality (difference with a SemiGroup?)
  def combine(a0: A, a1: A): A

  // Laws
  final def closureLaw(a0: A, a1: A): Boolean =
    try { combine(a0, a1)); true } catch { case _: Throwable => false }
}
```

Product-composition (applicative)
```scala
(Equal[A], Equal[B]) => Equal[(A, B)]
```

Sum-composition
```scala
(Equal[A], Equal[B]) => Equal[Either[A, B]]
```
see contramap, both, either etc...
implicits priorization
we could use implicit chaining but then we cannot create one typeclass without the other

How to support multiple implementation of a typeclass for the same data-type?
new type


Talk Idea: code organization (check exports pattern)