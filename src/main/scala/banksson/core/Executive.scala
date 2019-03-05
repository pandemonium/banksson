package banksson
package core

import cats._,
       cats.arrow._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.free._,
       cats.effect._
import doobie._,
       doobie.implicits._
import io.circe._,
       io.circe.syntax._,
       io.circe.java8.time._
import java.time._,
       java.util.UUID
import domain.model._


trait Commands { module: Processes =>
  object Command {
    sealed trait T[A]
    case class CreateParty(id: UUID,
                       `type`: Party.Type.T,
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
    
    case class CreateProduct(id: UUID, 
                         `type`: Product.Type.T, 
                           name: String)
      extends T[Unit]

    def createParty(id: UUID, 
                `type`: Party.Type.T,
                  name: String): Process.F[Unit] =
      CreateParty(id, `type`, name).inject

    def createContract(id: UUID,
                   `type`: Contract.Type.T,
                  product: Product.Id,
                validFrom: LocalDate,
             validThrough: LocalDate): Process.F[Unit] =
      CreateContract(id, `type`, product, validFrom, validThrough).inject

    def addContractParty(contractId: UUID,
                               role: Party.Role.T,
                            partyId: Party.Id,
                              share: Option[Double]): Process.F[Unit] =
      AddContractParty(contractId, role, partyId, share).inject

    def createAccount(id: UUID, 
                  `type`: Account.Type.T, 
                    name: String): Process.F[Unit] =
      CreateAccount(id, `type`, name).inject

    def createLoan(id: UUID,
               `type`: Loan.Type.T,
             contract: Contract.Id,
      loanReceivables: Account.Id,
             currency: Currency.Id,
            createdAt: LocalDateTime,
            principal: Int): Process.F[Unit] =
      CreateLoan(id, 
                 `type`, 
                 contract, 
                 loanReceivables, 
                 currency, 
                 createdAt, 
                 principal).inject

    def createProduct(id: UUID, 
                  `type`: Product.Type.T,
                    name: String): Process.F[Unit] =
      CreateProduct(id, `type`, name).inject
  }
}


// The queries have to be extensible. How?
trait Queries { module: Processes =>
  object Query {
    sealed trait T[A]

    case object ConjureId
      extends T[UUID]

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

    def conjureId: Process.F[UUID] =
      ConjureId.inject

    def partyById(id: UUID): Process.F[Option[Party.T]] =
      PartyById(id).inject

    def contractById(id: UUID): Process.F[Option[Contract.T]] =
      ContractById(id).inject

    // Can I have sub-interpreters?
    // I could just use an extractor, no?
    def interpret(repositories: Repositories): Query.T ~> ConnectionIO = {
      def runQuery[A](q: Query.T[A]): ConnectionIO[A] = q match {
        case Query.ConjureId =>
          Async[ConnectionIO].delay(UUID.randomUUID)

        case Query.PartyById(id) =>
          repositories.parties
                      .partyById(Party.Id.fromNakedValue(id))

        case Query.ContractById(id) =>
          repositories.contracts
                      .contractById(Contract.Id.fromNakedValue(id))
      }

      FunctionK.lift(runQuery)
    }
  }
}


trait Events {
  import io.circe.generic.extras._,
         io.circe.generic.extras.{ Configuration => Config }

  object Event {
    sealed trait T[A]
      extends Signature

    case class DidCreateParty(id: UUID,
                          `type`: Party.Type.T,
                            name: String)
      extends T[Unit]
         with Implementation.Template

    case class DidCreateAccount(id: UUID, 
                            `type`: Account.Type.T, 
                              name: String)
      extends T[Unit]
         with Implementation.Template

    case class DidCreateContract(id: UUID,
                             `type`: Contract.Type.T,
                            product: Product.Id,
                          validFrom: LocalDate,
                       validThrough: LocalDate)
      extends T[Unit]
         with Implementation.Template

