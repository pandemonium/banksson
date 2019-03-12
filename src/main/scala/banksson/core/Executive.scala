package banksson
package core

import cats._,
       cats.arrow._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.free._,
       cats.effect._
import doobie.{ Query => _, _ },
       doobie.implicits._
import fs2.Stream
import io.circe._,
       io.circe.syntax._,
       io.circe.java8.time._
import java.time._,
       java.util.UUID

import domain.model._
import process._


trait AggregateWriters {
  object DatabaseAggregateWriter {
    sealed trait T
      extends Signature

    sealed trait Signature {
      def accumulate[A](e: Event.T[A]): ConnectionIO[A]
    }

    def make(r: Repositories): T = Implementation.make(r)


    private[DatabaseAggregateWriter]
    object Implementation {
      sealed abstract class Template { self: Signature =>
        def r: Repositories

        // Is this really what I want here?
        def accumulate[A](e: Event.T[A]): ConnectionIO[A] = e match {
          case Event.DidCreateParty(id, tpe, name) =>
            r.parties
             .newParty(Party.Id.fromNakedValue(id), tpe, name)

          case Event.DidCreateContract(id, tpe, product, validFrom, validThrough) =>
            r.contracts
             .newContract(Contract.Id.fromNakedValue(id),
                          tpe, product,
                          validFrom,
                          validThrough)

          case Event.DidCreateProduct(id, tpe, name) =>
            r.products
             .newProduct(Product.Id.fromNakedValue(id),
                         tpe,
                         name)
        }
      }

      class Writer(val r: Repositories)
        extends Template
           with T

      def make(r: Repositories): T = new Writer(r)
    }
  }
}

trait AggregateReaders {
  object DatabaseAggregateReader {

    sealed trait T
      extends Signature

    sealed trait Signature {
      def query[A](q: Query.T[A]): ConnectionIO[A]
    }

    def make(r: Repositories): T = Implementation.make(r)

    object Implementation {
      sealed abstract class Template { self: Signature =>
        def r: Repositories

        def query[A](e: Query.T[A]): ConnectionIO[A] =
          translate(e)

        def translate[A](q: Query.T[A]): ConnectionIO[A] = q match {
          case Query.GenerateId =>
            Async[ConnectionIO].delay(UUID.randomUUID)

          case Query.PartyById(id) =>
            r.parties
             .partyById(Party.Id.fromNakedValue(id))

          case Query.ContractById(id) =>
            r.contracts
             .contractById(Contract.Id.fromNakedValue(id))
        }
      }

      class Evaluator(val r: Repositories)
        extends Template
           with T

      def make(r: Repositories): T = new Evaluator(r)
    }
  }
}

// Really? I really needed 30 lines to save 2?
trait Journals {
  import domain._

  object Journal {
    sealed trait T
      extends Signature

    sealed trait Signature {
      def write[E: Encoder](data: E): ConnectionIO[Unit]
      def readAll[E: Decoder]: Stream[ConnectionIO, E]
    }

    def make(events: EventRecordRepository.T): T =
      Implementation.make(events)

    private[Journal]
    object Implementation {
      sealed trait Template { self: Signature =>
        def events: EventRecordRepository.T

        def write[E: Encoder](data: E): ConnectionIO[Unit] =
          events.newEventRecord(data)

        def readAll[E: Decoder]: Stream[ConnectionIO, E] =
          events.journalStream[E]
      }

      class Writer(val events: EventRecordRepository.T)
        extends Template
           with T

      def make(events: EventRecordRepository.T): T = new Writer(events)
    }
  }
}

trait CommandHandlers {
  object CommandHandler {
    sealed trait T
      extends Signature

    sealed trait Signature {

    }
  }
}

trait ExecutiveModule { module: AggregateWriters with AggregateReaders
                                                 with Journals  =>
  object Executive {
    case class Context[F[_]](xa: DatabaseTransactor.T[F],
                   repositories: Repositories,
                     aggregates: DatabaseAggregateWriter.T,
                         reader: DatabaseAggregateReader.T,
                        journal: Journal.T)

    sealed trait T[F[_]]
      extends Signature[F]

    trait Signature[F[_]] {
      def run[A](program: Process.F[A]): F[A]
    }

    def make[F[_]: Monad](context: Context[F]): T[F] =
      Implementation.make[F](context)


    object Implementation {

