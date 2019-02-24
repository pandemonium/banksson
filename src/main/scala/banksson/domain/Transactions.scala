package banksson
package domain

trait Transactions {
  object BusinessTransaction {
    sealed trait T

    case class PayInvoice()
      extends T

    case class ChargeInterest(/** account-of(loan) */)
      extends T

    case class CreateAccount()
      extends T

    case class CreateParty()
      extends T

    case class CreateContract()
      extends T

    case class CreateLoan()
      extends T
  }
}