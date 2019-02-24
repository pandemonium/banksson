package banksson
package domain

import doobie.{ Get, Put }


import shapeless._,
       newtype._,
       tag._

trait Identifiers {
  object Identifier {
    type T = Newtype[Int, IdentifierOps]

    def fromNakedInt(value: Int): T =
      newtype(value)

    case class IdentifierOps(value: Int) {

      // Protect this so that only the Put can see it?
      def toNakedInt: Int = value
    }

    implicit val makeIdentifierOps = IdentifierOps
  }

  trait EntityModule {
    type Tag
    type Id = Identifier.T @@ Tag

    object Id {
      def fromNakedInt(value: Int): Id =
        tag[Tag][Identifier.T](Identifier.fromNakedInt(value))

      def asNakedInt(id: Id): Int = 
        id.toNakedInt

      implicit def deriveGet: Get[Id] =
        Get[Int].tmap(fromNakedInt)

      implicit def derivePut: Put[Id] =
        Put[Int].tcontramap(asNakedInt)
    }
  }
}