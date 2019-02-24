package banksson
package domain

import cats._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.effect._
import doobie._,
       doobie.implicits._
import java.time._,
       java.sql.Timestamp


object LoanRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    def newLoan(`type`: Loan.Type.T,
            contractId: Contract.Id,
       loanReceivables: Account.Id,
              currency: Currency.Id,
             createdAt: LocalDateTime,
             principal: Int): ConnectionIO[Loan.Id]
  }

  // This thing could just aswell take the Transactor
  def make[F[_]: Async]: T[F] = 
    Implementation[F]

  object Implementation {
    implicit val dateMeta: Meta[LocalDateTime] =
      Meta[Timestamp].xmap(_.toLocalDateTime, 
                           Timestamp.valueOf)

    private[Implementation]
    trait Template[F[_]] { self: Signature[F] =>
      def loanTypeId(`type`: Loan.Type.T): ConnectionIO[Int] = sql"""
        SELECT
            lt.id
          FROM loan_type lt
          WHERE
            lt.name = ${`type`}
      """.query[Int]
         .unique

      def insertNew(loanTypeId: Int,
                    contractId: Contract.Id,
               loanReceivables: Account.Id,
                    currencyId: Currency.Id,
                     createdAt: LocalDateTime,
                     principal: Int): ConnectionIO[Loan.Id] = sql"""
        INSERT
          INTO 
            loan (account_id, 
                  created_at, 
                  loan_type_id,
                  contract_id,
                  principal,
                  currency_id)
          VALUES
            ($loanReceivables, 
             $createdAt,
             $loanTypeId, 
             $contractId,
             $principal,
             $currencyId)
      """.update
         .withUniqueGeneratedKeys[Loan.Id]("id")

      // does it return ConnectionIO[A] or F[A]
      def newLoan(`type`: Loan.Type.T,
              contractId: Contract.Id,
         loanReceivables: Account.Id,
              currencyId: Currency.Id,
               createdAt: LocalDateTime,
               principal: Int): ConnectionIO[Loan.Id] = for {
        ltId   <- loanTypeId(`type`)
        loanId <- insertNew(ltId, 
                            contractId, 
                            loanReceivables,
                            currencyId,
                            createdAt,
                            principal)
      } yield loanId
    }

    def apply[F[_]: Async]: T[F] =
      new Implementation.Repository[F]

    class Repository[F[_]]
      extends T[F]
         with Template[F]
  }
}

object RunLoanRepository extends IOApp {
  import com.typesafe.config._
  import domain.model._

  def run(args: List[String]): IO[ExitCode] = {
    val xa = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    // I don't think LoanRepository needs access to IO
    // Are there cases where it would not be able to
    // sequence code over ConnectionIO?
    val repo    = LoanRepository.make[IO]
    val partyId = repo.newLoan(Loan.Type.ConsumerCredit,
                               Contract.Id.fromNakedInt(3), 
                               Account.Id.fromNakedInt(2), 
                               Currency.Id.fromNakedInt(1), 
                               LocalDateTime.now,
                               100000)
    val result  = partyId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}