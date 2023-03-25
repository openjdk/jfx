package test.javafx.beans.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import javafx.beans.value.Subscription;

public class SubscriptionTest {

    @Test
    void ofShouldReturnSubscriptionWhichCanCancelAllGivenSubscriptions() {
        AtomicReference<String> a = new AtomicReference<>();
        AtomicReference<String> b = new AtomicReference<>();
        AtomicReference<String> c = new AtomicReference<>();

        Subscription subscription = Subscription.of(
            () -> a.set("canceled"),
            () -> b.set("canceled"),
            () -> c.set("canceled")
        );

        assertNull(a.get());
        assertNull(b.get());
        assertNull(c.get());

        subscription.unsubscribe();

        assertEquals("canceled", a.get());
        assertEquals("canceled", b.get());
        assertEquals("canceled", c.get());
    }

    @Test
    void ofShouldRejectNulls() {
        assertThrows(NullPointerException.class, () -> Subscription.of((Subscription[]) null));
        assertThrows(NullPointerException.class, () -> Subscription.of((Subscription) null));
        assertThrows(NullPointerException.class, () -> Subscription.of(Subscription.EMPTY, null, () -> {}));
    }

    @Test
    void andShouldReturnSubscriptionWhichCanCancelBothSubscriptions() {
        AtomicReference<String> a = new AtomicReference<>();
        AtomicReference<String> b = new AtomicReference<>();

        Subscription subscription1 = () -> a.set("canceled");
        Subscription subscription2 = () -> b.set("canceled");

        Subscription combined = subscription1.and(subscription2);

        assertNull(a.get());
        assertNull(b.get());

        combined.unsubscribe();

        assertEquals("canceled", a.get());
        assertEquals("canceled", b.get());
    }

    @Test
    void andShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> Subscription.EMPTY.and(null));
    }
}
