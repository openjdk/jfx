package javafx.scene.control;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.behavior.BehaviorInstaller;
import javafx.scene.control.behavior.KeyHandler;
import javafx.scene.control.behavior.KeyState;
import javafx.scene.input.KeyEvent;
import javafx.util.Subscription;

class StandardBehaviorInstaller<C extends Control> implements BehaviorInstaller<C> {
    private final C control;
    private final List<EventHandlerDefinition<?, ?>> eventHandlerDefinitions = new ArrayList<>();
    private final List<PropertyListenerDefinition<?, ?, ?>> propertyListenerDefinitions = new ArrayList<>();

    private KeyHandler<C> keyHandler;

    public StandardBehaviorInstaller(C control) {
        this.control = control;
    }

    public <S, T extends Event> Subscription install(S state) {
        Subscription subscription = Subscription.EMPTY;

        /*
         * Install event handlers on the control:
         */

        for (EventHandlerDefinition<?, ?> rawDefinition : eventHandlerDefinitions) {
            @SuppressWarnings("unchecked")
            EventHandlerDefinition<S, T> mapping = (EventHandlerDefinition<S, T>) rawDefinition;
            EventHandler<T> handler = e -> mapping.eventHandler.accept(state, e);
            EventType<T> eventType = mapping.eventType;

            control.addEventHandler(eventType, handler);

            subscription = subscription.and(() -> control.removeEventHandler(eventType, handler));
        }

        /*
         * Install property listeners on the control:
         */

        for (PropertyListenerDefinition<?, ?, ?> rawDefinition : propertyListenerDefinitions) {
            @SuppressWarnings("unchecked")
            PropertyListenerDefinition<S, C, Object> definition = (PropertyListenerDefinition<S, C, Object>) rawDefinition;

            subscription = subscription.and(definition.propertySupplier.apply(control).subscribe(value -> definition.listener.accept(state, value)));
        }

        /*
         * Install key handler on the control:
         */

        if (keyHandler != null) {
            EventHandler<? super KeyEvent> eventHandler = e -> {
                KeyState keyState = new KeyState(e.getCode(), e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown());

                if (keyHandler.trigger(keyState, control)) {
                    e.consume();
                }
            };

            control.addEventHandler(KeyEvent.KEY_PRESSED, eventHandler);

            subscription = subscription.and(() -> control.removeEventHandler(KeyEvent.KEY_PRESSED, eventHandler));
        }

        return subscription;
    }

    @Override
    public <S, T extends Event> void registerEventHandler(EventType<T> eventType, BiConsumer<S, ? super T> eventHandler) {
        eventHandlerDefinitions.add(new EventHandlerDefinition<>(eventType, eventHandler));
    }

    @Override
    public <S, T> void registerPropertyListener(Function<C, ObservableValue<T>> supplier, BiConsumer<S, T> listener) {
        propertyListenerDefinitions.add(new PropertyListenerDefinition<>(supplier, listener));
    }

    @Override
    public void setKeyHandler(KeyHandler<C> binder) {
        this.keyHandler = binder;
    }

    record EventHandlerDefinition<S, T extends Event>(EventType<T> eventType, BiConsumer<S, ? super T> eventHandler) {}
    record PropertyListenerDefinition<S, C, V>(Function<C, ObservableValue<V>> propertySupplier, BiConsumer<S, V> listener) {}
}