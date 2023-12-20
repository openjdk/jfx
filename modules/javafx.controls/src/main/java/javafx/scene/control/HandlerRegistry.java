package javafx.scene.control;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;

/**
 * Allows configuring handlers that call back to a controller {@code C} once applied to
 * a control {@code N}.
 *
 * @param <N> the type of the control
 * @param <C> the type of the controller
 */
public interface HandlerRegistry<N extends Control, C> {

    /**
     * Registers an event handler for a given event type. Earlier registrations take precedence over later ones.
     *
     * @param <E> the type of event
     * @param eventType an {@code EventType}, cannot be {@code null}
     * @param eventHandler a consumer taking the configured state and an event as arguments, cannot be {@code null}
     * @return self
     */
    <E extends Event> HandlerRegistry<N, C> registerEventHandler(EventType<E> eventType, BiConsumer<C, ? super E> eventHandler);

    /**
     * Registers a property listener for a given property. Earlier registrations take precedence over later ones.
     *
     * @param <T> the type of property
     * @param supplier a function which supplies a property given a control of type {@code C}, cannot be {@code null}
     * @param listener a listener which is called when the property changes, cannot be {@code null}
     * @return self
     */
     <T> HandlerRegistry<N, C> registerPropertyListener(Function<N, ObservableValue<T>> supplier, BiConsumer<C, T> listener);

    /**
     * Registers a key handler with this installer. Earlier registrations take precedence over later ones.
     *
     * <p>Note: key event handlers always take precedence over key handlers, regardless of registration order.
     *
     * @param keyHandler a {@code KeyHandler}, cannot be {@code null}
     * @return self
     */
    HandlerRegistry<N, C> registerKeyHandler(KeyHandler keyHandler);
}
