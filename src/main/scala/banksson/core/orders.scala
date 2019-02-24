package banksson
package core


object Order {
  sealed trait T
  case class Loan()
}