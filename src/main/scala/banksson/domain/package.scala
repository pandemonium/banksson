package banksson

import doobie._
import java.time.LocalDateTime
import java.sql.Timestamp

package object domain {
  implicit val dateMeta: Meta[LocalDateTime] =
    Meta[Timestamp].xmap(_.toLocalDateTime, Timestamp.valueOf)

  object model extends AnyRef
    with Accounts
    with Contracts
    with Currencies
    with EventRecords
    with Identifiers
    with Invoices
    with Loans
    with Parties
    with Products
    with PaymentStructures
}