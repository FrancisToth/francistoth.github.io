package dev.contramap.tio

import scala.io.StdIn

sealed trait IO[+E, +A] { self =>

  def *>[E0 >: E, B](io: IO[E0, B]): IO[E0, B] =
    andThen(_ => io)

  def andThen[E0 >: E, B](f: A => IO[E0, B]): IO[E0, B] =
    IO.andThen(self, f)

  def failedWith[E0](error: E0): IO[E0, Nothing] =
    IO.failed(error)

  def retry(count: Int): IO[E, A] =
    IO.retry(count - 1, self)
}

object IO {
  type UIO[+A] = IO[Nothing, A]

  case class AndThen[A, B, E](
    io: IO[E, A],
    f: A => IO[E, B]
  ) extends IO[E, B]

  case object GetStrLn                             extends UIO[String]
  case class PutStrLn(s: String)                   extends UIO[Unit]
  case class Failed[E](error: E)                   extends IO[E, Nothing]
  case class Retry[E, A](count: Int, io: IO[E, A]) extends IO[E, A]

  def andThen[A, B, E](io: IO[E, A], f: A => IO[E, B]): IO[E, B] =
    AndThen(io, f)

  def failed[E](error: E): IO[E, Nothing] =
    Failed(error)

  def getStrLn(): UIO[String] =
    GetStrLn

  def putStrLn(s: String): UIO[Unit] =
    PutStrLn(s)

  def retry[E, A](count: Int, io: IO[E, A]): IO[E, A] =
    Retry(count, io)

  // stack-safe
  def run0[E, A](io: IO[E, A]): Either[E, A] = trampoline(io).runT

  private def trampoline[E, A](io: IO[E, A]): Trampoline[Either[E, A]] =
    io match {
      case PutStrLn(s)   => Trampoline.done(Right(println(s)))
      case GetStrLn      => Trampoline.done(Right(StdIn.readLine()))
      case Failed(error) => Trampoline.done(Left(error))
      case AndThen(io, k) =>
        Trampoline.more(trampoline(io)).flatMap {
          case Left(e)   => Trampoline.done(Left(e))
          case Right(a0) => Trampoline.more(trampoline(k(a0)))
        }
      case Retry(count, io) =>
        Trampoline.more(trampoline(io)).flatMap {
          case Left(e) if count == 0 => Trampoline.done(Left(e))
          case Left(_)               => Trampoline.more(trampoline(io.retry(count)))
          case Right(a)              => Trampoline.done(Right(a))
        }
    }

  // not stack-safe
  def run[E, A](io: IO[E, A]): Either[E, A] = io match {
    case PutStrLn(s)    => Right(println(s))
    case GetStrLn       => Right(StdIn.readLine())
    case AndThen(io, f) => run(io).flatMap(a0 => run(f(a0)))
    case Failed(error)  => Left(error)
    case Retry(count, io) =>
      run(io) match {
        case Right(a)              => Right(a)
        case Left(e) if count == 0 => Left(e)
        case Left(_)               => run(io.retry(count))
      }
  }

  val program = putStrLn("Enter password") *>
    getStrLn().andThen {
      case "admin" => putStrLn("Welcome Bob!")
      case _ =>
        putStrLn("Ah ah ah. You didn't say the magic word.") *>
          failed("No soup for you!")
    }.retry(3)

  def count(n: Int): IO[Unit, Unit] =
    putStrLn("test").andThen(_ => 
      if (n == 0) putStrLn("Done") else count(n - 1)  
    )
}

sealed trait Trampoline[A] { self =>
  import Trampoline._

  def flatMap[B](f: A => Trampoline[B]): Trampoline[B] = self match {
    case FlatMap(sub, k) => FlatMap(sub, (x: Any) => k(x).flatMap(f))
    case x               => FlatMap(x, f)
  }

  final def runT: A = resume match {
    case Right(value) => value
    case Left(more)   => more().runT
  }

  def resume: Either[() => Trampoline[A], A] = self match {
    case Done(v) => Right(v)
    case More(k) => Left(k)
    case FlatMap(sub, cont) =>
      sub match {
        case Done(v) => cont(v).resume
        case More(k) => Left(() => FlatMap(k(), cont))
        case FlatMap(sub2, cont2) =>
          (FlatMap(sub2, (x: Any) => FlatMap(cont2(x), cont)): Trampoline[A]).resume
      }
  }
}

object Trampoline {
  case class Done[A](value: A)                  extends Trampoline[A]
  case class More[A](call: () => Trampoline[A]) extends Trampoline[A]
  case class FlatMap[A, B](
    sub: Trampoline[A],
    cont: A => Trampoline[B]
  ) extends Trampoline[B]

  def done[A](value: A): Trampoline[A]            = Done(value)
  def more[A](k: => Trampoline[A]): Trampoline[A] = More(() => k)
}
