package banksson
package domain

trait Transactions {

  // These are Commands - essentially
  object BusinessTransaction {
    sealed trait T

    case class PayInvoice()
      extends T

    case class ChargeInterest(/** account-of(loan) */)
      extends T

    // Could I make a Process.T such that could

    /**
     * 
     * If this is a sequence over the/ a State monad, then this
     * will work. The state is the EventStream. Right?
     * 
     * But there are side effects that need to happen! ConnectionIO:s
     * have to be sequenced over and later transact():ed.
     * 
     * But that happens in applyEvent()
     * 
     * So, do I have an F[State[EventStream]] ?
     * 
     * def createCustomerLoan(...): Process.T =
     *   for {
     *     _          <- createParty(...)
     *     partyId    <- consume[PartyCreatedEvent].map(_.id)
     *     _          <- createContract(Bank.ownParty, partyId, ...)
     *     contractId <- consume[ContractCreatedEvent].map(_.id)
     *     _          <- createAccount(contract, ...)
     *     accountId  <- consume[AccountCreatedEvent].map(_.id)
     *     _          <- createLoan(accountId, ...)
     *     loanId     <- consume[LoanCreatedEvent].map(_.id)
     *   } yield loanId
     * 
     */

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