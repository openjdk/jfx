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
     * Registers an event handler for a given event type.
     *
     * @param <T> the type of event
     * @param eventType an {@code EventType}, cannot be {@code null}
     * @param eventHandler a consumer taking the event and control as arguments, cannot be {@code null}
     */
    <S, T extends Event> void registerEventHandler(EventType<T> eventType, BiConsumer<S, ? super T> eventHandler);

    /**
     * Registers a property listener for a given property.
     *
     * @param <T> the type of property
     * @param supplier a function which supplies a property given a control of type {@code C}, cannot be {@code null}
     * @param listener a listener which is called when the property changes, cannot be {@code null}
     */
    <S, T> void registerPropertyListener(Function<C, ObservableValue<T>> supplier, BiConsumer<S, T> listener);

    /**
     * Associates a key handler with this installer. Setting this to {@code null} (or not
     * setting it) will result in no key handler being installed.
     *
     * @param keyHandler a {@code KeyHandler}, can be {@code null}
     */
    void setKeyHandler(KeyHandler<C> keyHandler);
}
