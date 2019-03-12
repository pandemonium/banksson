package banksson
package domain

import doobie.{ Get, Put }
import java.time._

trait Accounts { module: Identifiers =>
  import io.circe._

  object Account extends EntityModule {
    object Type {
      sealed trait T
      case object Transaction
        extends T
      // Suppose `Asset-Account` is a better type?
      case object LoanReceivables
        extends T

      private[Type]
      case class Unknown(name: String)
        extends T

      def fromName(name: String): Type.T = name match {
        case "transaction" => LoanReceivables
        case unknown       => Unknown(name)
      }

      def toName(`type`: Type.T): String = `type` match {
        case Transaction     => "transaction"
        case LoanReceivables => "loan-receivables"

        // How safe is this?
        case Unknown(name)  => s"unknown: $name"
      }

      implicit def encodeAccountType: Encoder[Type.T] =
        Encoder.encodeString
               .contramap(toName)
    }

    case class T(`type`: Type.T, 
                   name: String)

    object Entry {
      sealed trait T
      case class Credit(data: Data)
        extends T
      case class Debit(data: Data)
        extends T

      case class Data(amount: Int,
                   valueDate: LocalDate, 
               transactionId: Long)
    }
  }

  implicit def decodeAccountId: Decoder[Account.Id] =
    Account.Id.deriveDecoder

  implicit def encodeAccountType: Encoder[Account.Type.T] =
    Encoder.encodeString
           .contramap(Account.Type.toName)

  implicit def decodeAccountType: Decoder[Account.Type.T] =
    Decoder.decodeString
           .map(Account.Type.fromName)

  implicit val getAccountType: Get[Account.Type.T] =
    Get[String].tmap(Account.Type.fromName)

  implicit val putAccountType: Put[Account.Type.T] =
    Put[String].tcontramap(Account.Type.toName)

  implicit val getAccountId: Get[Account.Id] = 
    Account.Id.deriveGet

  implicit val putAccountId: Put[Account.Id] =
    Account.Id.derivePut
}