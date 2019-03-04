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

  sealed trait T
    extends Signature

  trait Signature {
    def newEventRecord[E: Encoder](at: LocalDateTime,
                                 data: E): ConnectionIO[Unit]
  }

  // This thing could just aswell take the Transactor
  def make: T = Implementation.make

  object Implementation {
    import java.sql.Timestamp

    // Why is this necessary?
    // Extract to `AbstractImplementation` ?
    // Just import ImplementationSupport._ ?
    implicit val dateMeta: Meta[LocalDateTime] =
      Meta[Timestamp].xmap(_.toLocalDateTime, Timestamp.valueOf)

    private[Implementation]
    trait Template { self: Signature =>
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
      def newEventRecord[E: Encoder](at: LocalDateTime,
                                   data: E): ConnectionIO[Unit] = 
        insertNew(at, data.asJson)
    }

    class Repository
      extends T
         with Template

    def make: T = new Repository
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
    val repo    = EventRecordRepository.make
    val program = repo.newEventRecord(LocalDateTime.now, "Hello".asJson)
    val result  = program.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}