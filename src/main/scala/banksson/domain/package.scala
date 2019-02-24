package banksson

package object domain {
  object model extends AnyRef
    with Accounts
    with Contracts
    with Currencies
    with Identifiers
    with Loans
    with Parties
    with Products
    with PaymentStructures

  
}