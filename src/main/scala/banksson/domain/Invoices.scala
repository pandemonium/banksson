package banksson
package domain

import doobie.{ Get, Put }
import java.time._


trait Invoices { module: Identifiers with Contracts
                                     with Parties =>
  import io.circe._

  object Invoice extends EntityModule {
    object State extends EntityModule {
      object Type {
        sealed trait T
        case object New
          extends T
        case object Sent
          extends T
        case object Paid
          extends T
        case object Partial
          extends T
        case object Overpaid
          extends T
        case object Overdue
          extends T

        private[Type]
        case class Unknown(name: String)
          extends T

        def fromName(name: String): Type.T = name match {
          case "new"         => New
          case "sent"        => Sent
          case "paid"        => Paid
          case "partial"     => Partial
          case "overpaid"    => Overpaid
          case "overdue"     => Overdue

          case unknown       => Unknown(name)
        }

        def toName(`type`: Type.T): String = `type` match {
          case New           => "new"      
          case Sent          => "sent"     
          case Paid          => "paid"     
          case Partial       => "partial"  
          case Overpaid      => "overpaid" 
          case Overdue       => "overdue"  

          // How safe is this?
          case Unknown(name) => s"unknown: $name"
        }
      }

      case class History(enteredAt: LocalDateTime,
                            `type`: Type.T,
                             notes: Option[String])
    }

    case class T(createdDate: LocalDate,
                     dueDate: LocalDate,
                    sellerId: Party.Id,
                     buyerId: Party.Id,
                 amountTotal: Int,
                    vatTotal: Int,
                   reference: String,
                       state: State.Type.T)  // this field is a tad bit worrysome.

    case class Item(contractId: Contract.Id,
                        amount: Int,
                           vat: Option[Int],
                   description: Option[String])
  }

  implicit def decodeInvoiceId: Decoder[Invoice.Id] =
    Invoice.Id.deriveDecoder

  implicit def encodeInvoiceStateType: Encoder[Invoice.State.Type.T] =
    Encoder.encodeString
           .contramap(Invoice.State.Type.toName)

  implicit def decodeInvoiceStateType: Decoder[Invoice.State.Type.T] =
    Decoder.decodeString
           .map(Invoice.State.Type.fromName)

  implicit val getInvoiceState: Get[Invoice.State.Type.T] =
    Get[String].tmap(Invoice.State.Type.fromName)

  implicit val putInvoiceState: Put[Invoice.State.Type.T] =
    Put[String].tcontramap(Invoice.State.Type.toName)

  implicit val getInvoiceId: Get[Invoice.Id] = 
    Invoice.Id.deriveGet

  implicit val putInvoiceId: Put[Invoice.Id] =
    Invoice.Id.derivePut

  implicit val getInvoiceStateId: Get[Invoice.State.Id] = 
    Invoice.State.Id.deriveGet

  implicit val putInvoiceStateId: Put[Invoice.State.Id] =
    Invoice.State.Id.derivePut
}