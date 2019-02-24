package banksson
package core

object Process {
  sealed trait T
  case class Ingest()
    extends T
  case class Process()
    extends T
  case class Emit()
    extends T
}

object Task {
  sealed trait T
  case class ReadTransactionLog()
    extends T
  case class ChargeInterest()
    extends T
}