package javafx.scene.control.behavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

class SimpleKeyBinder<C extends Control> implements KeyHandler<C> {
    private final Map<KeyCode, List<Mapping<C>>> mappingsByKeyCode = new HashMap<>();

    // - KeyEvent is not a good parameter to pass here because then the event can be consumed
    // - KeyCodeCombination is not a good parameter to pass here because it has ANY states for modifiers
    // -> new KeyState type created
    @Override
    public boolean trigger(KeyState keyState, C control) {
        List<Mapping<C>> mappings = mappingsByKeyCode.get(keyState.code());

        for (Mapping<C> mapping : mappings) {
            if (mapping.keyCodeCombination.match(keyState.code(), keyState.shift(), keyState.control(), keyState.alt(), keyState.meta()) && mapping.condition.test(control)) {
                mapping.handler.accept(control);

                return true;
            }
        }

        return false;
    }

    /**
     * Registers a {@link Consumer} which is called when the given {@link KeyCodeCombination}
     * is pressed. If the consumer is called, the event associated with the key press is consumed. Consumption
     * should be avoided by using an appropriate condition so events that are unused bubble up correctly.
     *
     * @param keyCodeCombination a {@link KeyCodeCombination}, cannot be {@code null}
     * @param consumer a consumer to handle the key press, cannot be {@code null}
     */
    public void addBinding(KeyCodeCombination keyCodeCombination, Consumer<C> consumer) {
        addBinding(keyCodeCombination, c -> true, consumer);
    }

    /**
     * Registers a {@link Consumer} which, when the given condition holds, is called when the given {@link KeyCodeCombination}
     * is pressed. If the consumer is called, the event associated with the key press is consumed. Consumption
     * should be avoided by using an appropriate condition so events that are unused bubble up correctly.
     *
     * @param keyCodeCombination a {@link KeyCodeCombination}, cannot be {@code null}
     * @param condition a condition which must hold before the consumer is called (and the event is consumed), cannot be {@code null}
     * @param consumer a consumer to handle the key press, cannot be {@code null}
     */
    public void addBinding(KeyCodeCombination keyCodeCombination, Predicate<C> condition, Consumer<C> consumer) {
        mappingsByKeyCode.computeIfAbsent(keyCodeCombination.getCode(), k -> new ArrayList<>())
            .add(new Mapping<>(keyCodeCombination, condition, consumer));
    }

    private record Mapping<C>(KeyCodeCombination keyCodeCombination, Predicate<C> condition, Consumer<C> handler) {}
}
