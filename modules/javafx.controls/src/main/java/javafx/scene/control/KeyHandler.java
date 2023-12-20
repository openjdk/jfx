package javafx.scene.control;

public interface KeyHandler {

    /**
     * Triggers an action mapped to the given {@link KeyState} for the given {@link Control}.
     * Returns {@code true} if an action was triggered, otherwise {@code false}.
     *
     * @param keyState a {@code KeyState}, cannot be {@code null}
     * @param controller a controller, cannot be {@code null}
     * @return {@code true} if an action was triggered, otherwise {@code false}
     */
    <C> boolean trigger(KeyState keyState, C controller);
}
