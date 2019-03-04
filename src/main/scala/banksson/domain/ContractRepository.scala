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
import java.util.UUID


object ContractRepository {
  import domain.model._

  sealed trait T
    extends Signature

  trait Signature {
    def newContract(id: Contract.Id,
                `type`: Contract.Type.T,
             productId: Product.Id,
             validFrom: LocalDate,
          validThrough: LocalDate): ConnectionIO[Unit]

    def addContractParty(contractId: Contract.Id,
                               role: Party.Role.T,
                            partyId: Party.Id,
                              share: Option[Double]): ConnectionIO[Unit]
  }

  // This thing could just aswell take the Transactor
  def make: T = Implementation.make

  object Implementation {

    private[Implementation]
    trait Template { self: Signature =>
      def typeId(`type`: Contract.Type.T): ConnectionIO[UUID] = sql"""
        SELECT
            ct.id
          FROM contract_type ct
          WHERE
            ct.name = ${`type`}
      """.query[UUID]
         .unique

      def partyRoleId(role: Party.Role.T): ConnectionIO[UUID] = sql"""
        SELECT
            pr.id
          FROM party_role pr
          WHERE
            pr.name = $role
      """.query[UUID]
         .unique

      def insertNew(id: Contract.Id,
                typeId: UUID,
             productId: Product.Id,
             validFrom: LocalDate,
          validThrough: LocalDate): ConnectionIO[Unit] = 
        sql"""
          INSERT
            INTO
              contract (id, 
                        contract_type_id, 
                        product_id, 
                        valid_from, 
                        valid_through)
            VALUES
              ($id, $typeId, $productId, $validFrom, $validThrough)
        """.update
           .run
           .void

      def insertNewContractParty(contractId: Contract.Id,
                                     roleId: UUID,
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
      def newContract(id: Contract.Id,
                  `type`: Contract.Type.T,
               productId: Product.Id,
               validFrom: LocalDate,
            validThrough: LocalDate): ConnectionIO[Unit] = for {
        typeId <- typeId(`type`)
        _      <- insertNew(id, typeId, productId, validFrom, validThrough)
      } yield ()

      def addContractParty(contractId: Contract.Id,
                                 role: Party.Role.T,
                              partyId: Party.Id,
                                share: Option[Double]): ConnectionIO[Unit] = 
        for {
          roleId <- partyRoleId(role)
          _      <- insertNewContractParty(contractId, roleId, partyId, share)
        } yield ()
    }

    class Repository
      extends T
         with Template

    def make: T = new Repository
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

    val repo       = ContractRepository.make
    val now        = LocalDate.now
    val through    = now.plusYears(9)

    val contractId = for {
      id        <- Async[ConnectionIO].delay(Contract.Id.fromNakedValue(UUID.randomUUID))
      productId <- Async[ConnectionIO].delay(Product.Id.fromNakedValue(UUID.randomUUID))
      _  <- repo.newContract(id, Contract.Type.DebtObligation,
                             productId,
                             now, through)
      lenderPartyId <- Async[ConnectionIO].delay(
                         Party.Id.fromNakedValue(UUID.randomUUID)
                       )
      _  <- repo.addContractParty(id, Party.Role.Lender,
                                  lenderPartyId,
                                  none)
      borrowerPartyId <- Async[ConnectionIO].delay(
                           Party.Id.fromNakedValue(UUID.randomUUID)
                         )
      _  <- repo.addContractParty(id, 
                                   Party.Role.Borrower, 
                                   borrowerPartyId,
                                   1D.some)
      borrower2PartyId <- Async[ConnectionIO].delay(
                            Party.Id.fromNakedValue(UUID.randomUUID)
                          )
      _  <- repo.addContractParty(id,
                                   Party.Role.Borrower, 
                                   borrower2PartyId,
                                   1D.some)
    } yield id

    val result     = contractId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}