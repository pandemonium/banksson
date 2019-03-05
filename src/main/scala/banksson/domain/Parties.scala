package banksson
package domain

import doobie.{ Get, Put }


trait Parties { module: Identifiers =>
  object Party extends EntityModule {
    object Type {
      sealed trait T
      case object PrivateIndividual
        extends T
      case object Bank
        extends T

      private[Type]
      case class Unknown(name: String)
        extends T

      def fromName(name: String): Type.T = name match {
        case "private-individual" => PrivateIndividual
        case "bank"               => Bank
        case unknown              => Unknown(name)
      }

      def toName(`type`: Type.T): String = `type` match {
        case PrivateIndividual => "private-individual"
        case Bank              => "bank"

        // How safe is this?
        case Unknown(name)     => s"unknown: $name"
      }
    }

    object Role {
      sealed trait T
      case object Lender
        extends T
      // Borrower ought to take a/ the `share` parameter.
      case object Borrower
        extends T
      case object Holder
        extends T
      case object Issuer
        extends T

      private[Role]
      case class Unknown(name: String)
        extends T

      def fromName(name: String): Role.T = name match {
        case "lender"   => Lender
        case "borrower" => Borrower
        case "holder"   => Holder
        case "issuer"   => Issuer
        case unknown    => Unknown(name)
      }

      def toName(role: Role.T): String = role match {
        case Lender        => "lender"
        case Borrower      => "borrower"
        case Holder        => "holder"
        case Issuer        => "issuer"

        // How safe is this?
        case Unknown(name) => s"unknown: $name"
      }

    }

    case class T(`type`: Party.Type.T,
                   name: String)
  }

  implicit def encodeRole: io.circe.Encoder[Party.Role.T] =
    io.circe.Encoder.encodeString
            .contramap(Party.Role.toName)

  implicit def encodePartyType: io.circe.Encoder[Party.Type.T] =
    io.circe.Encoder.encodeString
            .contramap(Party.Type.toName)

  implicit val getPartyType: Get[Party.Type.T] =
    Get[String].tmap(Party.Type.fromName)

  implicit val putPartyType: Put[Party.Type.T] =
    Put[String].tcontramap(Party.Type.toName)

  implicit val getRole: Get[Party.Role.T] =
    Get[String].tmap(Party.Role.fromName)

  implicit val putRole: Put[Party.Role.T] =
    Put[String].tcontramap(Party.Role.toName)

  implicit val getPartyId: Get[Party.Id] = 
    Party.Id.deriveGet

  implicit val putPartyId: Put[Party.Id] =
    Party.Id.derivePut
}