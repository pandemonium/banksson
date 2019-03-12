package banksson
package domain

import doobie.{ Get, Put }


trait Products { module: Identifiers =>
  import io.circe._

  object Product extends EntityModule {
    object Type {
      sealed trait T
      case object AnnuityLoan
        extends T
      case object LoanProtectionInsurance
        extends T

      private[Type]
      case class Unknown(name: String)
        extends T

      def fromName(name: String): Type.T = name match {
        case "annuity-loan"              => AnnuityLoan
        case "loan-protection-insurance" => LoanProtectionInsurance
        case unknown                     => Unknown(name)
      }

      def toName(`type`: Type.T): String = `type` match {
        case AnnuityLoan                 => "annuity-loan"
        case LoanProtectionInsurance     => "loan-protection-insurance"

        // How safe is this?
        case Unknown(name)               => s"unknown: $name"
      }
    }

    case class T(`type`: Type.T,
                   name: String)
  }

  implicit def decodeProductId: Decoder[Product.Id] =
    Product.Id.deriveDecoder

  implicit def encodeProductType: Encoder[Product.Type.T] =
    Encoder.encodeString
           .contramap(Product.Type.toName)

  implicit def decodeProductType: Decoder[Product.Type.T] =
    Decoder.decodeString
           .map(Product.Type.fromName)

  implicit val getProductType: Get[Product.Type.T] =
    Get[String].tmap(Product.Type.fromName)

  implicit val putProductType: Put[Product.Type.T] =
    Put[String].tcontramap(Product.Type.toName)

  implicit val getProductId: Get[Product.Id] = 
    Product.Id.deriveGet

  implicit val putProductId: Put[Product.Id] =
    Product.Id.derivePut
}