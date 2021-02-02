package main.scala.dev.contramap.recursionScheme

object RecursionScheme {
  sealed trait Json
  object Json {
    case object Null                               extends Json
    case class Str(value: String)                  extends Json
    case class Num(value: BigDecimal)              extends Json
    case class Bool(value: Boolean)                extends Json
    case class Arr(values: Vector[Json])           extends Json
    case class Obj(values: Vector[(String, Json)]) extends Json
  }

  // implementation of fold, size, fields
  def fold[Z](z: Z)(pf: PartialFunction[Json, Z]): Z = ???

//   def fields: Set[String] =
//     self.fold {
//         case Json.Obj(fields) => fields.map(_._1).toSet
//     }

  // Recursion Scheme's purpose:
  // getting traversal for free in exchange of model refactoring: fold, unfold, ...

}

object RecursionScheme2 {
  // First step: take the recursive data-structure, and identity every case where
  // the data is referring itself and replace it by Self so that we rip out
  // the recursion
  sealed trait JsonCase[+Self] { self =>
    // First step to get fold for free
    def map[Self2](f: Self => Self2): JsonCase[Self2] = self match {
      case JsonCase.Null               => JsonCase.Null
      case str @ JsonCase.Str(value)   => str
      case num @ JsonCase.Num(value)   => num
      case bool @ JsonCase.Bool(value) => bool
      case JsonCase.Arr(values)        => JsonCase.Arr(values.map(f))
      case JsonCase.Obj(fields) =>
        JsonCase.Obj(fields.map {
          case (field, value) => (field, f(value))
        })
    }
  }
  object JsonCase {
    // leaf nodes terminate the recursion
    case object Null                                     extends JsonCase[Nothing]
    case class Str(value: String)                        extends JsonCase[Nothing]
    case class Num(value: BigDecimal)                    extends JsonCase[Nothing]
    case class Bool(value: Boolean)                      extends JsonCase[Nothing]
    case class Arr[Self](values: Vector[Self])           extends JsonCase[Self]
    case class Obj[Self](fields: Vector[(String, Self)]) extends JsonCase[Self]
  }

  final case class Json(value: JsonCase[Json]) {
    // Folding is done bottom up
    def fold[Z](f: JsonCase[Z] => Z): Z =
      f(value.map(_.fold(f))) // decompose this part in the blog post
    // This is super type-safe, the recursion is done in the map
    // it's not infinite thanks to leaf node

    def fields: Set[String] =
      fold[Set[String]] {
        case JsonCase.Null    => Set.empty
        case JsonCase.Str(_)  => Set.empty
        case JsonCase.Num(_)  => Set.empty
        case JsonCase.Bool(_) => Set.empty
        // all elements of the array have their field computed already
        case JsonCase.Arr(vs) => vs.fold(Set.empty)(_ ++ _)
        case JsonCase.Obj(vs) =>
          vs.foldLeft(Set.empty[String]) {
            case (acc, (name, names)) => (acc + name) ++ names
          }
      }

    def size: Int =
      fold[Int] {
        case JsonCase.Null    => 1
        case JsonCase.Str(_)  => 1
        case JsonCase.Num(_)  => 1
        case JsonCase.Bool(_) => 1
        case JsonCase.Arr(vs) => vs.sum // all elements of the array have their size computed already
        case JsonCase.Obj(vs) => vs.map(_._2).sum
      }
  }
  object Json {
    val Null                                = Json(JsonCase.Null)
    def Str(value: String)                  = Json(JsonCase.Str(value))
    def Num(value: BigDecimal)              = Json(JsonCase.Num(value))
    def Bool(value: Boolean)                = Json(JsonCase.Bool(value))
    def Arr(values: Vector[Json])           = Json(JsonCase.Arr(values))
    def Obj(values: Vector[(String, Json)]) = Json(JsonCase.Obj(values))
  }

  val person = Json.Obj(Vector("name" -> Json.Str("Sherlock Holmes"), "age" -> Json.Num(42)))

  // Cons: Complex, boilerplate, lot of allocation, lot of work
  // This is a Fix-Point encoding
  // How can we get from here to something that gives fold for free
  // we need to map over Self

}

