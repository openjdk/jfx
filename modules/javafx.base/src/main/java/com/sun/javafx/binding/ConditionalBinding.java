package com.sun.javafx.binding;

import java.util.Objects;

import javafx.beans.value.ObservableValue;

public class ConditionalBinding<T> extends LazyObjectBinding<T> {

    private final ObservableValue<T> source;
    private final ObservableValue<Boolean> nonNullCondition;

    private Subscription subscription;

    public ConditionalBinding(ObservableValue<T> source, ObservableValue<Boolean> condition) {
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.nonNullCondition = Objects.requireNonNull(condition, "condition cannot be null").orElse(false);

        // condition is always observed and never unsubscribed
        Subscription.subscribe(nonNullCondition, current -> {
            invalidate();

            if (!current) {
                getValue();
            }
        });
    }

    /**
     * This binding is valid whenever it is observed, or it is currently inactive.
     * When inactive, the binding has the value of its source at the time it became
     * inactive.
     */
    @Override
    protected boolean allowValidation() {
        return super.allowValidation() || !isActive();
    }

    @Override
    protected T computeValue() {
        if (isObserved() && isActive()) {
            if (subscription == null) {
                subscription = Subscription.subscribeInvalidations(source, this::invalidate);
            }
        }
        else {
            unsubscribe();
        }

        return source.getValue();
    }

    @Override
    protected Subscription observeSources() {
        return this::unsubscribe;
    }

    private boolean isActive() {
        return nonNullCondition.getValue();
    }

    private void unsubscribe() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }
}
