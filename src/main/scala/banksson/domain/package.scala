package banksson

package object domain {
  object model extends AnyRef
    with Accounts
    with Contracts
    with Currencies
    with EventRecords
    with Identifiers
    with Loans
    with Parties
    with Products
    with PaymentStructures
}