object RecursionScheme3 {

  sealed trait JsonCase[+Self] { self =>
    def map[Self2](f: Self => Self2): JsonCase[Self2] = self match {
      case JsonCase.Null               => JsonCase.Null
      case str @ JsonCase.Str(value)   => str
      case num @ JsonCase.Num(value)   => num
      case bool @ JsonCase.Bool(value) => bool
      case JsonCase.Arr(values)        => JsonCase.Arr(values.map(f))
      case JsonCase.Obj(fields) =>
        JsonCase.Obj(fields.map {
          case (field, value) => (field, f(value))
        })
    }
  }
  object JsonCase {
    case object Null                                     extends JsonCase[Nothing]
    case class Str(value: String)                        extends JsonCase[Nothing]
    case class Num(value: BigDecimal)                    extends JsonCase[Nothing]
    case class Bool(value: Boolean)                      extends JsonCase[Nothing]
    case class Arr[Self](values: Vector[Self])           extends JsonCase[Self]
    case class Obj[Self](fields: Vector[(String, Self)]) extends JsonCase[Self]

    implicit def FnToAlgebra[A](f: JsonCase[A] => A): JsonAlgebra[A] =
      JsonAlgebra(f)
  }

  final case class Json(value: JsonCase[Json]) {
    def fold[Z](f: JsonAlgebra[Z]): Z =
      f(value.map(_.fold(f)))

    def fields: Set[String] = fold(FieldsAlgebra)
    def size: Int           = fold(SizeAlgebra)
  }
  object Json {
    val Null                                = Json(JsonCase.Null)
    def Str(value: String)                  = Json(JsonCase.Str(value))
    def Num(value: BigDecimal)              = Json(JsonCase.Num(value))
    def Bool(value: Boolean)                = Json(JsonCase.Bool(value))
    def Arr(values: Vector[Json])           = Json(JsonCase.Arr(values))
    def Obj(values: Vector[(String, Json)]) = Json(JsonCase.Obj(values))
  }

  // Recursion Schemes support fusion, that is a property enabling to fuse
  // multiple Recursion Schemes to traverse a data structure in a single path

  // To achieve that, we have to abstract over operations like fold

  final case class JsonAlgebra[A](f: JsonCase[A] => A) extends (JsonCase[A] => A) { self =>
    def apply(v: JsonCase[A]): A = f(v)

    def zip[B](that: JsonAlgebra[B]): JsonAlgebra[(A, B)] =
      JsonAlgebra[(A, B)](jsonCaseA => self(jsonCaseA.map(_._1)) -> that(jsonCaseA.map(_._2)))
  }

  val SizeAlgebra: JsonAlgebra[Int] = {
    case JsonCase.Null    => 1
    case JsonCase.Str(_)  => 1
    case JsonCase.Num(_)  => 1
    case JsonCase.Bool(_) => 1
    case JsonCase.Arr(vs) => vs.sum
    case JsonCase.Obj(vs) => vs.map(_._2).sum
  }

  val FieldsAlgebra: JsonAlgebra[Set[String]] = {
    case JsonCase.Null    => Set.empty
    case JsonCase.Str(_)  => Set.empty
    case JsonCase.Num(_)  => Set.empty
    case JsonCase.Bool(_) => Set.empty
    case JsonCase.Arr(vs) => vs.fold(Set.empty)(_ ++ _)
    case JsonCase.Obj(vs) =>
      vs.foldLeft(Set.empty[String]) {
        case (acc, (name, names)) => (acc + name) ++ names
      }
  }

  val person = Json.Obj(Vector("name" -> Json.Str("Sherlock Holmes"), "age" -> Json.Num(42)))

  person.fold(FieldsAlgebra zip SizeAlgebra)

  // Drawbacks: performance, that can be solved with Opaque types
  // Lot of machinery that can be solved with recursion schemes libraries

  // Exercise
  // Write a fold that computes the maximum size of any array inside this json value

