package banksson
package domain

import doobie.{ Get, Put }
import java.time._


trait PaymentStructures { module: Identifiers with Loans =>
  import io.circe._

  object PaymentStructure extends EntityModule {
    object Term extends EntityModule {
      object Type {
        sealed trait T
        case object AnnuityPayment
          extends T
        case object InterestRate
          extends T
        case object AmortizationAmount
          extends T

        private[Type]
        case class Unknown(name: String)
          extends T

        def fromName(name: String): Type.T = name match {
          case "annuity-payment"     => AnnuityPayment
          case "interest-rate"       => InterestRate
          case "amortization-amount" => AmortizationAmount
          case unknown               => Unknown(name)
        }

        def toName(`type`: Type.T): String = `type` match {
          case AnnuityPayment     => "annuity-payment"
          case InterestRate       => "interest-rate"
          case AmortizationAmount => "amortization-amount"

          // How safe is this?
          case Unknown(name)  => s"unknown: $name"
        }

        implicit def encodePsTermType: Encoder[Term.Type.T] =
          Encoder.encodeString
                .contramap(toName)
      }

      case class T(`type`: Type.T,
              structureId: PaymentStructure.Id,
                     from: LocalDate,
                  through: Option[LocalDate],
                    value: Int)
    }

    object Type {
      sealed trait T
      case object AnnuityLoan
        extends T
      case object FixedAmountAmortization
        extends T

      private[Type]
      case class Unknown(name: String)
        extends T

      def fromName(name: String): Type.T = name match {
        case "annuity-loan"              => AnnuityLoan
        case "fixed-amount-amortization" => FixedAmountAmortization
        case unknown                     => Unknown(name)
      }

      def toName(`type`: Type.T): String = `type` match {
        case AnnuityLoan             => "annuity-loan"
        case FixedAmountAmortization => "fixed-amount-amortization"

        // How safe is this?
        case Unknown(name)           => s"unknown: $name"
      }

      implicit def encodePsType: Encoder[Type.T] =
        Encoder.encodeString
               .contramap(toName)
    }

    case class T(`type`: Type.T, 
                   loan: Loan.Id)
  }


  implicit val getPaymentStructureType: Get[PaymentStructure.Type.T] =
    Get[String].tmap(PaymentStructure.Type.fromName)

  implicit val putPaymentStructureType: Put[PaymentStructure.Type.T] =
    Put[String].tcontramap(PaymentStructure.Type.toName)

  implicit val getPaymentStructureTermType: Get[PaymentStructure.Term.Type.T] =
    Get[String].tmap(PaymentStructure.Term.Type.fromName)

  implicit val putPaymentStructureTermType: Put[PaymentStructure.Term.Type.T] =
    Put[String].tcontramap(PaymentStructure.Term.Type.toName)

  implicit val getPaymentStructureId: Get[PaymentStructure.Id] = 
    PaymentStructure.Id.deriveGet

  implicit val putPaymentStructureId: Put[PaymentStructure.Id] =
    PaymentStructure.Id.derivePut

  implicit val getPaymentStructureTermId: Get[PaymentStructure.Term.Id] = 
    PaymentStructure.Term.Id.deriveGet

  implicit val putPaymentStructureTermId: Put[PaymentStructure.Term.Id] =
    PaymentStructure.Term.Id.derivePut
}