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
       io.circe.syntax._
import java.time._


/**
 * Transforms some Algebra into a stream of events, writing them
 * to an Event Log.
 * 
 * Applies the events to aggregates in an effect monad F, composed with
 * writes to the Event Log to make possible some transaction sematic.
 */
trait EventSourceUniverse {
  type Algebra[_]
  type Event[_]
  type F[_]

  implicit val F: Monad[F]

  type AlgebraC[A] = EitherK[Algebra, EventLog.T, A]

  type AlgebraF[A] = Free[AlgebraC, A]

  implicit def encodeEvent[A]: Encoder[Event[A]]

  def execute[A](program: Algebra[A]): Event[A]

  def applyEvent[A](event: Event[A]): F[A]

  def writeEventLog(data: Json): F[Unit]


  object EventLog {
    sealed trait T[A]
    case class Append(e: Event[_])
      extends T[Unit]

    def append(e: Event[_]): AlgebraF[Unit] =
      EventLog.Append(e).injected
  }

  object interpreters {
    def appendEvent[A](x: EventLog.T[A]): F[A] = x match {
      case EventLog.Append(e) =>
        writeEventLog(e.asJson)
    }

    val eventLogInterpreter: EventLog.T ~> F =
      FunctionK.lift(appendEvent)

    def injectEventLog[A](program: Algebra[A]): AlgebraF[A] = for {
      _ <- EventLog.append(execute(program))
      c <- program.injected[AlgebraC]
    } yield c

    val withEventLog: Algebra ~> AlgebraF =
      FunctionK.lift(injectEventLog)

    def run[A](program: Algebra[A]): F[A] =
      applyEvent(execute(program))

    // public
    val algebraInterpreter: Algebra ~> F =
      FunctionK.lift(run)

    def interpretAlgebra[A](program: Algebra[A]): F[A] =
      withEventLog(program).foldMap(algebraInterpreter or eventLogInterpreter)

    // public
    val coreInterpreter: Algebra ~> F =
      FunctionK.lift(interpretAlgebra)
  }
}

object RunEventSourcing extends IOApp {
  import com.typesafe.config._
  import domain.model._
  import java.util.UUID

  object Command {
    sealed trait T[A]
    case class CreateParty(id: UUID,
                         name: String)
      extends T[Unit]

    def createParty(id: UUID,
                  name: String): CommandQuery.F[Unit] =
      CreateParty(id, name).injected
  }

  object Query {
    sealed trait T[A]
    case object ConjureId
      extends T[UUID]
    case class PartyById(id: UUID)
      extends T[Option[Party.T]]

    def conjureId: CommandQuery.F[UUID] =
      ConjureId.injected

    def partyById(id: UUID): CommandQuery.F[Option[Party.T]] =
      PartyById(id).injected

    def interpreter(r: Repositories): Query.T ~> ConnectionIO = {
      def interpret[A](q: T[A]): ConnectionIO[A] = q match {
        case ConjureId =>
          Async[ConnectionIO].delay(UUID.randomUUID)

        case PartyById(id) =>
          r.parties
           .partyById(Party.Id.fromNakedValue(id))
      }

      FunctionK.lift(interpret)
    }
  }

  object CommandQuery {
    type C[A] = EitherK[Command.T, Query.T, A]
    type F[A] = Free[C, A]

    def interpreter(u: Universe): C ~> ConnectionIO =
      u.interpreters.coreInterpreter or Query.interpreter(u.repositories)
  }

  object Event {
    import io.circe.generic.extras._
    import io.circe.generic.extras.{ Configuration => Config, _ }
  
    sealed trait T[A]
      extends Signature

    case class DidCreateParty(id: UUID, 
                            name: String)
        extends Implementation.Template
           with T[Unit]

    sealed trait Signature {
      // def id: Int
      // def at: LocalDateTime
      def void: T[Unit]
    }

    object Implementation {
      sealed abstract class Template { self: T[Unit] =>
        def void: T[Unit] = self
      }
    }

    implicit val config = 
      Config.default
            .withDiscriminator("type")

    // Why is .void : T[Unit] and not : T[A] ?
    implicit def encodeEvent[A]: Encoder[T[A]] = 
      semiauto.deriveEncoder[T[Unit]]
              .contramap(_.void)
  }

  case class Universe(repositories: Repositories) extends EventSourceUniverse {
    type Event[A]   = Event.T[A]
    type Algebra[A] = Command.T[A]
    type F[A]       = ConnectionIO[A]

    implicit val F: Monad[F] = Monad[F]

    def encodeEvent[A]: Encoder[Event[A]] =
      Encoder[Event.T[A]]

    // there's no concept of a current state and state data here
    // ... is that a problem?
    // The return value would have to be monadic in order for 
    // anything to accumulate or happen there.
    // I couldn't even generate an id here for the events since
    // there's no F[_] to bind that in.
    def execute[A](program: Algebra[A]): Event[A] = program match {
      case Command.CreateParty(id, name) =>
        Event.DidCreateParty(id, name)
    }

    // should this be `persist` instead?
    def applyEvent[A](e: Event[A]): F[A] = e match {
      case Event.DidCreateParty(id, name) =>
        repositories.parties
                    .newParty(Party.Id.fromNakedValue(id),
                              Party.Type.PrivateIndividual,
                              name)
    }

    def writeEventLog(data: Json): F[Unit] =
      repositories.events
                  .newEventRecord(data)
  }

  def run(args: List[String]): IO[ExitCode] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val repos = assembleRepositories
    val xa = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    val universe = Universe(repos)
    val program: CommandQuery.F[Unit] = for {
      // Can NewId be a query?
      // Or is it a new, other, category?
      // WHY NOT? x     <- Async[CommandQuery.F].pure(id)
      id    <- Query.conjureId
      _     <- Command.createParty(id, "Algot Enkelsten")
      party <- Query.partyById(id)
    } yield ()

    val xx =
    program.foldMap(CommandQuery.interpreter(universe))

         xx.transact(xa)
           .unsafeRunSync
  
    IO(ExitCode.Success)  
  }
}