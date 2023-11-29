package javafx.scene.control.skin;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.AccessibleAction;

public class AccessibleActionEvent extends Event {
    private static final long serialVersionUID = 1L;

    public static final EventType<AccessibleActionEvent> ANY = new EventType<>(Event.ANY, "ACCESSIBLE_ACTION");
    public static final EventType<AccessibleActionEvent> TRIGGERED = new EventType<>(AccessibleActionEvent.ANY, "ACCESSIBLE_ACTION_TRIGGERED");

    private final AccessibleAction action;

    public static AccessibleActionEvent triggered(AccessibleAction action) {
        return new AccessibleActionEvent(TRIGGERED, action);
    }

    private AccessibleActionEvent(EventType<? extends AccessibleActionEvent> eventType, AccessibleAction action) {
        super(eventType);

        this.action = action;
    }

    public AccessibleAction getAction() {
        return action;
    }
}
