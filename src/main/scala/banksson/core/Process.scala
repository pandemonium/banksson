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
      CreateParty(id, `type`, name).injected

    def createContract(id: UUID,
                   `type`: Contract.Type.T,
                  product: Product.Id,
                validFrom: LocalDate,
             validThrough: LocalDate): Process.F[Unit] =
      CreateContract(id, `type`, product, validFrom, validThrough).injected

    def addContractParty(contractId: UUID,
                               role: Party.Role.T,
                            partyId: Party.Id,
                              share: Option[Double]): Process.F[Unit] =
      AddContractParty(contractId, role, partyId, share).injected

    def createAccount(id: UUID, 
                  `type`: Account.Type.T, 
                    name: String): Process.F[Unit] =
      CreateAccount(id, `type`, name).injected

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
                 principal).injected

    def createProduct(id: UUID, 
                  `type`: Product.Type.T,
                    name: String): Process.F[Unit] =
      CreateProduct(id, `type`, name).injected
  }
}


// The queries have to be extensible. How?
trait Queries { module: Processes =>

  object Query {
    sealed trait T[A]

    case object GenerateId
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

    def generateId: Process.F[UUID] =
      GenerateId.injected

    def partyById(id: UUID): Process.F[Option[Party.T]] =
      PartyById(id).injected

    def contractById(id: UUID): Process.F[Option[Contract.T]] =
      ContractById(id).injected
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