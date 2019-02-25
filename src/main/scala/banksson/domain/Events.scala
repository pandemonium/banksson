package banksson
package domain

import io.circe._
import java.time._


trait EventRecords {
  object EventRecord {
    case class T(recordedAt: LocalDateTime,
                      event: Json)
  }
}