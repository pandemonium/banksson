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
import java.util.UUID


object PaymentStructureRepository {
  import domain.model._

  sealed trait T
    extends Signature

  trait Signature {
    // These names are way too long; change `newPaymentStructure` to
    // something like `make` (and: `makeTerm` for instance.)

    // Maybe a PaymentStructure should work on a Contract instead?
    def newPaymentStructure(id: PaymentStructure.Id,
                        `type`: PaymentStructure.Type.T,
                        loanId: Loan.Id): ConnectionIO[Unit]

    def addPaymentStructureTerm(structureId: PaymentStructure.Id,
                                     termId: PaymentStructure.Term.Id,
                                     `type`: PaymentStructure.Term.Type.T,
                               appliesAfter: Option[Period],
                                 appliesFor: Option[Period],
                                      value: Int): ConnectionIO[Unit]
  }

  // This thing could just aswell take the Transactor
  def make: T = Implementation.make

  object Implementation {
    import org.postgresql.util.PGInterval

    /**
     * This Period <=> PGInterval code is a PoS.
     */

    def makePGIntervalFromPeriod(p: Period): PGInterval = {
      val interval = new PGInterval

      interval.setYears(p.getYears)
      interval.setMonths(p.getMonths)
      interval.setDays(p.getDays)

      interval
    }

    def makePeriodFromPGInterval(i: PGInterval): Period =
      Period.ofYears(i.getYears)
            .withMonths(i.getMonths)
            .withDays(i.getDays)

    implicit val getPeriod: Get[Period] = 
      Get[PGInterval].tmap(makePeriodFromPGInterval)

    implicit val putPeriod: Put[Period] =
      Put[PGInterval].tcontramap(makePGIntervalFromPeriod)

    private[Implementation]
    trait Template { self: Signature =>
      def typeId(`type`: PaymentStructure.Type.T): ConnectionIO[UUID] = sql"""
        SELECT
            pst.id
          FROM payment_structure_type pst
          WHERE
            pst.name = ${`type`}
      """.query[UUID]
         .unique

      def insertNew(id: PaymentStructure.Id,
                typeId: UUID, 
                loanId: Loan.Id): ConnectionIO[Unit] = sql"""
        INSERT
          INTO 
            payment_structure (id, type_id, loan_id)
          VALUES
            ($id, $typeId, $loanId)
      """.update
         .run
         .void

      // does it return ConnectionIO[A] or F[A]
      def newPaymentStructure(id: PaymentStructure.Id,
                          `type`: PaymentStructure.Type.T,
                          loanId: Loan.Id): ConnectionIO[Unit] = for {
        typeId <- typeId(`type`)
        _      <- insertNew(id, typeId, loanId)
      } yield ()

      def termTypeId(`type`: PaymentStructure.Term.Type.T): ConnectionIO[UUID] = sql"""
        SELECT
            pstt.id
          FROM payment_structure_term_type pstt
          WHERE
            pstt.name = ${`type`}
      """.query[UUID]
         .unique

      def insertNewTerm(structureId: PaymentStructure.Id,
                             termId: PaymentStructure.Term.Id,
                         termTypeId: UUID,
                       appliesAfter: Option[Period],
                         appliesFor: Option[Period],
                              value: Int): ConnectionIO[Unit] = 
        sql"""
          INSERT
            INTO 
              payment_structure_term (
                payment_structure_id,
                id,
                type_id,
                applies_after,
                applies_for,
                value
              )
            VALUES (
              $structureId,
              $termId,
              $termTypeId,
              $appliesAfter,
              $appliesFor,
              $value
            )
        """.update
           .run
           .void

      def addPaymentStructureTerm(structureId: PaymentStructure.Id,
                                       termId: PaymentStructure.Term.Id,
                                       `type`: PaymentStructure.Term.Type.T,
                                 appliesAfter: Option[Period],
                                   appliesFor: Option[Period],
                                        value: Int): ConnectionIO[Unit] = for {
        termTypeId <- termTypeId(`type`)
        _          <- insertNewTerm(structureId, termId, termTypeId, appliesAfter, appliesFor, value)
      } yield ()
    }

    class Repository
      extends T
         with Template

    def make: T = new Repository
  }
}

object RunPaymentStructureRepository extends IOApp {
  import com.typesafe.config._
  import domain.model._

  def run(args: List[String]): IO[ExitCode] = {
    val xa = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    // I don't think PaymentStructureRepository needs access to IO
    // Are there cases where it would not be able to
    // sequence code over ConnectionIO?
    val repo = PaymentStructureRepository.make

    val paymentStructureId = for {
      id     <- Async[ConnectionIO].delay(PaymentStructure.Id.fromNakedValue(UUID.randomUUID))
      tid1   <- Async[ConnectionIO].delay(PaymentStructure.Term.Id.fromNakedValue(UUID.randomUUID))
      tid2   <- Async[ConnectionIO].delay(PaymentStructure.Term.Id.fromNakedValue(UUID.randomUUID))
      loanId <- Async[ConnectionIO].delay(Loan.Id.fromNakedValue(UUID.randomUUID))
      _      <- repo.newPaymentStructure(id, PaymentStructure.Type.AnnuityLoan, 
                                         loanId)
      _    <- repo.addPaymentStructureTerm(
                id,
                tid1, 
                PaymentStructure.Term.Type.InterestRate,
                Period.ZERO.some,
                Option.empty,
                550)  // 5,5% as basis points
      _    <- repo.addPaymentStructureTerm(
                id,
                tid2,
                PaymentStructure.Term.Type.AnnuityPayment,
                Period.ZERO.some,
                Option.empty,
                1270) // 1270 currency
    } yield id

    val result             = paymentStructureId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}