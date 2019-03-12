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
import fs2.Stream
import java.time._


object EventRecordRepository {
  import domain.model._

  sealed trait T
    extends Signature

  trait Signature {
    def newEventRecord[E: Encoder](data: E): ConnectionIO[Unit]
    def journalStream[E: Decoder]: Stream[ConnectionIO, E]
  }

  def make: T = Implementation.make

  object Implementation {
    private[Implementation]
    trait Template { self: Signature =>
      def insert(data: Json): ConnectionIO[Unit] = sql"""
        INSERT
          INTO
            event_log (data)
          VALUES
            ($data)
      """.update
         .run
         .void

      def newEventRecord[E: Encoder](data: E): ConnectionIO[Unit] = 
        insert(data.asJson)

      def journal: Stream[ConnectionIO, Json] = sql"""
        SELECT
            el.data
          FROM 
            event_log el
          ORDER BY
            el.id
      """.query[Json]
         .stream

      // non-decodeable Json blobs is quite serious here. What does it mean?
      // ... or is it? `E` is supplied by the caller.
      // Should this thing actually return Either instead?
      def journalStream[E: Decoder]: Stream[ConnectionIO, E] =
        journal.flatMap(_.as[E].fold(e => { throw new RuntimeException("hi") }, Stream.emit))
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
    val program = repo.newEventRecord("Hello".asJson)
    val result  = program.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}