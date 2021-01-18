package main.scala.dev.contramap.hlist

sealed trait HList {
  type Append[That] <: HList
  def append[That](that: That): Append[That]

  type Concat[That <: HList] <: HList
  def ++[That <: HList](that: That): Concat[That] = concat(that)
  def concat[That <: HList](that: That): Concat[That]
}
object HList {
  type HNil = HNil.type

  case object HNil extends HList {
    type Append[That]          = That :: HNil
    type Concat[That <: HList] = That

    def concat[That <: HList](that: That): That =
      that

    def ::[A](that: A): A :: HNil =
      HList.::(that, HNil)

    def append[That](that: That): HList.::[That, HNil] =
      that :: HNil

  }

  case class ::[A, B <: HList](head: A, tail: B) extends HList { self =>
    type Append[That]          = A :: tail.Append[That]
    type Concat[That <: HList] = A :: tail.Concat[That]

    def concat[That <: HList](that: That): Concat[That] =
      HList.::(head, tail.concat(that))

    def ::[C](that: C): C :: A :: B = HList.::(that, self)

    def append[That](that: That): A :: tail.Append[That] =
      HList.::(head, tail.append(that))

    def :+[That](that: That): A :: tail.Append[That] =
      append(that)

    def map[C](implicit p: Poly[A :: B, C]): C =
      p(HList.::(head, tail))

    def take[N <: Natural, T <: HList](n: N)(implicit t: Take.Aux[N, A :: B, T]): T = {
      val _ = n
      t(self)
    }

    def filter[C, O](tt: TType[C])(implicit f: Filter.Aux[C, A :: B, O]): O = {
      val _ = tt
      f(self)
    }
  }
}

import HList._
import Natural._

sealed trait Natural {
  def value: Int
}
object Natural {
  case object _0                           extends Natural { val value = 0 }
  class Succ[A <: Natural](val value: Int) extends Natural

  type _0 = _0.type
  type _1 = Succ[_0]
  type _2 = Succ[_1]
  type _3 = Succ[_2]
  type _4 = Succ[_3]

  implicit val _1: _1 = new Succ(0)
  implicit val _2: _2 = new Succ(1)
  implicit val _3: _3 = new Succ(2)
  implicit val _4: _4 = new Succ(3)
}

sealed trait TType[A]
object TType {
  case object TInt     extends TType[Int]
  case object TString  extends TType[String]
  case object TBoolean extends TType[Boolean]
  case object TDouble  extends TType[Double]
}

sealed trait Filter[-Lookup, -In] {
  type Out <: HList
  def apply(in: In): Out
}

object Filter extends FilterImplicits0 {
  type Aux[Lookup, In, Out0] = Filter[Lookup, In] { type Out = Out0 }

  implicit case object Zero extends Filter[Any, HNil] {
    type Out = HNil
    def apply(in: HNil): HNil = HNil
  }

  implicit def found[A, B <: HList](implicit f: Filter[A, B]): Filter.Aux[A, A :: B, A :: f.Out] =
    new Filter[A, A :: B] {
      type Out = A :: f.Out
      def apply(in: A :: B): A :: f.Out =
        HList.::(in.head, f(in.tail))
    }
}

sealed trait FilterImplicits0 {

  implicit def notFound[A0, A, B <: HList](
    implicit f: Filter[A0, B]
  ): Filter.Aux[A0, A :: B, f.Out] =
    new Filter[A0, A :: B] {
      type Out = f.Out
      def apply(in: A :: B): Out =
        f(in.tail)
    }
}

sealed trait Take[N <: Natural, A <: HList] {
  type Out <: HList
  def apply(a: A): Out
}
object Take {

  type Aux[N <: Natural, A <: HList, Out0] = Take[N, A] { type Out = Out0 }

  implicit def zero[A <: HList]: Take.Aux[_0, A, HNil] =
    new Take[_0, A] {
      type Out = HNil
      def apply(a: A): HNil = HNil
    }

  implicit def many[A, B <: HList, N <: Natural](
    implicit t: Take[N, B]
  ): Take.Aux[Succ[N], A :: B, A :: t.Out] =
    new Take[Succ[N], A :: B] {
      type Out = A :: t.Out
      def apply(a: A :: B): Out =
        HList.::(a.head, t(a.tail))
    }
}

// Polymorphic function for Mapping
sealed trait Poly[A, B] { self =>
  def apply(value: A): B
}
object Poly {

  def make[A, B](f: A => B): Poly[A, B] =
    new Poly[A, B] {
      def apply(value: A): B =
        f(value)
    }

  implicit def zero: Poly[HNil, HNil] = make(identity)

  implicit def many[A, A0, B <: HList, B0 <: HList](
    implicit ev: Poly[A, A0],
    ev0: Poly[B, B0]
  ): Poly[A :: B, A0 :: B0] =
    make(value => HList.::(ev(value.head), ev0(value.tail)))
}

