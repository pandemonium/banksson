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


object PaymentStructureRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    // These names are way too long; change `newPaymentStructure` to
    // something like `make` (and: `makeTerm` for instance.)

    // Maybe a PaymentStructure should work on a Contract instead?
    def newPaymentStructure(`type`: PaymentStructure.Type.T,
                            loanId: Loan.Id): ConnectionIO[PaymentStructure.Id]

    def addPaymentStructureTerm(structureId: PaymentStructure.Id,
                                     `type`: PaymentStructure.Term.Type.T,
                               appliesAfter: Option[Period],
                                 appliesFor: Option[Period],
                                      value: Int): ConnectionIO[Unit]
  }

  // This thing could just aswell take the Transactor
  def make[F[_]: Async]: T[F] = 
    Implementation[F]

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
    trait Template[F[_]] { self: Signature[F] =>
      def paymentStructureTypeId(`type`: PaymentStructure.Type.T): ConnectionIO[Int] = sql"""
        SELECT
            pst.id
          FROM payment_structure_type pst
          WHERE
            pst.name = ${`type`}
      """.query[Int]
         .unique

      def insertNew(paymentStructureType: Int, 
                                  loanId: Loan.Id): ConnectionIO[PaymentStructure.Id] = sql"""
        INSERT
          INTO 
            payment_structure (type_id, loan_id)
          VALUES
            ($paymentStructureType, $loanId)
      """.update
         .withUniqueGeneratedKeys[PaymentStructure.Id]("id")

      // does it return ConnectionIO[A] or F[A]
      def newPaymentStructure(`type`: PaymentStructure.Type.T,
                              loanId: Loan.Id): ConnectionIO[PaymentStructure.Id] = for {
        pstId              <- paymentStructureTypeId(`type`)
        paymentStructureId <- insertNew(pstId, loanId)
      } yield paymentStructureId

      def paymentStructureTermTypeId(`type`: PaymentStructure.Term.Type.T): ConnectionIO[Int] = sql"""
        SELECT
            pstt.id
          FROM payment_structure_term_type pstt
          WHERE
            pstt.name = ${`type`}
      """.query[Int]
         .unique

      def insertNewTerm(structureId: PaymentStructure.Id,
             paymentStructureTypeId: Int,
                       appliesAfter: Option[Period],
                         appliesFor: Option[Period],
                              value: Int): ConnectionIO[PaymentStructure.Id] = 
        sql"""
          INSERT
            INTO 
              payment_structure_term (
                payment_structure_id,
                type_id,
                applies_after,
                applies_for,
                value
              )
            VALUES (
              $structureId,
              $paymentStructureTypeId,
              $appliesAfter,
              $appliesFor,
              $value
            )
        """.update
           .withUniqueGeneratedKeys[PaymentStructure.Id]("id")

      def addPaymentStructureTerm(structureId: PaymentStructure.Id,
                                       `type`: PaymentStructure.Term.Type.T,
                                 appliesAfter: Option[Period],
                                   appliesFor: Option[Period],
                                        value: Int): ConnectionIO[Unit] = for {
        psttid <- paymentStructureTermTypeId(`type`)
        _      <- insertNewTerm(structureId, psttid, appliesAfter, appliesFor, value)
      } yield ()
    }

    def apply[F[_]: Async]: T[F] =
      new Implementation.Repository[F]

    class Repository[F[_]]
      extends T[F]
         with Template[F]
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
    val repo = PaymentStructureRepository.make[IO]

    val paymentStructureId = for {
      psId <- repo.newPaymentStructure(PaymentStructure.Type.AnnuityLoan, Loan.Id.fromNakedInt(1))
      _    <- repo.addPaymentStructureTerm(
                psId, 
                PaymentStructure.Term.Type.InterestRate,
                Period.ZERO.some,
                Option.empty,
                550)  // 5,5% as basis points
      _    <- repo.addPaymentStructureTerm(
                psId, 
                PaymentStructure.Term.Type.AnnuityPayment,
                Period.ZERO.some,
                Option.empty,
                1270) // 1270 currency
    } yield psId

    val result             = paymentStructureId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}