package io.github.francistoth.coherency

/*object Main extends App with Functor.Export {
  val none: Maybe[Int] = Maybe.Empty
  val some: Maybe[Int] = Maybe(42)

  println(none.map(_ + 1).fold(identity, 0)) // 0
  println(some.map(_ + 1).fold(identity, 0)) // 43
}*/

object Coherency extends App {

  trait Enumerable[A] {
    def enumerate: Iterable[A]
  }
  object Enumerable {
    def apply[A: Enumerable]: Enumerable[A] = implicitly[Enumerable[A]]
  }

  trait Bounded[A] {
    def min: A
    def max: A
  }
  object Bounded {}

  trait Indexed[A] {
    def indexOf(a: A): Int
    def get(idx: Int): Option[A]
  }
  object Indexed {}

  //def example[A: Indexed: Bounded](): Unit = Enumerable[A]
}

/*
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
}*/
