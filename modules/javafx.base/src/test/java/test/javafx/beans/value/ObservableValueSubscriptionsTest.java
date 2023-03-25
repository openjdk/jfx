package test.javafx.beans.value;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ObservableValueSubscriptionsTest {
    private final StringProperty value = new SimpleStringProperty("Initial");

    @Test
    void invalidationsShouldCallSubscriberWhenObservableInvalidated() {
        AtomicInteger calls = new AtomicInteger();

        assertEquals(0, calls.get());

        value.invalidations(() -> calls.addAndGet(1));

        assertEquals(0, calls.get());

        value.set("A");

        assertEquals(1, calls.get());

        value.set("B");

        assertEquals(1, calls.get());
    }

    @Test
    void invalidationsShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> value.invalidations(null));
    }

    @Test
    void valuesShouldCallSubscriberImmediatelyAndAfterEachChange() {
        AtomicReference<String> lastCall = new AtomicReference<>();

        assertNull(lastCall.get());

        value.values(lastCall::set);

        assertEquals("Initial", lastCall.get());  // provides initial upon subscribing

        value.set("A");

        assertEquals("A", lastCall.get());

        value.set("B");

        assertEquals("B", lastCall.get());

        lastCall.set(null);

        value.set("B");

        assertNull(lastCall.get());
    }

    @Test
    void valuesShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> value.values(null));
    }

    @Test
    void changesShouldCallSubscriberAfterEachChange() {
        AtomicReference<String> lastCall = new AtomicReference<>();

        assertNull(lastCall.get());

        value.changes((old, current) -> lastCall.set(old + " -> " + current));

        assertNull(lastCall.get());  // Nothing happens upon subscribing

        value.set("A");

        assertEquals("Initial -> A", lastCall.get());

        value.set("B");

        assertEquals("A -> B", lastCall.get());

        lastCall.set(null);

        value.set("B");

        assertNull(lastCall.get());
    }

    @Test
    void changesShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> value.changes(null));
    }
}
