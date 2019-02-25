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
  case class Append(at: LocalDateTime, 
                 event: Event.T)
    extends T[Unit]

  def append[G[_]](at: LocalDateTime, 
                event: Event.T)(implicit 
                    I: InjectK[EventLog.T, G]): Free[G, Unit] =
    Append(at, event).inject

  class Interpreter[F[_]](r: Repositories[F])
      extends (EventLog.T ~> ConnectionIO) {
    def apply[A](e: EventLog.T[A]): ConnectionIO[A] = e match {
      case Append(at, e) =>
        // Ideally I would like type-of(e) to be an Event.Type.T
        // Can I do that?
        // I want the event-type to be an Int
        r.events.newEventRecord(at, e.asJson)
    }
  }
}

object Event {
  import io.circe.generic.extras.{ Configuration => Config, _ }

  sealed trait T
  case class DidCreateParty(id: UUID,
                          name: String)
    extends T
  case class DidCreateAccount(id: UUID)
    extends T
  case class DidCreateContract(id: UUID)
    extends T
  case class DidAddContractParty(id: UUID)
    extends T
  case class DidCreateLoan(id: UUID)
    extends T

  implicit val config = 
    Config.default
          .withDiscriminator("type")

  implicit val encodeEvent: Encoder[T] = 
    semiauto.deriveEncoder

  implicit val decodeEvent: Decoder[T] = 
    semiauto.deriveDecoder
}

object Bank {
  type Op[A] = EitherK[Command.T, Query.T, A]

  case class CommandInterpreter[F[_]](r: Repositories[F])
      extends (Command.T ~> ConnectionIO) {

    // This must call the command handler to make a List[Event.T]
    def apply[A](a: Command.T[A]): ConnectionIO[A] = a match {
      case Command.CreateParty(id, name) =>
        r.parties
         .newParty(Party.Id.fromNakedValue(id), 
                   Party.Type.PrivateIndividual, 
                   name)

      case x =>
        ???
    }
  }

  case class QueryInterpreter[F[_]](r: Repositories[F]) 
      extends (Query.T ~> ConnectionIO) {

    def apply[A](a: Query.T[A]): ConnectionIO[A] = a match {
      case Query.PartyById(id) =>
        r.parties.partyById(Party.Id.fromNakedValue(???))
    }
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

    val partyId = UUID.randomUUID

    // Could possibly fix the G[_] to Bank.Op in Bank.
    val program = for {
      _ <- Bank.createParty[Bank.Op](partyId, "Efraim Fralla")
      p <- Bank.partyById[Bank.Op](partyId)
    } yield p

    val interpreter: Bank.Op ~> ConnectionIO = 
      Bank.CommandInterpreter(repos) or
      Bank.QueryInterpreter(repos)

    val result = program.foldMap(interpreter)
                        .transact(xa)
                        .unsafeRunSync

    println(result)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}