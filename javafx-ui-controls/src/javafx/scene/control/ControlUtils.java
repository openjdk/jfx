package javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.Event;
import javafx.scene.Node;

class ControlUtils {
    private static final String CACHE_KEY = "util.scroll.index";
    public static void scrollToIndex(final Node node, int index) {
        if( node.getScene() == null ) {
            if( ! node.getProperties().containsKey(CACHE_KEY) ) {
                node.sceneProperty().addListener(new InvalidationListener() {
                    
                    @Override
                    public void invalidated(Observable observable) {
                        Integer idx = (Integer) node.getProperties().remove(CACHE_KEY);
                        if( idx != null ) {
                            Event.fireEvent(node, new ScrollToEvent<Integer>(node, node, ScrollToEvent.SCROLL_TO_TOP_INDEX, idx));    
                        }
                        node.sceneProperty().removeListener(this);
                    }
                });
            }
            node.getProperties().put(CACHE_KEY, index);
        } else {
            Event.fireEvent(node, new ScrollToEvent<Integer>(node, node, ScrollToEvent.SCROLL_TO_TOP_INDEX, index));  
        }
    }
}
