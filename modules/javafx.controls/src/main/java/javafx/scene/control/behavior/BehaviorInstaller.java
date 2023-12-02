package javafx.scene.control.behavior;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.Control;

/**
 * Allows behaviors to configure how they should be installed on a {@link Control}. This facade
 * is provided by the involved control, and guides the installation process. This allows the
 * control to track any changes made by the behavior, and helps to limit the available API to
 * keep behaviors focused on their role.
 *
 * @param <C> the type of the control
 */
public interface BehaviorInstaller<C extends Control> {

    /**
     * Registers an event handler for a given event type. Earlier registrations take precedence over later ones.
     *
     * @param <E> the type of event
     * @param <S> the type of the configured state
     * @param eventType an {@code EventType}, cannot be {@code null}
     * @param eventHandler a consumer taking the configured state and an event as arguments, cannot be {@code null}
     */
    <S, E extends Event> void registerEventHandler(EventType<E> eventType, BiConsumer<S, ? super E> eventHandler);

    /**
     * Registers a property listener for a given property. Earlier registrations take precedence over later ones.
     *
     * @param <T> the type of property
     * @param <S> the type of the configured state
     * @param supplier a function which supplies a property given a control of type {@code C}, cannot be {@code null}
     * @param listener a listener which is called when the property changes, cannot be {@code null}
     */
    <S, T> void registerPropertyListener(Function<C, ObservableValue<T>> supplier, BiConsumer<S, T> listener);

    /**
     * Registers a key handler with this installer. Earlier registrations take precedence over later ones.
     *
     * <p>Note: event handlers always take precedence over key handlers, regardless of registration order
     *
     * @param keyHandler a {@code KeyHandler}, cannot be {@code null}
     */
    void registerKeyHandler(KeyHandler<? super C> keyHandler);
}
