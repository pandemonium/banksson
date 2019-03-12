package banksson
package domain

import doobie.{ Get, Put }
import io.circe._


trait Currencies { module: Identifiers =>
  object Currency extends EntityModule {
    case class T(name: String)
  }

  implicit def decodeCurrencyId: Decoder[Currency.Id] =
    Currency.Id.deriveDecoder

  implicit val getCurrencyId: Get[Currency.Id] = 
    Currency.Id.deriveGet

  implicit val putCurrencyId: Put[Currency.Id] =
    Currency.Id.derivePut
}