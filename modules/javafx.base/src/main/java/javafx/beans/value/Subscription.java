package javafx.beans.value;

import java.util.Objects;

/**
 * A subscription encapsulates how to cancel it without having
 * to keep track of how it was created.<p>
 *
 * For example:<p>
 * <pre>Subscription s = property.subscribe(System.out::println)</pre>
 * The function passed in to {@code subscribe} does not need to be stored
 * in order to clean up the subscription later.
 */
public interface Subscription {

    /**
     * An empty subscription. Does nothing when cancelled.
     */
    static final Subscription EMPTY = () -> {};

    /**
     * Cancels this subscription.
     */
    void unsubscribe();

    /**
     * Combines this {@link Subscription} with the given {@code Subscription}
     * and returns a new {@code Subscription} which will cancel both when
     * cancelled.
     *
     * @param other another {@link Subscription}, cannot be null
     * @return a combined {@link Subscription} which will cancel both when
     *   cancelled, never null
     */
    default Subscription and(Subscription other) {
      Objects.requireNonNull(other);

      return () -> {
        unsubscribe();
        other.unsubscribe();
      };
    }
}
