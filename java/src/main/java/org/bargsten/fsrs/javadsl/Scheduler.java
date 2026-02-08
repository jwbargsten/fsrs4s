package org.bargsten.fsrs.javadsl;

import org.bargsten.fsrs.Parameters$;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Random;

/**
 * FSRS scheduler for spaced repetition.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class Scheduler {
    private final org.bargsten.fsrs.Scheduler underlying;

    public Scheduler() {
        this.underlying = new org.bargsten.fsrs.Scheduler(
            new org.bargsten.fsrs.Parameters(
                Parameters$.MODULE$.defaultWeights(),
                0.9,
                (scala.collection.immutable.List) scala.jdk.javaapi.CollectionConverters.asScala(
                    java.util.List.of(
                        scala.jdk.javaapi.DurationConverters.toScala(java.time.Duration.ofMinutes(1)),
                        scala.jdk.javaapi.DurationConverters.toScala(java.time.Duration.ofMinutes(10))
                    )
                ).toList(),
                (scala.collection.immutable.List) scala.jdk.javaapi.CollectionConverters.asScala(
                    java.util.List.of(
                        scala.jdk.javaapi.DurationConverters.toScala(java.time.Duration.ofMinutes(10))
                    )
                ).toList(),
                36500,
                true
            ),
            new scala.util.Random(),
            Clock.systemUTC()
        );
    }

    public Scheduler(Parameters parameters) {
        this.underlying = new org.bargsten.fsrs.Scheduler(
            parameters.unwrap(),
            new scala.util.Random(),
            Clock.systemUTC()
        );
    }

    public Scheduler(Parameters parameters, Random random) {
        this.underlying = new org.bargsten.fsrs.Scheduler(
            parameters.unwrap(),
            new scala.util.Random(random.nextLong()),
            Clock.systemUTC()
        );
    }

    public Scheduler(Parameters parameters, Random random, Clock clock) {
        this.underlying = new org.bargsten.fsrs.Scheduler(
            parameters.unwrap(),
            new scala.util.Random(random.nextLong()),
            clock
        );
    }

    public ReviewResult reviewCard(Card card, Rating rating) {
        var result = underlying.review(card.unwrap(), Converters.toScala(rating));
        return new ReviewResult(
            Card.wrap(result._1()),
            ReviewLogEntry.wrap(result._2())
        );
    }

    public ReviewResult reviewCard(Card card, Review review) {
        var result = underlying.review(card.unwrap(), review.unwrap());
        return new ReviewResult(
            Card.wrap(result._1()),
            ReviewLogEntry.wrap(result._2())
        );
    }

    public double calcRetrievability(Card card) {
        return underlying.calcRetrievability(card.unwrap(), OffsetDateTime.now(Clock.systemUTC()));
    }

    public double calcRetrievability(Card card, OffsetDateTime now) {
        return underlying.calcRetrievability(card.unwrap(), now);
    }
}