import TType._

object Example {
  val bool   = true :: HNil
  val string = "" :: HNil
  val int    = 1 :: HNil

  val boolAndFloat: Boolean :: Double :: HNil = bool :+ 3.14
  val all: Boolean :: String :: Int :: HNil   = bool ++ string ++ int

  // **** MAP ****
  implicit val case1 = Poly.make((_: Boolean).toString())
  implicit val case2 = Poly.make((_: Int).longValue())
  implicit val case3 = Poly.make((_: String).length)

  val boolAsString: String :: HNil        = bool.map
  val all0: String :: Int :: Long :: HNil = all.map

  // **** TAKE ****
  val zero: HNil                              = all.take(_0)
  val one: Boolean :: HNil                    = all.take(_1)
  val two: Boolean :: String :: HNil          = all.take(_2)
  val three: Boolean :: String :: Int :: HNil = all.take(_3)
  // val four = all.take(_4) // does not compile as expected

  // **** Filter ****
  val zero0: HNil             = all.filter(TDouble)
  val bools: Boolean :: HNil  = all.filter(TBoolean)
  val ints: Int :: HNil       = all.filter(TInt)
  val strings: String :: HNil = all.filter(TString)

  val allBools: Boolean :: Boolean :: HNil = (all ++ all).filter(TBoolean)
  val allInts: Int :: Int :: HNil          = (all ++ all).filter(TInt)
  val allStrings: String :: String :: HNil = (all ++ all).filter(TString)
}

sealed trait HMap {
  type Append[K, V] <: HMap
  def append[K, V](kv: (K, V)): Append[K, V]
  def :+[K, V](kv: (K, V)): Append[K, V] = append(kv)

  type Concat[That <: HMap] <: HMap
  def ++[That <: HMap](that: That): Concat[That] = concat(that)
  def concat[That <: HMap](that: That): Concat[That]

  type Keys <: HList
  def keys: Keys

  type Values <: HList
  def values: Values
}
object HMap {

  sealed trait Drop[-K0, M <: HMap] {
    type Out <: HMap
    def apply(map: M): Out
  }
  object Drop extends DropImplicits0 {

    type Aux[K0, M <: HMap, Out0] = Drop[K0, M] { type Out = Out0 }

    implicit def empty: Drop.Aux[Any, HMNil, HMNil] = new Drop[Any, HMNil] {
      type Out = HMNil
      def apply(map: HMNil): HMNil = HMNil
    }

    implicit def found[K, V, T <: HMap, Out0 <: HMap](implicit ev: Drop.Aux[K, T, Out0]): Drop.Aux[K, ::[K, V, T], Out0] = ???
      // new Drop[K, ::[K, V, T]] {
      //   type Out = Out0
      //   def apply(map: ::[K, V, T]): Out0 = ev(map.tail)
      // }
  }

  sealed trait DropImplicits0 {
    implicit def notFound[K0, K, V, T <: HMap, Out0 <: HMap](implicit ev: Drop.Aux[K0, T, Out0]): Drop.Aux[K0, ::[K, V, T], Out0] = ???
      // new Drop[K0, ::[K, V, T]] {
      //   type Out = ::[K, V, T]
      //   def apply(map: ::[K, V, T]): ::[K, V, T] = ???
      // }

  }

  type HMNil = HMNil.type
  case object HMNil extends HMap { self =>
    type Append[K, V] = ::[K, V, HMNil]
    def append[K, V](kv: (K, V)): ::[K, V, HMNil] =
      ::(kv, self)

    type Concat[That <: HMap] = That
    def concat[That <: HMap](that: That): Concat[That] =
      that

    type Values = HNil
    def values: Values = HNil

    type Keys = HNil
    def keys: HNil = HNil
  }
  case class ::[K, V, T <: HMap](kv: (K, V), tail: T) extends HMap { self =>
    type Append[K0, V0] = ::[K, V, tail.Append[K0, V0]]
    def append[K0, V0](kv0: (K0, V0)): Append[K0, V0] =
      ::(kv, tail.append(kv0))

    type Concat[That <: HMap] = ::[K, V, tail.Concat[That]]
    def concat[That <: HMap](that: That): Concat[That] =
      ::(kv, tail.concat(that))

    type Values = HList.::[V, tail.Values]
    def values: Values = HList.::(kv._2, tail.values)

    type Keys = HList.::[K, tail.Keys]
    def keys: Keys = HList.::(kv._1, tail.keys)

    def drop[K0, Out](k: K0)(implicit d: Drop.Aux[K0, ::[K, V, T], Out]): Out =
      d.apply(self)
  }
}

object HMapExample {
  import HMap._

  val x = ::(("" -> 42), HMNil)

  val y1: HMNil = x.drop("")
  val y0 = x.drop(42)

  val xx = x :+ (42f -> true)
  val yy = (xx ++ xx).drop("")
}
