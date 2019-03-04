package banksson
package core
package events

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


trait EventUniverse {
  type Event[_]
  type Algebra[_]

  type AlgebraC[A] = EitherK[Algebra, EventLog.T, A]
  type AlgebraF[A] = Free[AlgebraC, A]

  implicit def encodeEvent[A]: Encoder[Event[A]]

  def execute[A](program: Algebra[A]): Event[A]

  def applyEvent[A](event: Event[A]): ConnectionIO[A]

  object EventLog {
    sealed trait T[A]
    case class Append[A](e: Event[A])
      extends T[Unit]

    def append(e: Event[_]): AlgebraF[Unit] =
      Free.inject[EventLog.T, AlgebraC](EventLog.Append(e))
  }

  case class Interpreters(r: core.Repositories) {
    def appendEvent[A](x: EventLog.T[A]): ConnectionIO[A] = x match {
      case EventLog.Append(e) =>
        for {
          at <- Async[ConnectionIO].delay(LocalDateTime.now)
          _  <- r.events.newEventRecord(at, e.asJson)
        } yield ()
    }

    val eventLogInterpreter: EventLog.T ~> ConnectionIO =
      FunctionK.lift(appendEvent)

    def injectEventLog[A](program: Algebra[A]): AlgebraF[A] = for {
      _ <- EventLog.append(execute(program))
      c <- Free.inject[Algebra, AlgebraC](program)
    } yield c

    val logged: Algebra ~> AlgebraF =
      FunctionK.lift(injectEventLog)

    def run[A](program: Algebra[A]): ConnectionIO[A] =
      applyEvent(execute(program))

    val algebraInterpreter: Algebra ~> ConnectionIO =
      FunctionK.lift(run)

    def interpretAlgebra[A](program: Algebra[A]): ConnectionIO[A] =
      logged(program).foldMap(algebraInterpreter or eventLogInterpreter)

    val coreInterpreter: Algebra ~> ConnectionIO =
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
      Free.inject[Command.T, CommandQuery.C](CreateParty(id, name))
  }

  object Query {
    sealed trait T[A]
    case class PartyById(id: UUID)
      extends T[Option[Party.T]]

    def partyById(id: UUID): CommandQuery.F[Option[Party.T]] =
      Free.inject[Query.T, CommandQuery.C](PartyById(id))

    def interpreter(r: Repositories): Query.T ~> ConnectionIO = {
      def interpret[A](q: T[A]): ConnectionIO[A] = q match {
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
    case class DidCreateParty(id: UUID, 
                            name: String)
      extends T[Unit]

    implicit val config = 
      Config.default
            .withDiscriminator("type")

    // Jesus Harold Christ.
    implicit def encodeEvent[A]: Encoder[T[A]] = 
      semiauto.deriveEncoder[T[Unit]]
              .contramap(_.asInstanceOf[T[Unit]])
  }

  case class Universe(repositories: Repositories) extends EventUniverse {
    type Event[A]   = Event.T[A]
    type Algebra[A] = Command.T[A]

    def encodeEvent[A]: Encoder[Event[A]] =
      Encoder[Event.T[A]]

    def execute[A](program: Algebra[A]): Event[A] = program match {
      case Command.CreateParty(id, name) =>
        Event.DidCreateParty(id, name)
    }

    def applyEvent[A](e: Event[A]): ConnectionIO[A] = e match {
      case Event.DidCreateParty(id, name) =>
        repositories.parties
                    .newParty(Party.Id.fromNakedValue(id),
                              Party.Type.PrivateIndividual,
                              name)
    }

    val interpreters: Interpreters = 
      Interpreters(repositories)
  }

  def run(args: List[String]): IO[ExitCode] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val repos = assembleRepositories
    val xa = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    val universe = Universe(repos)
    val id       = UUID.randomUUID
    val program  = for {
      // WHY NOT? x     <- Async[CommandQuery.F].pure(id)
      _     <- Command.createParty(id, "Pär Mauliz")
      party <- Query.partyById(id)
    } yield (id, party)

    val x =
    program.foldMap(CommandQuery.interpreter(universe))
           .transact(xa)
           .unsafeRunSync
  
    IO(ExitCode.Success)  
  }
}