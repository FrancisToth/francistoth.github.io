trait Enumerable[A] {
  def enumerate: Iterable[A]
}

object Enumerable extends EnumerableImplicits0 {
  def apply[A: Enumerable]: Enumerable[A] = implicitly[Enumerable[A]]

  // implicit def `Bounded ~> Enumerate`[A: Bounded]: Enumerable[A] = ???
  // implicit def `Indexed ~> Enumerate`[A: Bounded]: Enumerable[A] = ???
}

trait EnumerableImplicits1 {
  implicit def `Bounded ~> Enumerate`[A: Bounded]: Enumerable[A] = ???
}

trait EnumerableImplicits0 extends EnumerableImplicits1 {
  implicit def `Indexed ~> Enumerate`[A: Indexed]: Enumerable[A] = ???
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

def example[A: Indexed: Bounded](): Unit = Enumerable[A]
