package javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.scene.Scene;

class ControlUtils {
    private static final String SCROLL_TO_KEY = "util.scroll.index";

    private static void installScrollToCallback(final Control control, final Observable property, final int index) {
        final ObservableMap<Object, Object> properties = control.getProperties();
            
        if(! properties.containsKey(SCROLL_TO_KEY)) {
            property.addListener(new InvalidationListener() {
                @Override public void invalidated(Observable observable) {
                    Integer idx = (Integer) properties.remove(SCROLL_TO_KEY);
                    if(idx != null) {
                        Event.fireEvent(control, 
                            new ScrollToEvent<Integer>(control, 
                                control, 
                                ScrollToEvent.SCROLL_TO_TOP_INDEX, 
                                idx));    
                    }
                    property.removeListener(this);
                }
            });
        }
        properties.put(SCROLL_TO_KEY, index);
    }
    
    private ControlUtils() { }
    
    public static void scrollToIndex(final Control control, int index) {
        if(control.getScene() == null) {
            installScrollToCallback(control, control.sceneProperty(), index);
        } else if(control.getSkin() == null) {
            installScrollToCallback(control, control.skinProperty(), index);
        } else {
            Event.fireEvent(control, new ScrollToEvent<Integer>(control, control, ScrollToEvent.SCROLL_TO_TOP_INDEX, index));  
        }
    }
}
