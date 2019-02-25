package banksson
package domain

import doobie._,
       doobie.implicits._,
       doobie.postgres._,
       doobie.postgres.implicits._
import java.util.UUID
import shapeless._,
       newtype._,
       tag._

trait Identifiers {
  object Identifier {
    private[Identifiers]
    type NakedId = UUID

    type T = Newtype[NakedId, IdentifierOps]

    def fromNakedValue(x: NakedId): T =
      newtype(x)

    case class IdentifierOps(value: NakedId) {

      // Protect this so that only the Put can see it?
      def toNakedValue: NakedId = value
    }

    implicit val makeIdentifierOps = IdentifierOps
  }

  trait EntityModule {
    type Tag
    type Id = Identifier.T @@ Tag

    object Id {
      def fromNakedValue(value: Identifier.NakedId): Id =
        tag[Tag][Identifier.T](Identifier.fromNakedValue(value))

      implicit def deriveGet: Get[Id] =
        Get[Identifier.NakedId].tmap(fromNakedValue)

      implicit def derivePut: Put[Id] =
        Put[Identifier.NakedId].tcontramap(_.toNakedValue)
    }
  }
}