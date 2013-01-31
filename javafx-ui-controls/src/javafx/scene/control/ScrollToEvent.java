package javafx.scene.control;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.*;

/**
 * Event related to {@link ScrollPane} and virtualised controls such as 
 * {@link ListView}, {@link TableView}, {@link TreeView} and {@link TreeTableView}.
 */
public class ScrollToEvent<T> extends Event {
    /**
     * This event occurs if the user requests scrolling a node into view.
     */
    public static final EventType<ScrollToEvent<Node>> SCROLL_TO_NODE = 
            new EventType<ScrollToEvent<Node>>(Event.ANY, "SCROLL_TO_NODE");
    
    /**
     * This event occurs if the user requests scrolling a given index into view.
     */
    public static final EventType<ScrollToEvent<Integer>> SCROLL_TO_TOP_INDEX = 
            new EventType<ScrollToEvent<Integer>>(Event.ANY, "SCROLL_TO_TOP_INDEX");

    private static final long serialVersionUID = -8557345736849482516L;
    
    private final T scrollTarget;

    /**
     * Construct a new {@code Event} with the specified event source, target
     * and type. If the source or target is set to {@code null}, it is replaced
     * by the {@code NULL_SOURCE_TARGET} value.
     * 
     * @param source the event source which sent the event
     * @param target the event source which sent the event
     * @param type the event type
     * @param target the target of the scroll to operation
     */
    public ScrollToEvent(Object source, EventTarget target, EventType<ScrollToEvent<T>> type, T scrollTarget) {
        super(source, target, type);
        assert scrollTarget != null;
        this.scrollTarget = scrollTarget;
        
    }
    
    public T getScrollTarget() {
        return scrollTarget;
    }
}