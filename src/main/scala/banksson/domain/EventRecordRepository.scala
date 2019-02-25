package banksson
package domain

import cats._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.effect._
import io.circe._,
       io.circe.syntax._
import doobie._,
       doobie.implicits._,
       doobie.postgres._,
       doobie.postgres.implicits._,
       doobie.postgres.circe.json.implicits._
import java.time._


object EventRecordRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    def newEventRecord(at: LocalDateTime,
                     data: Json): ConnectionIO[Unit]
  }

  // This thing could just aswell take the Transactor
  def make[F[_]: Async]: T[F] = 
    Implementation[F]

  object Implementation {
    import java.sql.Timestamp

    // Why is this necessary?
    // Extract to `AbstractImplementation` ?
    // Just import ImplementationSupport._ ?
    implicit val dateMeta: Meta[LocalDateTime] =
      Meta[Timestamp].xmap(_.toLocalDateTime, Timestamp.valueOf)

    private[Implementation]
    trait Template[F[_]] { self: Signature[F] =>
      def insertNew(at: LocalDateTime,
                  data: Json): ConnectionIO[Unit] = sql"""
        INSERT
          INTO
            event_log (at, data)
          VALUES
            ($at, $data)
      """.update
         .run
         .void

      // does it return ConnectionIO[A] or F[A]
      def newEventRecord(at: LocalDateTime,
                       data: Json): ConnectionIO[Unit] = 
        insertNew(at, data)
    }

    def apply[F[_]: Async]: T[F] =
      new Implementation.Repository[F]

    class Repository[F[_]]
      extends T[F]
         with Template[F]
  }
}

object RunEventRecordRepository extends IOApp {
  import com.typesafe.config._
  import domain.model._

  def run(args: List[String]): IO[ExitCode] = {
    val xa = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    // I don't think EventRecordRepository needs access to IO
    // Are there cases where it would not be able to
    // sequence code over ConnectionIO?
    val repo    = EventRecordRepository.make[IO]
    val program = repo.newEventRecord(LocalDateTime.now, "Hello".asJson)
    val result  = program.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}