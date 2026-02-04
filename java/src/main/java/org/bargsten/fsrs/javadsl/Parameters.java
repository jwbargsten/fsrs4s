package org.bargsten.fsrs.javadsl;

import org.bargsten.fsrs.Parameters$;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class Parameters {
    private final org.bargsten.fsrs.Parameters underlying;

    private Parameters(org.bargsten.fsrs.Parameters underlying) {
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

    public static Builder builder(Parameters other) {
        if (other == null) {
            return new Builder();
        }
        return new Builder(other);
    }

    org.bargsten.fsrs.Parameters unwrap() {
        return underlying;
    }

    public List<Double> weights() {
        var scalaSeq = underlying.weights();
        var result = new ArrayList<Double>();
        var it = scalaSeq.iterator();
        while (it.hasNext()) {
            result.add((Double) it.next());
        }
        return result;
    }

    public double desiredRetention() {
        return underlying.desiredRetention();
    }

    public int maximumInterval() {
        return (Integer) underlying.maximumInterval();
    }

    public boolean withFuzzing() {
        return underlying.withFuzzing();
    }

    public static List<Double> defaultWeights() {
        var scalaSeq = Parameters$.MODULE$.defaultWeights();
        var result = new ArrayList<Double>();
        var it = scalaSeq.iterator();
        while (it.hasNext()) {
            result.add((Double) it.next());
        }
        return result;
    }

    public static double defaultDecay() {
        return Parameters$.MODULE$.defaultDecay();
    }

    public static final class Builder {
        private List<Double> weights = null;
        private double desiredRetention = 0.9;
        private List<Duration> learningSteps = List.of(Duration.ofMinutes(1), Duration.ofMinutes(10));
        private List<Duration> relearningSteps = List.of(Duration.ofMinutes(10));
        private int maximumInterval = 36500;
        private boolean withFuzzing = true;

        private Builder() {}

        private Builder(Builder other) {
            this.weights = other.weights != null ? new ArrayList<>(other.weights) : null;
            this.desiredRetention = other.desiredRetention;
            this.learningSteps = new ArrayList<>(other.learningSteps);
            this.relearningSteps = new ArrayList<>(other.relearningSteps);
            this.maximumInterval = other.maximumInterval;
            this.withFuzzing = other.withFuzzing;
        }

        private Builder(Parameters other) {
            this.weights = other.weights();
            this.desiredRetention = other.desiredRetention();
            this.learningSteps = new ArrayList<>(scala.jdk.javaapi.CollectionConverters.asJava(
                other.underlying.learningSteps().map(Converters::toJava)
            ));
            this.relearningSteps = new ArrayList<>(scala.jdk.javaapi.CollectionConverters.asJava(
                other.underlying.relearningSteps().map(Converters::toJava)
            ));
            this.maximumInterval = other.maximumInterval();
            this.withFuzzing = other.withFuzzing();
        }

        public Builder weights(List<Double> weights) {
            this.weights = weights;
            return this;
        }

        public Builder desiredRetention(double desiredRetention) {
            this.desiredRetention = desiredRetention;
            return this;
        }

        public Builder learningSteps(List<Duration> learningSteps) {
            this.learningSteps = learningSteps;
            return this;
        }

        public Builder relearningSteps(List<Duration> relearningSteps) {
            this.relearningSteps = relearningSteps;
            return this;
        }

        public Builder maximumInterval(int maximumInterval) {
            this.maximumInterval = maximumInterval;
            return this;
        }

        public Builder withFuzzing(boolean withFuzzing) {
            this.withFuzzing = withFuzzing;
            return this;
        }

        public Parameters build() {
            var scalaWeights = weights != null
                ? (scala.collection.immutable.Seq) scala.jdk.javaapi.CollectionConverters.asScala(weights).toSeq()
                : Parameters$.MODULE$.defaultWeights();

            var scalaLearningSteps = (scala.collection.immutable.List) scala.jdk.javaapi.CollectionConverters.asScala(
                learningSteps.stream().map(Converters::toScala).toList()
            ).toList();

            var scalaRelearningSteps = (scala.collection.immutable.List) scala.jdk.javaapi.CollectionConverters.asScala(
                relearningSteps.stream().map(Converters::toScala).toList()
            ).toList();

            return new Parameters(new org.bargsten.fsrs.Parameters(
                scalaWeights,
                desiredRetention,
                scalaLearningSteps,
                scalaRelearningSteps,
                maximumInterval,
                withFuzzing
            ));
        }
    }
}
