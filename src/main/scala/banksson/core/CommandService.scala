package banksson
package core

import cats._,
       cats.data._,
       cats.arrow._,
       cats.implicits._,
       cats.syntax._,
       cats.effect._,
       cats.free._
import doobie._,
       doobie.implicits._
import io.circe._,
       io.circe.syntax._
import java.util.UUID
import java.time._

import domain.model._

object Command {
  import domain.model._

  sealed trait T[A]

  // Type the primary key already here?
  case class CreateParty(id: UUID, 
                       name: String)
    extends T[Unit]

  case class CreateContract(id: UUID,
                        `type`: Contract.Type.T,
                       product: Product.Id,
                     validFrom: LocalDate,
                  validThrough: LocalDate)
    extends T[Unit]

  case class AddContractParty(contractId: UUID,
                                    role: Party.Role.T,
                                 partyId: Party.Id,
                                   share: Option[Double])
    extends T[Unit]

  case class CreateAccount(id: UUID, 
                       `type`: Account.Type.T, 
                         name: String)
    extends T[Unit]

  case class CreateLoan(id: UUID,
                    `type`: Loan.Type.T,
                  contract: Contract.Id,
           loanReceivables: Account.Id,
                  currency: Currency.Id,
                 createdAt: LocalDateTime,
                 principal: Int)
    extends T[Unit]
}

object Query {
  sealed trait T[A]

  case class PartyById(id: UUID)
    extends T[Option[Party.T]]
  case class PartyByName(name: String)
    extends T[Option[Party.T]]

  case class ContractById(id: UUID)
    extends T[Option[Contract.T]]
  
  case class AccountById(id: UUID)
    extends T[Option[Account.T]]

  case class LoanById(id: UUID)
    extends T[Option[Loan.T]]
}

object EventLog {
  sealed trait T[A]
  case class Append(event: Event.T[_])
    extends T[Unit]

  def append[G[_], A](event: Event.T[A])(implicit 
                       I: InjectK[EventLog.T, G]): Free[G, Unit] =
    Append(event).inject

  case class Interpreter[F[_]](r: Repositories[F])
      extends (EventLog.T ~> ConnectionIO) {
    def apply[A](e: EventLog.T[A]): ConnectionIO[A] = e match {
      case Append(e) =>
        // Ideally I would like type-of(e) to be an Event.Type.T
        // Can I do that?
        // I want the event-type to be an Int
        for {
          at <- Async[ConnectionIO].delay(LocalDateTime.now)
          _  <- r.events.newEventRecord(at, e.asInstanceOf[Event.T[Unit]])
        } yield ()
    }
  }
}

object Event {
  import io.circe.generic.extras.{ Configuration => Config, _ }

  sealed trait T[A]
  case class DidCreateParty(id: UUID,
                          name: String)
    extends T[Unit]
  case class DidCreateAccount(id: UUID)
    extends T[Unit]
  case class DidCreateContract(id: UUID)
    extends T[Unit]
  case class DidAddContractParty(id: UUID)
    extends T[Unit]
  case class DidCreateLoan(id: UUID)
    extends T[Unit]

  implicit val config = 
    Config.default
          .withDiscriminator("type")

  implicit def encodeEvent[A]: Encoder[T[Unit]] = 
    semiauto.deriveEncoder

  implicit def decodeEvent[A]: Decoder[T[Unit]] = 
    semiauto.deriveDecoder
}

object Bank {
  type Op[A] = EitherK[Command.T, Query.T, A]

  // I guess this should actually be:
  // Command.T ~> Event.T
  // What if one Event.T triggers new Command.T:s? Where does it put them?
  // Can or should it?
  // It must be allowed to issue Query.T:s because Command.T:s
  // can fail. Then what happens?
  case class CommandInterpreter[F[_]](r: Repositories[F])
      extends (Command.T ~> Event.T) {

    // This must call the command handler to make a List[Event.T]
    def apply[A](a: Command.T[A]): Event.T[A] = a match {
      case Command.CreateParty(id, name) =>
        Event.DidCreateParty(id, name)
    }
  }

  case class QueryInterpreter[F[_]](r: Repositories[F]) 
      extends (Query.T ~> ConnectionIO) {

    def apply[A](a: Query.T[A]): ConnectionIO[A] = a match {
      case Query.PartyById(id) =>
        r.parties.partyById(Party.Id.fromNakedValue(id))

      case x =>
        ???
    }
  }

  case class EventCapture[F[_]](r: Repositories[F])
      extends (Event.T ~> EventLog.T) {

    def apply[A](e: Event.T[A]): EventLog.T[A] = e match {
      case e1 @ Event.DidCreateParty(id, name) =>
        EventLog.Append(e1)
    }
  }

  case class EventInterpreter[F[_]](r: Repositories[F]) 
      extends (Event.T ~> ConnectionIO) {
    def apply[A](e: Event.T[A]): ConnectionIO[A] = e match {
      case Event.DidCreateParty(id, name) =>
        r.parties
         .newParty(Party.Id.fromNakedValue(id), 
                   Party.Type.PrivateIndividual,
                   name)

      case x =>
        ???
    }
  }

  type X[A] = Tuple2K[ConnectionIO, ConnectionIO, A]

  case class T2kIntp[F[_], A](r: Repositories[F])
      extends (X ~> ConnectionIO) {
    def apply[B](t: Tuple2K[ConnectionIO, ConnectionIO, B]): ConnectionIO[B] = 
      for {
        _ <- t.first
        b <- t.second
      } yield b
  }

  def createParty[G[_]](id: UUID,
                      name: String)(implicit 
                         I: InjectK[Command.T, G]): Free[G, Unit] =
    Command.CreateParty(id, name).inject

  def partyById[G[_]](id: UUID)(implicit 
                       I: InjectK[Query.T, G]): Free[G, Option[Party.T]] =
    Query.PartyById(id).inject
}

object RunCommandService extends IOApp {
  import com.typesafe.config._
  import domain.model._

  def run(args: List[String]): IO[ExitCode] = {
    val repos   = assembleRepositories[IO]
    val xa      = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    // Could possibly fix the G[_] to Bank.Op in Bank.
    // What about Event:s that want to issue commands?
    // What does that even mean?

    // A separate set of listeners that trigger needed commands?
    val pId = UUID.randomUUID
    val qId = UUID.randomUUID
    val program = for {
      _ <- Bank.createParty[Bank.Op](pId, "Björne Magazinsson")
      p <- Bank.partyById[Bank.Op](pId)
      _ <- Bank.createParty[Bank.Op](qId, "Eulalia II")
      q <- Bank.partyById[Bank.Op](qId)
    } yield (p, q)

    val a = Bank.CommandInterpreter(repos)          // Command => Event
    val b = a andThen Bank.EventCapture(repos)      // Event => EventLog
    val c = b andThen EventLog.Interpreter(repos)   // EventLog => ConnectionIO
    val d = a andThen Bank.EventInterpreter(repos)  // Event => ConnectionIO
    val e = c and d andThen Bank.T2kIntp(repos)     // (ConnectionIO, ConnectionIO) => ConnectionIO

    val commandIntp = (Bank.CommandInterpreter(repos) 
        andThen Bank.EventInterpreter(repos))

    val interpreter: Bank.Op ~> ConnectionIO = 
      e or Bank.QueryInterpreter(repos)

    val result = program.foldMap(interpreter)
                        .transact(xa)
                        .unsafeRunSync

    println(result)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}