# fsrs4s

Scala 3 implementation of [FSRS-6](https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm) (Free Spaced Repetition Scheduler).

Scala implementation of [py-fsrs](https://github.com/open-spaced-repetition/py-fsrs).
The code is close to the python version, but I improved the interface to have a more
idiomatic Scala feeling.

## Usage

```scala
import org.bargsten.fsrs.*

val scheduler = Scheduler()
val card = Card.create()

// Review a card
val (updatedCard, reviewLog) = scheduler.review(card, Rating.Good)

// Next review is scheduled at:
updatedCard.due
```

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
