// :snx example
import org.bargsten.fsrs.*

import java.time.ZoneId

@main
def main() =
  val scheduler = Scheduler()
  // create initial card with random ID
  val card = Card()

  // Review a card
  val (updatedCard, reviewLog) = scheduler.review(card, Rating.Good)
  // datetimes are in OffsetDateTime(UTC)
  val due = updatedCard.due.atZoneSameInstant(ZoneId.of("Europe/Amsterdam"))

  println(s"Next review is scheduled at: ${due}")
// :xns
