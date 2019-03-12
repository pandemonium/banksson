package banksson

import cats._,
       cats.arrow._,
       cats.data._,
       cats.implicits._,
       cats.syntax._,
       cats.free._,
       cats.effect._

import core._,
       core.process._,
       core.executive._,
       domain.model._

object Main extends IOApp {
  import com.typesafe.config._

  def run(args: List[String]): IO[ExitCode] = {
    val databaseTransactor =
      core.Database.readConnectionSpec("database.master")
                   .map(core.DatabaseTransactor.make[IO])
                   .run(ConfigFactory.load)
    val repositories = assembleRepositories
    val context      = Executive.Context[IO](
      databaseTransactor,
      repositories,
      DatabaseAggregateWriter.make(repositories),
      DatabaseAggregateReader.make(repositories),
      Journal.make(repositories.events))
    val executive    = Executive.make(context)

    val task = for {
      partyId    <- Query.generateId
      _          <- Command.createParty(partyId,
                                        Party.Type.PrivateIndividual,
                                        "Herr Doktor Segersparre")
      party      <- Query.partyById(partyId)
    } yield (partyId, party)

    val result = executive.run(task).unsafeRunSync
    println(result)

    IO(ExitCode.Success)
  }
}