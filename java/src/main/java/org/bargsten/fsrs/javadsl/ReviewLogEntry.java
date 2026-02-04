package org.bargsten.fsrs.javadsl;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

public final class ReviewLogEntry {
    private final org.bargsten.fsrs.ReviewLogEntry underlying;

    private ReviewLogEntry(org.bargsten.fsrs.ReviewLogEntry underlying) {
        this.underlying = underlying;
    }

    static ReviewLogEntry wrap(org.bargsten.fsrs.ReviewLogEntry entry) {
        return new ReviewLogEntry(entry);
    }

    @SuppressWarnings("unchecked")
    public String cardId() {
        return (String) underlying.cardId();
    }

    public Rating rating() {
        return Converters.toJava(underlying.rating());
    }

    public OffsetDateTime reviewedAt() {
        return underlying.reviewedAt();
    }

    public Optional<Duration> duration() {
        var opt = scala.jdk.javaapi.OptionConverters.toJava(underlying.duration());
        return opt.map(d -> Duration.ofNanos(d.toNanos()));
    }
}
