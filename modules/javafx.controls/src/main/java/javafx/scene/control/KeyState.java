package javafx.scene.control;

import javafx.scene.input.KeyCode;

// Decision: could turn this into an interface and have KeyEvent implement it
public record KeyState(KeyCode code, boolean shift, boolean control, boolean alt, boolean meta) {}