  val MaxSizeAlgebra: JsonAlgebra[Int] = {
    case JsonCase.Null    => 0
    case JsonCase.Str(_)  => 1
    case JsonCase.Num(_)  => 1
    case JsonCase.Bool(_) => 1
    case JsonCase.Arr(vs) => vs.fold(vs.size)(_ max _)
    case JsonCase.Obj(vs) => vs.map(_._2).fold(vs.size)(_ max _)
  }
}

object RecursionScheme4 {

  sealed trait JsonCase[+Self] { self =>
    def map[Self2](f: Self => Self2): JsonCase[Self2] = self match {
      case JsonCase.Null               => JsonCase.Null
      case str @ JsonCase.Str(value)   => str
      case num @ JsonCase.Num(value)   => num
      case bool @ JsonCase.Bool(value) => bool
      case JsonCase.Arr(values)        => JsonCase.Arr(values.map(f))
      case JsonCase.Obj(fields) =>
        JsonCase.Obj(fields.map {
          case (field, value) => (field, f(value))
        })
    }
  }
  object JsonCase {
    case object Null                                     extends JsonCase[Nothing]
    case class Str(value: String)                        extends JsonCase[Nothing]
    case class Num(value: BigDecimal)                    extends JsonCase[Nothing]
    case class Bool(value: Boolean)                      extends JsonCase[Nothing]
    case class Arr[Self](values: Vector[Self])           extends JsonCase[Self]
    case class Obj[Self](fields: Vector[(String, Self)]) extends JsonCase[Self]

    implicit def FnToAlgebra[A](f: JsonCase[A] => A): JsonAlgebra[A] =
      JsonAlgebra(f)
  }

  trait JsonCaseMapper {
    def apply[A](jsonCase: JsonCase[A]): JsonCase[A]
  }

  final case class Json(value: JsonCase[Json]) { self =>
    def fold[Z](f: JsonAlgebra[Z]): Z =
      f(value.map(_.fold(f)))

    // transformation using direct recursion, not great though
    // How can we solve this issue using recursion schemes?
    // we need to describe to transcribe this concept of transformation
    // def replaceNulls(json: Json): Json =
    //   value match {
    //     case JsonCase.Null    => json
    //     case JsonCase.Bool(v) => ???
    //     case JsonCase.Str(v)  => ???
    //     case JsonCase.Num(v)  => ???
    //     case JsonCase.Arr(vs) => Json(JsonCase.Arr(vs.map(_.replaceNulls(json))))
    //     case JsonCase.Obj(vs) =>
    //       Json(JsonCase.Obj(vs.map {
    //         case (field, value) => (field, value.replaceNulls(json))
    //       }))
    //   }

    // def transform(f: Json => Json): Json = ???
    // def replaceNull(json: Json): Json = ???

    // def transform(f: JsonCase[?] => JsonCase[?]): Json = f(self)

    // Allows to transform a tree
    // Bottom Up (from the leaf)
    def transformUp(f: JsonCaseMapper): Json =
      Json(f(value.map(_.transformUp(f))))

    // Top Down (from to the top to the leave)
    def transformDown(f: JsonCaseMapper): Json =
      Json(f(value).map(_.transformDown(f)))

    // Does this matter? Yes in the case of replaceNulls and in terms of
    // number of calls. If f(value) happens to be a Null, then map is a no-op
    // This matters also if f has side-effect. If it's referentially-transparent
    // there is no difference from the caller's point of view.
    // So this can be useful with an IO for example, in terms of performances and
    // number of calls.

    def replaceNulls(defaultValue: JsonCase[Nothing]): Json =
      transformUp(new JsonCaseMapper {
        def apply[A](json: JsonCase[A]): JsonCase[A] =
          json match {
            case JsonCase.Null => defaultValue
            case jsonCase      => jsonCase
          }
      })

    def replaceFields(f: String => String): Json =
      transformUp(new JsonCaseMapper {
        def apply[A](json: JsonCase[A]): JsonCase[A] =
          json match {
            case JsonCase.Obj(fields) =>
              JsonCase.Obj(fields.map {
                case (field, value) => f(field) -> value
              })
            case jsonCase => jsonCase
          }
      })

    // Exercise: JsonCaseMapper from Functional Design point of view

  }
  object Json {

