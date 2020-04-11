package io.github.francistoth.newtype

object Main extends App {

  object Mult extends Newtype[Int]
  type Mult = Mult.WrappedType

  implicit val sum: Associative[Int] = _ + _
  implicit val product: Associative[Mult] =
    (a0, a1) => Mult(Mult.unwrap(a0) * Mult.unwrap(a1))

  def reduce[A: Associative](zero: A, as: List[A]): A =
    as.fold(zero)(Associative[A].combine)

  reduce(1, List(2, 3))                   // 5
  println(reduce(Mult(1), List(Mult(2), Mult(3)))) // 6

  val m0: Mult = Mult(42)
  // val i0: Int  = Mult(42) // would only compile if Mult is a Subtype[Int]

  Mult(42) match {
    case Mult(i) => i
  } // 42
}

trait Associative[A] {
  def combine(a0: A, a1: A): A
}
object Associative {
  def apply[A: Associative]: Associative[A] = implicitly[Associative[A]]
}

sealed trait NewtypeModule {
  def newtype[A]: Newtype[A]
  def subtype[A]: Subtype[A]

  sealed trait Newtype[A] {
    type WrappedType
    def apply(a: A): WrappedType
    def unwrap(wt: WrappedType): A
    def unapply(wt: WrappedType): Option[A] =
      Some(unwrap(wt))
  }

  sealed trait Subtype[A] extends Newtype[A] {
    type WrappedType <: A
  }
}

object NewtypeModule {
  val instance: NewtypeModule = new NewtypeModule {
    def newtype[A]: Newtype[A] = new Newtype[A] {
      type WrappedType = A
      def apply(a: A): WrappedType   = a
      def unwrap(wt: WrappedType): A = wt
    }

    def subtype[A]: Subtype[A] = new Subtype[A] {
      type WrappedType = A
      def apply(a: A): WrappedType   = a
      def unwrap(wt: WrappedType): A = wt
    }
  }
}

trait NewtypeModuleExports {
  import NewtypeModule._

  abstract class Newtype[A] extends instance.Newtype[A] {
    val newtype: instance.Newtype[A] = instance.newtype[A]
    type WrappedType = newtype.WrappedType
    def apply(a: A): WrappedType   = newtype(a)
    def unwrap(wt: WrappedType): A = newtype.unwrap(wt)
  }

  abstract class Subtype[A] extends instance.Subtype[A] {
    val subtype: instance.Subtype[A] = instance.subtype[A]
    type WrappedType = subtype.WrappedType
    def apply(a: A): WrappedType   = subtype(a)
    def unwrap(wt: WrappedType): A = subtype.unwrap(wt)
  }
}