    case class DidAddContractParty(contractId: UUID,
                                         role: Party.Role.T,
                                      partyId: Party.Id,
                                        share: Option[Double])
      extends T[Unit]
         with Implementation.Template

    case class DidCreateLoan(id: UUID,
                         `type`: Loan.Type.T,
                       contract: Contract.Id,
                loanReceivables: Account.Id,
                       currency: Currency.Id,
                      createdAt: LocalDateTime,
                      principal: Int)
      extends T[Unit]
         with Implementation.Template

    case class DidCreateProduct(id: UUID, 
                            `type`: Product.Type.T, 
                              name: String)
      extends T[Unit]
         with Implementation.Template

    sealed trait Signature {
      // def id: Int
      // def at: LocalDateTime
      def void: T[Unit]
    }

    object Implementation {
      sealed trait Template { self: T[Unit] =>
        def void: T[Unit] = self
      }
    }

    implicit val config = 
      Config.default
            .withDiscriminator("event-type")

    // Accept T forall A, contramap it onto T at Unit
    // which has a known Encoder.
    implicit def encodeEvent[A]: Encoder[T[A]] = 
      semiauto.deriveEncoder[T[Unit]]
              .contramap(_.void)
  }
}


trait Processes { module: Commands with Queries =>
  object Process {
    type T[A] = EitherK[Command.T, Query.T, A]
    type F[A] = Free[T, A]
  }
}

object model extends AnyRef
  with Processes
  with Commands
  with Queries
  with Events

object Executive {
  import model._

  sealed trait T
    extends Signature

  trait Signature {
    def run[A](program: Process.F[A]): ConnectionIO[A]
  }

  def make(repositories: Repositories): T =
    Implementation.make(repositories)

  
  object Implementation {

    private[Implementation]
    case class Universe(r: Repositories) extends EventSourceUniverse {
      type Event[A]   = Event.T[A]
      type Algebra[A] = Command.T[A]

      def encodeEvent[A]: Encoder[Event[A]] =
        Encoder[Event.T[A]]

      // Can I replace all DidXxx wih a Did(command) ?
      // THIS...
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

      // ... AND THIS has to move out of `Implementation` and into
      // something that lives somewhere else.
      def applyEvent[A](e: Event[A]): ConnectionIO[A] = e match {
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

      def writeEventLog(data: Json): ConnectionIO[Unit] =
        r.events
         .newEventRecord(data)
    }

    trait Template { self: Signature =>
      def universe: Universe

      def run[A](program: Process.F[A]): ConnectionIO[A] =
        program.foldMap(processInterpreter)

      def processInterpreter: Process.T ~> ConnectionIO =
        universe.interpreters.coreInterpreter or Query.interpret(universe.r)
    }

    class Kernel(val universe: Universe)
      extends T
         with Template

    // Does it take Transactor too?
    def make(repositories: Repositories): T = 
      new Kernel(Universe(repositories))
  }
}

object TestJson extends App {
  import io.circe.generic.auto._
  import model._

  Event.DidCreateParty(UUID.randomUUID, Party.Type.PrivateIndividual, "Hi")
    .asJson

  Party.Id.fromNakedValue(UUID.randomUUID)
    .asJson
}

object RunExecutive extends IOApp {
  import com.typesafe.config._
  import model._

  def run(args: List[String]): IO[ExitCode] = {
    val xa =
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)
    val executive = Executive.make(assembleRepositories)

    val program = for {
      partyId    <- Query.conjureId
      _          <- Command.createParty(partyId, 
                                     Party.Type.PrivateIndividual, 
                                     "Ludvig Gislason")
      party      <- Query.partyById(partyId)

      productId  <- Query.conjureId
      _          <- Command.createProduct(productId, 
                                          Product.Type.AnnuityLoan,
                                          "Standard Loan")

      contractId <- Query.conjureId
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
    val result =
      executive.run(program)
               .transact(xa)
               .unsafeRunSync

    println(result)

    IO(ExitCode.Success)
  }
}