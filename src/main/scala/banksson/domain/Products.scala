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

      private[Type]
      case class Unknown(name: String)
        extends T

      def fromName(name: String): Type.T = name match {
        case "annuity-loan" => AnnuityLoan
        case unknown        => Unknown(name)
      }

      def toName(`type`: Type.T): String = `type` match {
        case AnnuityLoan    => "annuity-loan"

        // How safe is this?
        case Unknown(name)  => s"unknown: $name"
      }

      implicit def encodeProductType: Encoder[Type.T] =
        Encoder.encodeString
               .contramap(toName)
    }

    case class T(`type`: Type.T,
                   name: String)
  }

  implicit val getProductType: Get[Product.Type.T] =
    Get[String].tmap(Product.Type.fromName)

  implicit val putProductType: Put[Product.Type.T] =
    Put[String].tcontramap(Product.Type.toName)

  implicit val getProductId: Get[Product.Id] = 
    Product.Id.deriveGet

  implicit val putProductId: Put[Product.Id] =
    Product.Id.derivePut
}