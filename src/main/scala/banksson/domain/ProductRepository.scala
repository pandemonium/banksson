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


object ProductRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    def newProduct(id: Product.Id,
               `type`: Product.Type.T,
                 name: String): ConnectionIO[Unit]
  }

  // This thing could just aswell take the Transactor
  def make[F[_]: Async]: T[F] = 
    Implementation[F]

  object Implementation {

    private[Implementation]
    trait Template[F[_]] { self: Signature[F] =>
      def typeId(`type`: Product.Type.T): ConnectionIO[UUID] = sql"""
        SELECT
            pt.id
          FROM product_type pt
          WHERE
            pt.name = ${`type`}
      """.query[UUID]
         .unique

      def insertNew(id: Product.Id,
                typeId: UUID, 
                  name: String): ConnectionIO[Unit] = sql"""
        INSERT
          INTO
            product (id, product_type_id, name)
          VALUES
            ($id, $typeId, $name)
      """.update
         .run
         .void

      // does it return ConnectionIO[A] or F[A]
      def newProduct(id: Product.Id,
                 `type`: Product.Type.T,
                   name: String): ConnectionIO[Unit] = for {
        typeId <- typeId(`type`)
        _      <- insertNew(id, typeId, name)
      } yield ()
    }

    def apply[F[_]: Async]: T[F] =
      new Implementation.Repository[F]

    class Repository[F[_]]
      extends T[F]
         with Template[F]
  }
}

object RunProductRepository extends IOApp {
  import com.typesafe.config._
  import domain.model._

  def run(args: List[String]): IO[ExitCode] = {
    val xa = 
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)

    // I don't think ProductRepository needs access to IO
    // Are there cases where it would not be able to
    // sequence code over ConnectionIO?
    val repo    = ProductRepository.make[IO]
    val productId = for {
      id <- Async[ConnectionIO].delay(Product.Id.fromNakedValue(UUID.randomUUID))
      _  <- repo.newProduct(id, Product.Type.AnnuityLoan, 
                           "Paid down to 50%")
    } yield id

    val result  = productId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}