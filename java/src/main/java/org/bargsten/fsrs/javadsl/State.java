package org.bargsten.fsrs.javadsl;

public enum State {
    LEARNING(1),
    REVIEW(2),
    RELEARNING(3);

    private final int value;

    State(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
