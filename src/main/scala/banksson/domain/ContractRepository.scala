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
import java.time._
import java.sql.Date


object ContractRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    def newContract(`type`: Contract.Type.T,
                 productId: Product.Id,
                 validFrom: LocalDate,
              validThrough: LocalDate): ConnectionIO[Contract.Id]

    def addContractParty(contractId: Contract.Id,
                               role: Party.Role.T,
                            partyId: Party.Id,
                              share: Option[Double]): ConnectionIO[Unit]
  }

  // This thing could just aswell take the Transactor
  def make[F[_]: Async]: T[F] = 
    Implementation[F]

  object Implementation {

    private[Implementation]
    trait Template[F[_]] { self: Signature[F] =>
      def contractTypeId(`type`: Contract.Type.T): ConnectionIO[Int] = sql"""
        SELECT
            ct.id
          FROM contract_type ct
          WHERE
            ct.name = ${`type`}
      """.query[Int]
         .unique

      def partyRoleId(role: Party.Role.T): ConnectionIO[Int] = sql"""
        SELECT
            pr.id
          FROM party_role pr
          WHERE
            pr.name = $role
      """.query[Int]
         .unique

      def insertNew(contractTypeId: Int,
                         productId: Product.Id,
                         validFrom: LocalDate,
                      validThrough: LocalDate): ConnectionIO[Contract.Id] = 
        sql"""
          INSERT
            INTO
              contract (contract_type_id, product_id, valid_from, valid_through)
            VALUES
              ($contractTypeId, $productId, $validFrom, $validThrough)
        """.update
           .withUniqueGeneratedKeys[Contract.Id]("id")

      def insertNewContractParty(contractId: Contract.Id,
                                     roleId: Int,
                                    partyId: Party.Id,
                                      share: Option[Double]): ConnectionIO[Unit] =
        sql"""
          INSERT
            INTO
              contract_party (contract_id, party_role_id, party_id, share)
            VALUES
              ($contractId, $roleId, $partyId, $share)
        """.update
           .run
           .void

      // does it return ConnectionIO[A] or F[A]
      def newContract(`type`: Contract.Type.T,
                   productId: Product.Id,
                   validFrom: LocalDate,
                validThrough: LocalDate): ConnectionIO[Contract.Id] = for {
        ctId       <- contractTypeId(`type`)
        contractId <- insertNew(ctId, productId, validFrom, validThrough)
      } yield contractId

      def addContractParty(contractId: Contract.Id,
                                 role: Party.Role.T,
                              partyId: Party.Id,
                                share: Option[Double]): ConnectionIO[Unit] = 
        for {
          roleId <- partyRoleId(role)
          _      <- insertNewContractParty(contractId, roleId, partyId, share)
        } yield ()
    }

    def apply[F[_]: Async]: T[F] =
      new Implementation.Repository[F]

    class Repository[F[_]]
      extends T[F]
         with Template[F]
  }
}

object RunContractRepository extends IOApp {
  import com.typesafe.config._
  import domain.model._

  def run(args: List[String]): IO[ExitCode] = {
    val xa = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    val repo       = ContractRepository.make[IO]
    val now        = LocalDate.now
    val through    = now.plusYears(9)

    val contractId = for {
      cid <- repo.newContract(Contract.Type.DebtObligation,
                              Product.Id.fromNakedInt(1),
                              now, through)
      _   <- repo.addContractParty(cid, Party.Role.Lender,
                                   Party.Id.fromNakedInt(1),
                                   none)
      _   <- repo.addContractParty(cid, 
                                   Party.Role.Borrower, 
                                   Party.Id.fromNakedInt(2),
                                   1D.some)
      _   <- repo.addContractParty(cid,
                                   Party.Role.Borrower, 
                                   Party.Id.fromNakedInt(3),
                                   1D.some)
    } yield cid

    val result     = contractId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}