      private[Implementation]
      case class Universe[G[_]: Monad](context: Context[G]) extends EventSourceUniverse {
        type Event[A]   = Event.T[A]
        type Algebra[A] = Command.T[A]
        type F[A]       = G[A]

        val F = Monad[F]

        def encodeEvent[A]: Encoder[Event[A]] =
          Encoder[Event.T[A]]

        def decodeEvent[A]: Decoder[Event[A]] =
          Decoder[Event.T[A]]

        // Is this a task for the Executive?
        // Why is the event system even involved in taking commands
        // when it so obviously can only deal with events?
        def execute[A](program: Algebra[A]): Event[A] = program match {
          case Command.CreateParty(id, tpe, name) =>
            Event.DidCreateParty(id, tpe, name)

          case Command.CreateContract(id, tpe, product, validFrom, validThrough) =>
            Event.DidCreateContract(id, tpe, product, validFrom, validThrough)

          case Command.AddContractParty(contractId, role, partyId, share) =>
            Event.DidAddContractParty(contractId, role, partyId, share)

          case Command.CreateAccount(id, tpe, name) =>
            Event.DidCreateAccount(id, tpe, name)

          case Command.CreateLoan(id, tpe, contract, account, currency, created, principal) =>
            Event.DidCreateLoan(id, tpe, contract, account, currency, created, principal)

          case Command.CreateProduct(id, tpe, name) =>
            Event.DidCreateProduct(id, tpe, name)
        }

        // This pattern of .transact:ing everything is ... weird
        def applyEvent[A](e: Event[A]): F[A] =
          context.aggregates.accumulate(e)
                 .transact(context.xa)

        // This pattern of .transact:ing everything is ... weird
        def writeJournal[E: Encoder](data: E): F[Unit] =
          context.journal.write(data)
                 .transact(context.xa)

        // This pattern of .transact:ing everything is ... weird
        def readJournal[E: Decoder]: Stream[F, E] =
          context.journal.readAll[E]
                 .transact(context.xa)

        // This pattern of .transact:ing everything is ... weird
        // Couldn't ConnectionIO be the F here?
        // Look into that.
        def replayJournal: F[Unit] = {
          context.journal.readAll[Event[Unit]]
                 .map(context.aggregates.accumulate)
                 .compile.drain           // what is the correct way here to 
                 .transact(context.xa)    // do all the writes
                                          // but discard all output?
        }
      }

      sealed abstract class Template[F[_]: Monad] { self: Signature[F] =>
        def universe: Universe[F]

        def run[A](program: Process.F[A]): F[A] =
          program.foldMap(interpretProcess)

        def interpretProcess: Process.T ~> F =
          universe.interpreters.coreInterpreter or queryInterpreter

        def queryInterpreter: Query.T ~> F = new (Query.T ~> F) {
          def apply[A](q: Query.T[A]): F[A] =
              universe.context
                      .reader
                      .query(q)
                      .transact(universe.context.xa)
        }
      }


      class Kernel[F[_]: Monad](val universe: Universe[F])
        extends Template[F]
          with T[F]

      // Does it take Transactor too?
      def make[F[_]: Monad](context: Context[F]): T[F] =
        new Kernel[F](Universe[F](context))
    }
  }
}

object TestJson extends App {
  import io.circe.generic.auto._

  Event.DidCreateParty(UUID.randomUUID, Party.Type.PrivateIndividual, "Hi")
    .asJson

  Party.Id.fromNakedValue(UUID.randomUUID)
    .asJson
}

object RunExecutive extends IOApp {
  import com.typesafe.config._

  def run(args: List[String]): IO[ExitCode] = {
    val xa =
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)
//    val executive = Executive.make[IO](assembleRepositories)

    val program = for {
      partyId    <- Query.generateId
      _          <- Command.createParty(partyId,
                                        Party.Type.PrivateIndividual,
                                        "Ludvig Gislason")
      party      <- Query.partyById(partyId)

      productId  <- Query.generateId
      _          <- Command.createProduct(productId,
                                          Product.Type.AnnuityLoan,
                                          "Standard Loan")

      contractId <- Query.generateId
      _          <- Command.createContract(
                      contractId, 
                      Contract.Type.DebtObligation,
                      Product.Id.fromNakedValue(productId),
                      LocalDate.parse("2019-03-05"),
                      LocalDate.parse("2022-09-12")
                    )
      contract   <- Query.contractById(contractId)
    } yield (partyId, party, contractId, contract)

    // Should this really be returning ConnectionIO for the caller
    // to interpret/ transact.
/*    val result =
      executive.run(program)
               .unsafeRunSync

    println(result)
*/
    IO(ExitCode.Success)
  }
}