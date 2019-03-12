package banksson
package domain

import doobie.{ Get, Put }
import java.time.LocalDateTime


trait Loans { module: Identifiers with Accounts 
                                  with Contracts
                                  with Currencies =>
  import io.circe._

  object Loan extends EntityModule {
    object Type {
      sealed trait T
      case object ConsumerCredit
        extends T

      private[Type]
      case class Unknown(name: String)
        extends T

      def fromName(name: String): Type.T = name match {
        case "consumer-credit" => ConsumerCredit
        case unknown           => Unknown(name)
      }

      def toName(`type`: Type.T): String = `type` match {
        case ConsumerCredit => "consumer-credit"

        // How safe is this?
        case Unknown(name)  => s"unknown: $name"
      }

      implicit def encodeLoanType: Encoder[Type.T] =
        Encoder.encodeString
               .contramap(toName)
    }

    case class T(`type`: Type.T,
               contract: Contract.Id,
        loanReceivables: Account.Id,
               currency: Currency.Id,
              createdAt: LocalDateTime,
              principal: Int)
  }

  implicit def decodeLoanId: Decoder[Loan.Id] =
    Loan.Id.deriveDecoder

  implicit def encodeLoanType: Encoder[Loan.Type.T] =
    Encoder.encodeString
           .contramap(Loan.Type.toName)

  implicit def decodeLoanType: Decoder[Loan.Type.T] =
    Decoder.decodeString
           .map(Loan.Type.fromName)

  implicit val getLoanType: Get[Loan.Type.T] =
    Get[String].tmap(Loan.Type.fromName)

  implicit val putLoanType: Put[Loan.Type.T] =
    Put[String].tcontramap(Loan.Type.toName)

  implicit val getLoanId: Get[Loan.Id] = 
    Loan.Id.deriveGet

  implicit val putLoanId: Put[Loan.Id] =
    Loan.Id.derivePut
}