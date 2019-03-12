package banksson
package domain

import doobie.{ Get, Put }
import java.time.LocalDate


trait Contracts { module: Identifiers with Products 
                                      with Parties =>
  import io.circe._

  object Contract extends EntityModule {
    object Type {
      sealed trait T
      case object DebtObligation
        extends T
      case object SavingsAccount
        extends T

      private[Type]
      case class Unknown(name: String)
        extends T

      def fromName(name: String): Type.T = name match {
        case "debt-obligation" => DebtObligation
        case "savings-account" => SavingsAccount
        case unknown           => Unknown(name)
      }

      def toName(`type`: Type.T): String = `type` match {
        case DebtObligation => "debt-obligation"
        case SavingsAccount => "savings-account"

        // How safe is this?
        case Unknown(name)  => s"unknown: $name"
      }
    }

    case class T(`type`: Type.T,
                product: Product.Id,
              validFrom: LocalDate,
           validThrough: LocalDate)

    case class ContractParty(contract: T,
                                party: Party.T, 
                                 role: Party.Role.T, 
                                share: Option[Double])
  }

  implicit def decodeContractId: Decoder[Contract.Id] =
    Contract.Id.deriveDecoder

  implicit def encodeContractType: Encoder[Contract.Type.T] =
    Encoder.encodeString
           .contramap(Contract.Type.toName)

  implicit def decodeContractType: Decoder[Contract.Type.T] =
    Decoder.decodeString
           .map(Contract.Type.fromName)

  implicit val getContractType: Get[Contract.Type.T] =
    Get[String].tmap(Contract.Type.fromName)

  implicit val putContractType: Put[Contract.Type.T] =
    Put[String].tcontramap(Contract.Type.toName)

  implicit val getContractId: Get[Contract.Id] = 
    Contract.Id.deriveGet

  implicit val putContractId: Put[Contract.Id] =
    Contract.Id.derivePut
}