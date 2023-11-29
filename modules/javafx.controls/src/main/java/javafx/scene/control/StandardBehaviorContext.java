package javafx.scene.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.behavior.BehaviorContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.util.Subscription;

class StandardBehaviorContext<C extends Control> implements BehaviorContext<C> {
    private final C control;

    private KeyEventHandler keyEventHandler;
    private Subscription subscription = Subscription.EMPTY;

    public StandardBehaviorContext(C control) {
        this.control = control;
    }

    @Override
    public <T extends Event> void registerEventHandler(EventType<T> eventType, BiConsumer<? super T, C> eventHandler) {
        EventHandler<T> handler = e -> eventHandler.accept(e, control);

        control.addEventHandler(eventType, handler);

        subscription = subscription.and(() -> control.removeEventHandler(eventType, handler));
    }

    @Override
    public <T> void registerPropertyListener(Function<C, ObservableValue<T>> supplier, BiConsumer<T, C> listener) {
        subscription = subscription.and(supplier.apply(control).subscribe(value -> listener.accept(value, control)));
    }

    @Override
    public void registerKeyPressedHandler(KeyCodeCombination keyCodeCombination, Predicate<C> condition, Consumer<C> eventHandler) {
        Objects.requireNonNull(keyCodeCombination, "keyCodeCombination");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(eventHandler, "eventHandler");

        if (keyEventHandler == null) {
            keyEventHandler = new KeyEventHandler();

            registerEventHandler(KeyEvent.KEY_PRESSED, keyEventHandler);
        }

        keyEventHandler.addBinding(keyCodeCombination, condition, eventHandler);
    }

    @Override
    public void registerKeyPressedHandler(KeyCodeCombination keyCodeCombination, Consumer<C> eventHandler) {
        registerKeyPressedHandler(keyCodeCombination, c -> true, eventHandler);
    }

    public Subscription getSubscription() {
        return subscription;
    }

    private class KeyEventHandler implements BiConsumer<KeyEvent, C> {
        private final Map<KeyCode, List<Mapping<C>>> mappingsByKeyCode = new HashMap<>();

        @Override
        public void accept(KeyEvent event, C control) {
            List<Mapping<C>> mappings = mappingsByKeyCode.get(event.getCode());

            for (Mapping<C> mapping : mappings) {
                if (mapping.keyCodeCombination.match(event) && mapping.condition.test(control)) {
                    mapping.handler.accept(control);
                    event.consume();
                    break;
                }
            }
        }

        void addBinding(KeyCodeCombination keyCodeCombination, Predicate<C> condition, Consumer<C> eventHandler) {
            mappingsByKeyCode.computeIfAbsent(keyCodeCombination.getCode(), k -> new ArrayList<>())
                .add(new Mapping<>(keyCodeCombination, condition, eventHandler));
        }
    }

    private record Mapping<C>(KeyCodeCombination keyCodeCombination, Predicate<C> condition, Consumer<C> handler) {}
}