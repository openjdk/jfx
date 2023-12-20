package javafx.scene.control.behavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.scene.control.KeyHandler;
import javafx.scene.control.KeyState;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

class SimpleKeyBinder implements KeyHandler {
    private final Map<KeyCode, List<Mapping<?>>> mappingsByKeyCode = new HashMap<>();

    // - KeyEvent is not a good parameter to pass here because then the event can be consumed
    // - KeyCodeCombination is not a good parameter to pass here because it has ANY states for modifiers
    // -> new KeyState type created
    @Override
    public <C> boolean trigger(KeyState keyState, C controller) {
        @SuppressWarnings("unchecked")
        List<Mapping<C>> mappings = (List<Mapping<C>>)(List<?>) mappingsByKeyCode.get(keyState.code());

        if (mappings != null) {

            /*
             * There can be multiple mappings for a given KeyCode; a linear search is performed for
             * the final part of the match. Matching modifiers is unsuitable to look up by key (because
             * the ANY modifier state can match UP or DOWN)
             */

            for (Mapping<C> mapping : mappings) {
                boolean keyCodeCombinationMatch = mapping.keyCodeCombination.match(keyState.code(), keyState.shift(), keyState.control(), keyState.alt(), keyState.meta());

                if (keyCodeCombinationMatch && mapping.condition.test(controller)) {
                    mapping.handler.accept(controller);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Registers a {@link Predicate} which is called when the given {@link KeyCodeCombination}
     * is pressed. If the predicate is called, it can indicate whether or not the key press
     * should be consumed. Consumption should be avoided if a key press is not used so events
     * bubble up correctly.
     *
     * @param keyCodeCombination a {@link KeyCodeCombination}, cannot be {@code null}
     * @param consumer a consumer to handle the key press, cannot be {@code null}
     */
    public <C> void addBinding(KeyCodeCombination keyCodeCombination, Predicate<C> consumer) {
        addBinding(keyCodeCombination, consumer, s -> {});
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
    public <C> void addBinding(KeyCodeCombination keyCodeCombination, Predicate<C> condition, Consumer<C> consumer) {
        mappingsByKeyCode.computeIfAbsent(keyCodeCombination.getCode(), k -> new ArrayList<>())
            .add(new Mapping<>(
                Objects.requireNonNull(keyCodeCombination, "keyCodeCombination"),
                Objects.requireNonNull(condition, "condition"),
                Objects.requireNonNull(consumer, "consumer")
            ));
    }

    private record Mapping<C>(KeyCodeCombination keyCodeCombination, Predicate<C> condition, Consumer<C> handler) {}
}