    def unfold[A](a: A)(coalgebra: JsonCoAlgebra[A]): Json =
      Json(coalgebra(a).map(unfold(_)(coalgebra)))

    val Null                                = Json(JsonCase.Null)
    def Str(value: String)                  = Json(JsonCase.Str(value))
    def Num(value: BigDecimal)              = Json(JsonCase.Num(value))
    def Bool(value: Boolean)                = Json(JsonCase.Bool(value))
    def Arr(values: Vector[Json])           = Json(JsonCase.Arr(values))
    def Obj(values: Vector[(String, Json)]) = Json(JsonCase.Obj(values))
  }

  // Recursion Schemes support fusion, that is a property enabling to fuse
  // multiple Recursion Schemes to traverse a data structure in a single path

  // To achieve that, we have to abstract over operations like fold

  final case class JsonAlgebra[A](f: JsonCase[A] => A) extends (JsonCase[A] => A) { self =>
    def apply(v: JsonCase[A]): A = f(v)

    def zip[B](that: JsonAlgebra[B]): JsonAlgebra[(A, B)] =
      JsonAlgebra[(A, B)](jsonCaseA => self(jsonCaseA.map(_._1)) -> that(jsonCaseA.map(_._2)))
  }

  final case class JsonCoAlgebra[A](f: A => JsonCase[A]) extends (A => JsonCase[A]) { self =>
    def apply(value: A): JsonCase[A] = f(value)
  }

}

object AdminEvent extends App {

  sealed trait DiffCase[+Self] { self =>
    def map[Self2](f: Self => Self2): DiffCase[Self2] = self match {
      case DiffCase.Null               => DiffCase.Null
      case name @ DiffCase.Name(value) => name
      case age @ DiffCase.Age(value)   => age
      case DiffCase.Arr(values)        => DiffCase.Arr(values.map(f))
    }
  }
  object DiffCase {
    case object Null                           extends DiffCase[Nothing]
    case class Name(value: String)             extends DiffCase[Nothing]
    case class Age(value: BigDecimal)          extends DiffCase[Nothing]
    case class Arr[Self](values: Vector[Self]) extends DiffCase[Self]
  }

  case class Diff(diffCase: DiffCase[Diff]) {
    def fold[Z](f: DiffAlgebra[Z]): Z =
      f(diffCase.map(_.fold(f)))
  }
  object Diff {
    val Null                      = Diff(DiffCase.Null)
    def age(value: BigDecimal)    = Diff(DiffCase.Age(value))
    def name(value: String)       = Diff(DiffCase.Name(value))
    def arr(values: Vector[Diff]) = Diff(DiffCase.Arr(values))
  }

  final case class DiffAlgebra[A](f: DiffCase[A] => A) extends (DiffCase[A] => A) { self =>
    def apply(v: DiffCase[A]): A = f(v)
  }
  object DiffAlgebra {
    implicit def FnToAlgebra[A](f: DiffCase[A] => A): DiffAlgebra[A] =
      DiffAlgebra(f)
  }

  import Diff._

  def compare(p1: Person, p2: Person): Diff = {
    val diff =
      if (p1.name != p2.name) Diff.name(p2.name)
      else if (p1.age != p2.age) Diff.age(p2.age)
      else Diff.Null

    val tuples = p1.persons.zip(p2.persons)
    // This could be optimized so that we could remove the recursive call
    val diffs = tuples.map { case (l, r) => compare(l, r) }.filter(_ != Diff.Null)
    if (diffs.isEmpty) diff else arr(diffs :+ diff)
  }

  val vs   = Vector(age(42), name(""))
  val diff = arr(Vector(name(""), arr(vs)))

  case class Person(name: String, age: Int, persons: Vector[Person])

  val p1 = Person("first", 42, Vector.empty)
  val p2 = Person("first", 24, Vector(p1))

  val merge = compare(p1, p2).fold[Person => Person] {
    case DiffCase.Null        => identity
    case DiffCase.Age(value)  => _.copy(age = value.toInt)
    case DiffCase.Name(value) => _.copy(name = value)
    case DiffCase.Arr(values) => values.fold[Person => Person](identity)(_ andThen _)
  }

  println(merge(p2))
  println(merge(p1))
}
