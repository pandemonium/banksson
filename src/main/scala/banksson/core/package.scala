package banksson

import cats._,
       cats.data._,
       cats.syntax._,
       cats.implicits._,
       cats.effect._
import com.typesafe.config._
import doobie._
import shapeless.tag,
       shapeless.tag._

package object core {       
  import cats.free._

  implicit class FreeOps[F[_], A](val fa: F[A]) extends AnyVal {
    def liftF: Free[F, A] = 
      Free.liftF(fa)

    def inject[G[_]](implicit I: InjectK[F, G]): Free[G, A] =
      Free.inject(fa)
  }

  object Configuration {
    // type C[F[_], A] = Kleisli[C, Config, A] ?
    type Configured[A] = Reader[Config, A]

    def read[A](f: Config => A): Configured[A] =
      Reader(f)
  }

  object Database {
    trait Flavour {
      type T
      type Signature[F[_]] = Transactor[F] @@ T

      // s/make/apply/ ?
      def make[F[_]: Async: ContextShift](spec: ConnectionSpec): Signature[F] =
        tag[T][Transactor[F]](
          Transactor.fromDriverManager(spec.driverName,
                                       spec.url,
                                       spec.username,
                                       spec.password)
        )
    }

    case class ConnectionSpec(driverName: String,
                                     url: String,
                                username: String,
                                password: String)

    import Configuration._
    def readConnectionSpec(flavour: String): Configured[ConnectionSpec] = for {
      driverName <- read(_.getString(s"$flavour.driver-name"))
             url <- read(_.getString(s"$flavour.url"))
        username <- read(_.getString(s"$flavour.username"))
        password <- read(_.getString(s"$flavour.password"))
    } yield ConnectionSpec(driverName, url, username, password)
  }

  import domain._
  case class Repositories[F[_]](
             accounts: AccountRepository.T[F],
            contracts: ContractRepository.T[F],
               events: EventRecordRepository.T[F],
              parties: PartyRepository.T[F],
    paymentStructures: PaymentStructureRepository.T[F],
             products: ProductRepository.T[F],
                loans: LoanRepository.T[F],
  )

  def assembleRepositories[F[_]: Async]: Repositories[F] =
    Repositories(
      AccountRepository.make[F],
      ContractRepository.make[F],
      EventRecordRepository.make[F],
      PartyRepository.make[F],
      PaymentStructureRepository.make[F],
      ProductRepository.make[F],
      LoanRepository.make[F]
    )
}