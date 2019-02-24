package banksson
package domain

import cats._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.effect._
import doobie._,
       doobie.implicits._


object PartyRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    def newParty(`type`: Party.Type.T,
                   name: String): ConnectionIO[Party.Id]
  }

  // This thing could just aswell take the Transactor
  def make[F[_]: Async]: T[F] = 
    Implementation[F]

  object Implementation {

    private[Implementation]
    trait Template[F[_]] { self: Signature[F] =>
      def partyTypeId(`type`: Party.Type.T): ConnectionIO[Int] = sql"""
        SELECT
            pt.id
          FROM party_type pt
          WHERE
            pt.name = ${`type`}
      """.query[Int]
         .unique

      def insertNew(partyTypeId: Int, 
                      partyName: String): ConnectionIO[Party.Id] = sql"""
        INSERT
          INTO 
            party (party_type_id, name)
          VALUES
            ($partyTypeId, $partyName)
      """.update
         .withUniqueGeneratedKeys[Party.Id]("id")

      // does it return ConnectionIO[A] or F[A]
      def newParty(`type`: Party.Type.T,
                     name: String): ConnectionIO[Party.Id] = for {
        ptId    <- partyTypeId(`type`)
        partyId <- insertNew(ptId, name)
      } yield partyId
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
    val partyId = repo.newParty(Party.Type.PrivateIndividual, 
                                "Ludvig Gislason")
    val result  = partyId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}