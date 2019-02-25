package banksson
package domain

import io.circe._
import java.time._


trait EventRecords {
  object EventRecord {
    // Ought it have an id column?
    case class T(recordedAt: LocalDateTime,
                      event: Json)
  }
}