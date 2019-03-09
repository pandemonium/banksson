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


object InvoiceRepository {
  import domain.model._

  sealed trait T
    extends Signature

  trait Signature {
    def byId(id: Invoice.Id): ConnectionIO[Option[Invoice.T]]

    def newInvoice(id: Invoice.Id,
              stateId: Invoice.State.Id,
          createdDate: LocalDate,
              dueDate: LocalDate,
             sellerId: Party.Id,
              buyerId: Party.Id,
            reference: String): ConnectionIO[Unit]

    def addItem(invoiceId: Invoice.Id,
               contractId: Contract.Id,
                  ordinal: Int,
                   amount: Int,
                      vat: Int,
              description: Option[String]): ConnectionIO[Unit]

    def changeState(invoiceId: Invoice.Id,
               invoiceStateId: Invoice.State.Id,
                           to: Invoice.State.Type.T,
                           at: LocalDateTime,
                        notes: Option[String]): ConnectionIO[Unit]  
  }

  def make: T = Implementation.make

  object Implementation {
    private[Implementation]
    trait Template { self: Signature =>
      def typeId(`type`: Invoice.State.Type.T): ConnectionIO[UUID] = sql"""
        SELECT
            ist.id
          FROM invoice_state_type ist
          WHERE
            ist.name = ${`type`}
      """.query[UUID]
         .unique

      def insertNew(id: Invoice.Id,
           createdDate: LocalDate,
               dueDate: LocalDate,
              sellerId: Party.Id,
               buyerId: Party.Id,
             reference: String): ConnectionIO[Unit] = sql"""
        INSERT
          INTO 
            invoice (id, created_date, due_date, seller_id, buyer_id, reference)
          VALUES
            ($id, $createdDate, $dueDate, $sellerId, $buyerId, $reference)
      """.update
         .run
         .void

    def insertItem(invoiceId: Invoice.Id,
                  contractId: Contract.Id,
                     ordinal: Int,
                      amount: Int, vat: Int,
                 description: Option[String]): ConnectionIO[Unit] = sql"""
      INSERT
        INTO
          invoice_item (invoice_id, contract_id, amount, vat, ordinal, description)
        VALUES
          ($invoiceId, $contractId, $amount, $vat, $ordinal, $description)
    """.update
       .run
       .void

    def addItem(invoiceId: Invoice.Id,
               contractId: Contract.Id,
                  ordinal: Int,
                   amount: Int, vat: Int,
              description: Option[String]): ConnectionIO[Unit]  = for {
        _ <- insertItem(invoiceId, 
                        contractId, 
                        ordinal, 
                        amount, vat, 
                        description)
      } yield ()

      def insertState(invoiceId: Invoice.Id,
                 invoiceStateId: Invoice.State.Id,
                    stateTypeId: UUID,
                             at: LocalDateTime,
                          notes: Option[String]): ConnectionIO[Unit] = sql"""
        INSERT
          INTO
            invoice_state (invoice_id, state_id, invoice_state_type_id, 
                           entered_at, notes)
          VALUES
            ($invoiceId, $invoiceStateId, $stateTypeId, $at, $notes)
      """.update
         .run
         .void

      def newInvoice(id: Invoice.Id,
                stateId: Invoice.State.Id,
            createdDate: LocalDate,
                dueDate: LocalDate,
               sellerId: Party.Id,
                buyerId: Party.Id,
              reference: String): ConnectionIO[Unit] = for {
        _           <- insertNew(id, 
                                 createdDate, dueDate, 
                                 sellerId, buyerId, 
                                 reference)
        stateTypeId <- typeId(Invoice.State.Type.New)

                       // is this really a job for the the repo?
        _           <- insertState(id, stateId, stateTypeId,
                                   createdDate.atStartOfDay, 
                                   Option.empty)
      } yield ()

      def changeState(invoiceId: Invoice.Id,
                 invoiceStateId: Invoice.State.Id,
                             to: Invoice.State.Type.T,
                             at: LocalDateTime,
                          notes: Option[String]): ConnectionIO[Unit] = for {
        stateTypeId <- typeId(to)
        _           <- insertState(invoiceId, invoiceStateId, stateTypeId, 
                                   at, notes)
      } yield ()

      def byId(id: Invoice.Id): ConnectionIO[Option[Invoice.T]] = sql"""
        WITH summed_invoices AS (
          SELECT
              i.created_date, i.due_date, i.seller_id, i.buyer_id, 
              SUM(ii.amount) AS amountTotal, SUM(ii.vat) AS vatTotal, 
              i.reference, i.id as invoice_id
            FROM invoice i
            JOIN invoice_item ii
              ON ii.invoice_id = i.id
            GROUP BY
              i.id
        )
        SELECT
            si.created_date, si.due_date, si.seller_id, si.buyer_id,
            si.amountTotal, si.vatTotal, si.reference, ist.name
          FROM invoice_state s
          JOIN summed_invoices si
            ON si.invoice_id = s.invoice_id
          JOIN invoice_state_type ist
            ON ist.id = s.invoice_state_type_id
          WHERE
            si.invoice_id = $id
          ORDER BY
            s.entered_at DESC
      """.query[Invoice.T]
         .to[List]
         .map(_.headOption)
    }

    class Repository
      extends T
         with Template

    def make: T = new Repository
  }
}

object RunInvoiceRepository extends IOApp {
  import com.typesafe.config._
  import domain.model._

  def run(args: List[String]): IO[ExitCode] = {
    val xa = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    val repo = InvoiceRepository.make

    val invoiceId = for {
      id          <- Async[ConnectionIO].delay(Invoice.Id.fromNakedValue(UUID.randomUUID))
      stateId0    <- Async[ConnectionIO].delay(Invoice.State.Id.fromNakedValue(UUID.randomUUID))
      stateId1    <- Async[ConnectionIO].delay(Invoice.State.Id.fromNakedValue(UUID.randomUUID))
      contractId0 <- Async[ConnectionIO].delay(Contract.Id.fromNakedValue(UUID.randomUUID))
      contractId1 <- Async[ConnectionIO].delay(Contract.Id.fromNakedValue(UUID.randomUUID))
      sellerId    <- Async[ConnectionIO].delay(Party.Id.fromNakedValue(UUID.randomUUID))
      buyerId     <- Async[ConnectionIO].delay(Party.Id.fromNakedValue(UUID.randomUUID))
      createdDate <- Async[ConnectionIO].delay(LocalDate.now)
      dueDate     <- Async[ConnectionIO].delay(LocalDate.now.plusDays(30))
      paidDate    <- Async[ConnectionIO].delay(LocalDateTime.now)

      _           <- repo.newInvoice(id, stateId0, 
                                     createdDate, dueDate, 
                                     sellerId, buyerId,
                                     "dragont")

      _           <- repo.addItem(id, contractId0, 1, 10000, 2000, 
                                  Option("Loan"))

      _           <- repo.addItem(id, contractId1, 1, 4250, 50, 
                                  Option("Insurance"))

      _           <- repo.changeState(id, stateId1, 
                                      Invoice.State.Type.Paid, paidDate, 
                                      Option(s"Pays #dragont"))

      invoice     <- repo.byId(id)

    } yield invoice

    val result     = invoiceId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}