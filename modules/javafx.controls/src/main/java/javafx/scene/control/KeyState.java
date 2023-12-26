package javafx.scene.control;

import java.util.Objects;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

// Decision: could turn this into an interface and have KeyEvent implement it
// - Care should be taken not to pass a real KeyEvent though to consumers that are not supposed to be able to consume the event
public record KeyState(KeyCode code, boolean shift, boolean control, boolean alt, boolean meta) {

    /**
     * Creates a {@link KeyState} given a {@link KeyEvent}.
     *
     * @param event a key event, cannot be {@code null}
     * @return a {@code KeyState}, never {@code null}
     * @throws NullPointerException when any argument is {@code null}
     */
    public static KeyState of(KeyEvent event) {
        Objects.requireNonNull(event, "event");

        return new KeyState(event.getCode(), event.isShiftDown(), event.isControlDown(), event.isAltDown(), event.isMetaDown());
    }
}
