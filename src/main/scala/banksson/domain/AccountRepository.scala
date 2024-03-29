package banksson
package domain

import cats._,
       cats.data._,
       cats.syntax._,
       cats.implicits._,
       cats.effect._
import doobie._,
       doobie.implicits._,
       doobie.postgres._,
       doobie.postgres.implicits._
import java.util.UUID


object AccountRepository {
  import domain.model._

  sealed trait T
    extends Signature

  trait Signature {
    def newAccount(id: Account.Id,
               `type`: Account.Type.T,
                 name: String): ConnectionIO[Unit]
  }

  def make: T = Implementation.make

  object Implementation {

    private[Implementation]
    trait Template { self: Signature =>
      def typeId(`type`: Account.Type.T): ConnectionIO[UUID] = sql"""
        SELECT
            at.id
          FROM account_type at
          WHERE
            at.name = ${`type`}
      """.query[UUID]
         .unique

      def insert(id: Account.Id,
             typeId: UUID, 
               name: String): ConnectionIO[Unit] = sql"""
        INSERT
          INTO 
            account (id, account_type_id, name)
          VALUES
            ($id, $typeId, $name)
      """.update
         .run
         .void

      // does it return ConnectionIO[A] or F[A]
      def newAccount(id: Account.Id,
                 `type`: Account.Type.T,
                   name: String): ConnectionIO[Unit] = for {
        typeId <- typeId(`type`)
        _      <- insert(id, typeId, name)
      } yield ()
    }

    class Repository
      extends T
         with Template

    def make: T = new Repository
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
    val repo      = AccountRepository.make

    val accountId = for {
      id <- Async[ConnectionIO].delay(Account.Id.fromNakedValue(UUID.randomUUID))
      _  <- repo.newAccount(id, Account.Type.LoanReceivables, 
                            "Some loan receivables")
    } yield id
                    
    val result    = accountId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}