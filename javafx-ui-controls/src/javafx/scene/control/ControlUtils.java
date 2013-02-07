package javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableMap;
import javafx.event.Event;

class ControlUtils {
    private static final String SCROLL_TO_INDEX_KEY = "util.scroll.index";
    private static final String SCROLL_TO_COLUMN_KEY = "util.scroll.column";

    private ControlUtils() { }
    
    public static void scrollToIndex(final Control control, int index) {
        if(control.getSkin() == null) {
            installScrollToIndexCallback(control, control.skinProperty(), index);
        } else {
            fireScrollToIndexEvent(control, index);  
        }
    }
    
    private static void installScrollToIndexCallback(final Control control, final Observable property, final int index) {
        final ObservableMap<Object, Object> properties = control.getProperties();
            
        if(! properties.containsKey(SCROLL_TO_INDEX_KEY)) {
            property.addListener(new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    Integer idx = (Integer) properties.remove(SCROLL_TO_INDEX_KEY);
                    if(idx != null) {
                        fireScrollToIndexEvent(control, idx);  
                    }
                    property.removeListener(this);
                }
            });
        }
        properties.put(SCROLL_TO_INDEX_KEY, index);
    }
    
    private static void fireScrollToIndexEvent(final Control control, final int index) {
        Event.fireEvent(control, new ScrollToEvent<Integer>(control, control, ScrollToEvent.scrollToTopIndex(), index));
    }
    
    
    
    public static void scrollToColumn(final Control control, final TableColumnBase<?, ?> column) {
        if(control.getSkin() == null) {
            installScrollToColumnCallback(control, control.skinProperty(), column);
        } else {
            fireScrollToColumnEvent(control, column);
        }
    }
    
    private static void installScrollToColumnCallback(final Control control, final Observable property, final TableColumnBase<?, ?> column) {
        final ObservableMap<Object, Object> properties = control.getProperties();
            
        if(! properties.containsKey(SCROLL_TO_COLUMN_KEY)) {
            property.addListener(new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    TableColumnBase<?, ?> col = (TableColumnBase<?, ?>) control.getProperties().remove(SCROLL_TO_COLUMN_KEY);
                    if( col != null ) {
                        fireScrollToColumnEvent(control, col);
                    }
                    property.removeListener(this);
                }
            });
        }
        properties.put(SCROLL_TO_COLUMN_KEY, column);
    }
    
    private static void fireScrollToColumnEvent(final Control control, final TableColumnBase<?, ?> column) {
        control.fireEvent(new ScrollToEvent<TableColumnBase<?, ?>>(control, control, ScrollToEvent.scrollToColumn(), column));
    }
}
