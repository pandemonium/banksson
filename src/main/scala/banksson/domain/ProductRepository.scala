package banksson
package domain

import cats._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.effect._
import doobie._,
       doobie.implicits._


object ProductRepository {
  import domain.model._

  sealed trait T[F[_]]
    extends Signature[F]

  trait Signature[F[_]] {
    def newProduct(`type`: Product.Type.T,
                     name: String): ConnectionIO[Product.Id]
  }

  // This thing could just aswell take the Transactor
  def make[F[_]: Async]: T[F] = 
    Implementation[F]

  object Implementation {

    private[Implementation]
    trait Template[F[_]] { self: Signature[F] =>
      def productTypeId(`type`: Product.Type.T): ConnectionIO[Int] = sql"""
        SELECT
            pt.id
          FROM product_type pt
          WHERE
            pt.name = ${`type`}
      """.query[Int]
         .unique

      def insertNew(productTypeId: Int, 
                      productName: String): ConnectionIO[Product.Id] = sql"""
        INSERT
          INTO
            product (product_type_id, name)
          VALUES
            ($productTypeId, $productName)
      """.update
         .withUniqueGeneratedKeys[Product.Id]("id")

      // does it return ConnectionIO[A] or F[A]
      def newProduct(`type`: Product.Type.T,
                       name: String): ConnectionIO[Product.Id] = for {
        ptId      <- productTypeId(`type`)
        productId <- insertNew(ptId, name)
      } yield productId
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
    val productId = repo.newProduct(Product.Type.AnnuityLoan, 
                                    "Paid down to 50%")
    val result  = productId.transact(xa)
    println(result.unsafeRunSync)

    // Make Process, such as: InterestChargeProcess, 
    // ... RequestIngestionProcess, etc

    IO(ExitCode.Success)
  }
}