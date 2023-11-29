package javafx.scene.control.behavior;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * A control provided facade which limits the available API when installing behaviors. This
 * simplifies the implementation of behaviors, and allows the control to track changes made by
 * an installed behavior.
 *
 * @param <C> the type of the control
 */
public interface BehaviorContext<C extends Control> {

    /**
     * Registers an event handler for a given event type.
     *
     * @param <T> the type of event
     * @param eventType an {@code EventType}, cannot be {@code null}
     * @param eventHandler a consumer taking the event and control as arguments, cannot be {@code null}
     */
    <T extends Event> void registerEventHandler(EventType<T> eventType, BiConsumer<? super T, C> eventHandler);

    /**
     * Registers a property listener for a given property.
     *
     * @param <T> the type of property
     * @param supplier a function which supplies a property given a control of type {@code C}, cannot be {@code null}
     * @param listener a listener which is called when the property changes, cannot be {@code null}
     */
    <T> void registerPropertyListener(Function<C, ObservableValue<T>> supplier, BiConsumer<T, C> listener);

    /**
     * Registers a handler which is called when the given {@link KeyCodeCombination} is pressed. The event
     * associated with the key press is consumed.
     *
     * @param keyCodeCombination a {@link KeyCodeCombination}, cannot be {@code null}
     * @param eventHandler a consumer to handle the event, cannot be {@code null}
     */
    default void registerKeyPressedHandler(KeyCodeCombination keyCodeCombination, Consumer<C> eventHandler) {
        registerKeyPressedHandler(keyCodeCombination, c -> true, eventHandler);
    }

    /**
     * Registers a handler which, when the given condition holds, is called when the given {@link KeyCodeCombination}
     * is pressed. If the handler is called, the event associated with the key press is consumed. Consumption
     * should be avoided by using an appropriate condition so events that are unused bubble up correctly.
     *
     * @param keyCodeCombination a {@link KeyCodeCombination}, cannot be {@code null}
     * @param condition a condition which must hold before the handler is called (and the event is consumed), cannot be {@code null}
     * @param eventHandler a consumer to handle the event, cannot be {@code null}
     */
    void registerKeyPressedHandler(KeyCodeCombination keyCodeCombination, Predicate<C> condition, Consumer<C> eventHandler);
}
