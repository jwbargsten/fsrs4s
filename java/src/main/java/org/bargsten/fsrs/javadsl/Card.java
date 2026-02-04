package org.bargsten.fsrs.javadsl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

public final class Card {
    private final org.bargsten.fsrs.Card underlying;

    private Card(org.bargsten.fsrs.Card underlying) {
        this.underlying = underlying;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Builder other) {
        if (other == null) {
            return new Builder();
        }
        return new Builder(other);
    }

    public static Builder builder(Card other) {
        if (other == null) {
            return new Builder();
        }
        return new Builder(other);
    }

    static Card wrap(org.bargsten.fsrs.Card card) {
        return new Card(card);
    }

    org.bargsten.fsrs.Card unwrap() {
        return underlying;
    }

    @SuppressWarnings("unchecked")
    public String id() {
        return (String) underlying.id();
    }

    public State state() {
        return Converters.toJava(underlying.state());
    }

    public OffsetDateTime due() {
        return underlying.due();
    }

    public double stability() {
        return underlying.stability();
    }

    public double difficulty() {
        return underlying.difficulty();
    }

    public int step() {
        return underlying.step();
    }

    public Optional<OffsetDateTime> lastReview() {
        return scala.jdk.javaapi.OptionConverters.toJava(underlying.lastReview());
    }

    public Optional<OffsetDateTime> firstReview() {
        return scala.jdk.javaapi.OptionConverters.toJava(underlying.firstReview());
    }

    public boolean isNew() {
        return underlying.isNew();
    }

    public static final class Builder {
        private String id = null;
        private State state = State.LEARNING;
        private OffsetDateTime due = OffsetDateTime.now(ZoneOffset.UTC);
        private double stability = 0.0;
        private double difficulty = 0.0;
        private int step = 0;
        private OffsetDateTime lastReview = null;
        private OffsetDateTime firstReview = null;

        private Builder() {}

        private Builder(Builder other) {
            this.id = other.id;
            this.state = other.state;
            this.due = other.due;
            this.stability = other.stability;
            this.difficulty = other.difficulty;
            this.step = other.step;
            this.lastReview = other.lastReview;
            this.firstReview = other.firstReview;
        }

        private Builder(Card other) {
            this.id = other.id();
            this.state = other.state();
            this.due = other.due();
            this.stability = other.stability();
            this.difficulty = other.difficulty();
            this.step = other.step();
            this.lastReview = other.lastReview().orElse(null);
            this.firstReview = other.firstReview().orElse(null);
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder state(State state) {
            this.state = state;
            return this;
        }

        public Builder due(OffsetDateTime due) {
            this.due = due;
            return this;
        }

        public Builder stability(double stability) {
            this.stability = stability;
            return this;
        }

        public Builder difficulty(double difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder step(int step) {
            this.step = step;
            return this;
        }

        public Builder lastReview(OffsetDateTime lastReview) {
            this.lastReview = lastReview;
            return this;
        }

        public Builder firstReview(OffsetDateTime firstReview) {
            this.firstReview = firstReview;
            return this;
        }

        @SuppressWarnings("unchecked")
        public Card build() {
            var cardId = id != null ? id : UUID.randomUUID().toString();
            return new Card(new org.bargsten.fsrs.Card(
                cardId,
                Converters.toScala(state),
                due,
                stability,
                difficulty,
                step,
                lastReview != null ? scala.Some.apply(lastReview) : scala.None$.empty(),
                firstReview != null ? scala.Some.apply(firstReview) : scala.None$.empty()
            ));
        }
    }
}
