package javafx.scene.control;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;

/**
 * Event related to {@link ScrollPane} and virtualised controls such as 
 * {@link ListView}, {@link TableView}, {@link TreeView} and {@link TreeTableView}.
 */
public class ScrollToEvent<T> extends Event {
    /**
     * This event occurs if the user requests scrolling a node into view.
     */
    @SuppressWarnings("unchecked")
    public static EventType<ScrollToEvent<Node>> scrollToNode() {
        return SCROLL_TO_NODE;
    }
    private static final EventType<ScrollToEvent<Node>> SCROLL_TO_NODE = 
            new EventType<ScrollToEvent<Node>>(Event.ANY, "SCROLL_TO_NODE");
    
    /**
     * This event occurs if the user requests scrolling a given index into view.
     */
    @SuppressWarnings("unchecked")
    public static EventType<ScrollToEvent<Integer>> scrollToTopIndex() {
        return SCROLL_TO_TOP_INDEX;
    }
    private static final EventType<ScrollToEvent<Integer>> SCROLL_TO_TOP_INDEX = 
            new EventType<ScrollToEvent<Integer>>(Event.ANY, "SCROLL_TO_TOP_INDEX");
    

    /**
     * This event occurs if the user requests scrolling a {@link TableColumnBase}
     * (i.e. {@link TableColumn} or {@link TreeTableColumn}) into view.
     */
    @SuppressWarnings("unchecked")
    public static <T extends TableColumnBase<?, ?>> EventType<ScrollToEvent<T>> scrollToColumn() {
        return (EventType<ScrollToEvent<T>>) SCROLL_TO_COLUMN;
    }
    private static final EventType<?> SCROLL_TO_COLUMN = 
            new EventType(Event.ANY, "SCROLL_TO_COLUMN");
    
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