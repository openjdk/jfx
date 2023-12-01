package javafx.scene.control.behavior;

import javafx.scene.control.Control;

public interface StateFactory<C extends Control> {
    Object createState(C control);
}
