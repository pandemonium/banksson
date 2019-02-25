package banksson
package domain

import cats._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.effect._
import doobie._,
       doobie.implicits._,
       doobie.postgres._,
       doobie.postgres.implicits._
import java.time._,
       java.sql.Timestamp
import java.util.UUID


object LoanRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    def newLoan(id: Loan.Id,
            `type`: Loan.Type.T,
        contractId: Contract.Id,
   loanReceivables: Account.Id,
        currencyId: Currency.Id,
         createdAt: LocalDateTime,
         principal: Int): ConnectionIO[Unit]
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
      def typeId(`type`: Loan.Type.T): ConnectionIO[UUID] = sql"""
        SELECT
            lt.id
          FROM loan_type lt
          WHERE
            lt.name = ${`type`}
      """.query[UUID]
         .unique

      def insertNew(id: Loan.Id,
                typeId: UUID,
            contractId: Contract.Id,
       loanReceivables: Account.Id,
            currencyId: Currency.Id,
             createdAt: LocalDateTime,
             principal: Int): ConnectionIO[Unit] = sql"""
        INSERT
          INTO 
            loan (id,
                  account_id,
                  created_at, 
                  loan_type_id,
                  contract_id,
                  principal,
                  currency_id)
          VALUES
            ($id,
             $loanReceivables, 
             $createdAt,
             $typeId, 
             $contractId,
             $principal,
             $currencyId)
      """.update
         .run
         .void

      // does it return ConnectionIO[A] or F[A]
      def newLoan(id: Loan.Id,
              `type`: Loan.Type.T,
          contractId: Contract.Id,
     loanReceivables: Account.Id,
          currencyId: Currency.Id,
           createdAt: LocalDateTime,
           principal: Int): ConnectionIO[Unit] = for {
        typeId <- typeId(`type`)
        _      <- insertNew(id,
                            typeId, 
                            contractId, 
                            loanReceivables,
                            currencyId,
                            createdAt,
                            principal)
      } yield ()
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
    val partyId = for {
      id         <- Async[ConnectionIO].delay(Loan.Id.fromNakedValue(UUID.randomUUID))
      contractId <- Async[ConnectionIO].delay(Contract.Id.fromNakedValue(UUID.randomUUID))
      accountId  <- Async[ConnectionIO].delay(Account.Id.fromNakedValue(UUID.randomUUID))
      currencyId <- Async[ConnectionIO].delay(Currency.Id.fromNakedValue(UUID.randomUUID))
      _          <- repo.newLoan(id,
                                 Loan.Type.ConsumerCredit,
                                 contractId, 
                                 accountId, 
                                 currencyId, 
                                 LocalDateTime.now,
                                 100000)
    } yield id
    val result  = partyId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}