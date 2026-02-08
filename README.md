# fsrs4s

Scala 3 implementation of
[FSRS-6](https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm) (Free
Spaced Repetition Scheduler).

Based on [py-fsrs](https://github.com/open-spaced-repetition/py-fsrs), with an idiomatic
Scala interface.

## Installation

<!-- include example/project.scala::dependencies -->
```scala
// Scala CLI
//> using dep org.bargsten::fsrs4s:0.1.3
// sbt
// "org.bargsten" %% "fsrs4s" % "0.1.3"
```
<!-- endinclude -->

## Usage

<!-- include example/main.scala::example -->
```scala
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
```
<!-- endinclude -->

## Build

```bash
sbt compile   # Compile
sbt test      # Run tests
sbt console   # REPL
```

## References

- [FSRS Algorithm Wiki](https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm)
- [Technical explanation of FSRS](https://expertium.github.io/Algorithm.html)
- [Anki FSRS Deck Options](https://docs.ankiweb.net/deck-options.html#fsrs)
