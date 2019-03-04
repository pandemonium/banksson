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

  case class QueryInterpreter(r: Repositories)
      extends (Query.T ~> ConnectionIO) {

    def apply[A](a: Query.T[A]): ConnectionIO[A] = a match {
      case Query.PartyById(id) =>
        r.parties.partyById(Party.Id.fromNakedValue(id))

      case x =>
        ???
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
    val repos   = assembleRepositories
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

    def interpreter: Bank.Op ~> ConnectionIO = ???
//      Bank.QueryInterpreter(repos)

    val result = program.foldMap(interpreter)
                        .transact(xa)
                        .unsafeRunSync

    println(result)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}