package org.bargsten.fsrs.javadsl;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class Review {
    private final org.bargsten.fsrs.Review underlying;

    private Review(org.bargsten.fsrs.Review underlying) {
        this.underlying = underlying;
    }

    public static Review of(Rating rating) {
        return new Review(new org.bargsten.fsrs.Review(
            Converters.toScala(rating),
            OffsetDateTime.now(ZoneOffset.UTC),
            scala.None$.empty()
        ));
    }

    public static Review of(Rating rating, OffsetDateTime reviewedAt) {
        return new Review(new org.bargsten.fsrs.Review(
            Converters.toScala(rating),
            reviewedAt,
            scala.None$.empty()
        ));
    }

    public static Review of(Rating rating, OffsetDateTime reviewedAt, Duration reviewDuration) {
        return new Review(new org.bargsten.fsrs.Review(
            Converters.toScala(rating),
            reviewedAt,
            scala.Option.apply(Converters.toScala(reviewDuration))
        ));
    }

    org.bargsten.fsrs.Review unwrap() {
        return underlying;
    }

    public Rating rating() {
        return Converters.toJava(underlying.rating());
    }

    public OffsetDateTime reviewedAt() {
        return underlying.reviewedAt();
    }
}
