package javafx.scene.control.behavior;

import javafx.scene.control.Control;

public interface KeyHandler<C extends Control> {

    /**
     * Triggers an action mapped to the given {@link KeyState} for the given {@link Control}.
     * Returns {@code true} if an action was triggered, otherwise {@code false}.
     *
     * @param keyState a {@code KeyState}, cannot be {@code null}
     * @param control a {@code Control}, cannot be {@code null}
     * @return {@code true} if an action was triggered, otherwise {@code false}
     */
    boolean trigger(KeyState keyState, C control);
}
