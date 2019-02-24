package banksson
package domain

import cats.effect._
import doobie._,
       doobie.implicits._


object AccountRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    def newAccount(`type`: Account.Type.T,
                     name: String): ConnectionIO[Account.Id]
  }

  // This thing could just aswell take the Transactor
  def make[F[_]: Async]: T[F] = 
    Implementation[F]

  object Implementation {

    private[Implementation]
    trait Template[F[_]] { self: Signature[F] =>
      def accountTypeId(`type`: Account.Type.T): ConnectionIO[Int] = sql"""
        SELECT
            at.id
          FROM account_type at
          WHERE
            at.name = ${`type`}
      """.query[Int]
         .unique

      def insertNew(accountTypeId: Int, 
                      accountName: String): ConnectionIO[Account.Id] = sql"""
        INSERT
          INTO 
            account (account_type_id, name)
          VALUES
            ($accountTypeId, $accountName)
      """.update
         .withUniqueGeneratedKeys[Account.Id]("id")

      // does it return ConnectionIO[A] or F[A]
      def newAccount(`type`: Account.Type.T,
                       name: String): ConnectionIO[Account.Id] = for {
        atId    <- accountTypeId(`type`)
        partyId <- insertNew(atId, name)
      } yield partyId
    }

    def apply[F[_]: Async]: T[F] =
      new Implementation.Repository[F]

    class Repository[F[_]]
      extends T[F]
         with Template[F]
  }
}

object RunAccountRepository extends IOApp {
  import com.typesafe.config._
  import domain.model._

  def run(args: List[String]): IO[ExitCode] = {
    val xa =
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    // I don't think AccountRepository needs access to IO
    // Are there cases where it would not be able to
    // sequence code over ConnectionIO?
    val repo      = AccountRepository.make[IO]
    val accountId = repo.newAccount(Account.Type.LoanReceivables, 
                                    "Some loan receivables")
    val result    = accountId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}