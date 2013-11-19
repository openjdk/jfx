package javafx.scene.control;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Event related to {@link TableView} and {@link TreeTableView} sorting.
 * @since JavaFX 8.0
 */
public class SortEvent<C> extends Event {

    /**
     * Common supertype for all sort event types.
     */
    public static final EventType<SortEvent> ANY =
            new EventType<SortEvent> (Event.ANY, "SORT");

    @SuppressWarnings("unchecked")
    public static <C> EventType<SortEvent<C>> sortEvent() {
        return (EventType<SortEvent<C>>) SORT_EVENT;
    }
    
    @SuppressWarnings("unchecked")
    private static final EventType<?> SORT_EVENT = new EventType(SortEvent.ANY, "SORT_EVENT");
    
//    /**
//     * Construct a new {@code Event} with the specified event source, target
//     * and type. If the source or target is set to {@code null}, it is replaced
//     * by the {@code NULL_SOURCE_TARGET} value.
//     * 
//     * @param source the event source which sent the event
//     * @param target the event source which sent the event
//     * @param type the event type
//     * @param target the target of the scroll to operation
//     */
    public SortEvent(@NamedArg("source") C source, @NamedArg("target") EventTarget target) {
        super(source, target, sortEvent());
        
    }

    @SuppressWarnings("unchecked")
    @Override public C getSource() {
        return (C) super.getSource();
    }
}
