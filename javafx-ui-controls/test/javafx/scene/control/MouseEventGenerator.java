package javafx.scene.control;

import javafx.event.EventType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public final class MouseEventGenerator {
    private static boolean primaryButtonDown = false;

    public static MouseEvent generateMouseEvent(EventType<MouseEvent> type,
            double x, double y) {

        MouseButton button = MouseButton.NONE;
        if (type == MouseEvent.MOUSE_PRESSED ||
                type == MouseEvent.MOUSE_RELEASED ||
                type == MouseEvent.MOUSE_DRAGGED) {
            button = MouseButton.PRIMARY;
        }

        if (type == MouseEvent.MOUSE_PRESSED ||
                type == MouseEvent.MOUSE_DRAGGED) {
            primaryButtonDown = true;
        }

        if (type == MouseEvent.MOUSE_RELEASED) {
            primaryButtonDown = false;
        }

        MouseEvent event = MouseEvent.impl_mouseEvent(x, y, x, y, button,
                1, false, false, false, false, false, primaryButtonDown,
                false, false, false, type);

        return event;
    }    
}    
