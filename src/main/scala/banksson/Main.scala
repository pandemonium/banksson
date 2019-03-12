package banksson

import cats._,
       cats.arrow._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.free._,
       cats.effect._
import java.time._

import core._,
       core.process._,
       core.executive._,
       domain.model._

object Main extends IOApp {
  import com.typesafe.config._

  def run(args: List[String]): IO[ExitCode] = {
    val databaseTransactor =
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)
    val repositories = assembleRepositories
    val context      = Executive.Context[IO](
      databaseTransactor,
      repositories,
      DatabaseAggregateWriter.make(repositories),
      DatabaseAggregateReader.make(repositories),
      Journal.make(repositories.events))
    val executive    = Executive.make(context)

    val task = for {
      partyId0   <- Query.generateId
      _          <- Command.createParty(partyId0,
                                        Party.Type.PrivateIndividual,
                                        "Bynk AB")

      partyId1   <- Query.generateId
      _          <- Command.createParty(partyId1,
                                        Party.Type.PrivateIndividual,
                                        "Patrik Andersson")

      productId  <- Query.generateId
      _          <- Command.createProduct(productId, 
                                          Product.Type.AnnuityLoan,
                                          "Standard Loan")

      contractId <- Query.generateId
      _          <- Command.createContract(contractId,
                                           Contract.Type.DebtObligation,
                                           Product.Id.fromNakedValue(productId), 
                                           LocalDate.now,
                                           LocalDate.now.plusYears(11))

      _          <- Command.addContractParty(contractId,
                                             Party.Role.Lender,
                                             Party.Id.fromNakedValue(partyId0),
                                             Option.empty)

      _          <- Command.addContractParty(contractId,
                                             Party.Role.Borrower,
                                             Party.Id.fromNakedValue(partyId1),
                                             Option(1))

      loanId     <- Query.generateId
      accountId  <- Query.generateId
      _          <- Command.createAccount(accountId, 
                                          Account.Type.LoanReceivables,
                                          s"Patrik Andersson ($loanId)")

      sek        <- Query.generateId
//      _          <- Command.createCurrency(sek, "SEK")

      _          <- Command.createLoan(loanId, 
                                       Loan.Type.ConsumerCredit,
                                       Contract.Id.fromNakedValue(contractId),
                                       Account.Id.fromNakedValue(accountId),
                                       Currency.Id.fromNakedValue(sek),
                                       LocalDateTime.now,
                                       10000000)
    } yield ()

//    val result = executive.run(task).unsafeRunSync
    val result = executive.accumulateFromJournal.unsafeRunSync
    println(result)

    IO(ExitCode.Success)
  }
}