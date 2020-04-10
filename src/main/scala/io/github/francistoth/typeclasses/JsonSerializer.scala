package io.github.francistoth.typeclasses

import io.github.francistoth.domain._

trait Json
trait JSonSerializer[A] {
  def toJson(a: A): Json
}
object JSonSerializer extends JsonSerializerSyntax  {
  def apply[A: JSonSerializer]: JSonSerializer[A] =
    implicitly[JSonSerializer[A]]

  implicit val personJsonSerializer: JSonSerializer[Person] = 
    _ => new Json {}
}

trait JsonSerializerSyntax {
  implicit class JsonSerializerOps[A: JSonSerializer](a: A) {
    def toJson: Json = JSonSerializer[A].toJson(a)
  }
}
