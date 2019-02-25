package banksson
package domain

import cats._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.effect._
import doobie._,
       doobie.implicits._,
       doobie.postgres._,
       doobie.postgres.implicits._
import java.util.UUID


object PartyRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    def partyById(id: Party.Id): ConnectionIO[Option[Party.T]]
    def newParty(id: Party.Id,
             `type`: Party.Type.T,
               name: String): ConnectionIO[Unit]
  }

  // This thing could just aswell take the Transactor
  def make[F[_]: Async]: T[F] = 
    Implementation[F]

  object Implementation {

    private[Implementation]
    trait Template[F[_]] { self: Signature[F] =>
      def typeId(`type`: Party.Type.T): ConnectionIO[UUID] = sql"""
        SELECT
            pt.id
          FROM party_type pt
          WHERE
            pt.name = ${`type`}
      """.query[UUID]
         .unique

      def insertNew(id: Party.Id,
                typeId: UUID, 
                  name: String): ConnectionIO[Unit] = sql"""
        INSERT
          INTO 
            party (id, party_type_id, name)
          VALUES
            ($id, $typeId, $name)
      """.update
         .run
         .void

      def partyById(id: Party.Id): ConnectionIO[Option[Party.T]] = sql"""
        SELECT
            pt.name, p.name
          FROM party p
          JOIN party_type pt
            ON pt.id = p.party_type_id
          WHERE
            p.id = $id
      """.query[Party.T]
         .option

      // does it return ConnectionIO[A] or F[A]
      def newParty(id: Party.Id,
               `type`: Party.Type.T,
                 name: String): ConnectionIO[Unit] = for {
        typeId    <- typeId(`type`)
        partyId <- insertNew(id, typeId, name)
      } yield ()
    }

    def apply[F[_]: Async]: T[F] =
      new Implementation.Repository[F]

    class Repository[F[_]]
      extends T[F]
         with Template[F]
  }
}

object RunPartyRepository extends IOApp {
  import com.typesafe.config._
  import domain.model._

  def run(args: List[String]): IO[ExitCode] = {
    val xa = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    // I don't think PartyRepository needs access to IO
    // Are there cases where it would not be able to
    // sequence code over ConnectionIO?
    val repo    = PartyRepository.make[IO]

    val partyBoy =
    for {
      id    <- Async[ConnectionIO].delay(Party.Id.fromNakedValue(UUID.randomUUID))
      _     <- repo.newParty(id, Party.Type.PrivateIndividual, 
                             "Tommy Andersson")
      party <- repo.partyById(id)
    } yield (id, party)

    val result  = partyBoy.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}