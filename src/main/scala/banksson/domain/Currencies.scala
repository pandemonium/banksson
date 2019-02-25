package banksson
package domain

import doobie.{ Get, Put }


trait Currencies { module: Identifiers =>
  object Currency extends EntityModule {
    case class T(name: String)
  }

  implicit val getCurrencyId: Get[Currency.Id] = 
    Currency.Id.deriveGet

  implicit val putCurrencyId: Put[Currency.Id] =
    Currency.Id.derivePut
}