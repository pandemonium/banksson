package banksson
package domain

trait Events {
  object Event {
    sealed trait T
    case class InvoicePayment()
      extends T

    def makeInvoicePayment(): T =
      ???
  }
}

trait Actions {
  object Action {
    sealed trait T

  }
}

object EventProcessor {
}