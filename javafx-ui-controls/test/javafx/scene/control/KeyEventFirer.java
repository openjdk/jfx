/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import java.util.Arrays;
import java.util.List;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.event.Event;
import javafx.event.EventTarget;


public class KeyEventFirer {
    
    private final EventTarget target;
    
    public KeyEventFirer(EventTarget target) {
        this.target = target;
    }
    
    public void doUpArrowPress(KeyModifier... modifiers) {
        doKeyPress(KeyCode.UP, modifiers);
    }
    
    public void doDownArrowPress(KeyModifier... modifiers) {
        doKeyPress(KeyCode.DOWN, modifiers);
    }
    
    public void doLeftArrowPress(KeyModifier... modifiers) {
        doKeyPress(KeyCode.LEFT, modifiers);
    }
    
    public void doRightArrowPress(KeyModifier... modifiers) {
        doKeyPress(KeyCode.RIGHT, modifiers);
    }
    
    public void doKeyPress(KeyCode keyCode, KeyModifier... modifiers) {
        fireEvents(createMirroredEvents(keyCode, modifiers));
    }
    
    private void fireEvents(KeyEvent... events) {
        for (KeyEvent evt : events) {
            Event.fireEvent(target, evt);
        }
    }
    
    private KeyEvent[] createMirroredEvents(KeyCode keyCode, KeyModifier... modifiers) {
        KeyEvent[] events = new KeyEvent[2];
        events[0] = createEvent(keyCode, KeyEvent.KEY_PRESSED, modifiers);
        events[1] = createEvent(keyCode, KeyEvent.KEY_RELEASED, modifiers);
        return events;
    }
    
    private KeyEvent createEvent(KeyCode keyCode, EventType<KeyEvent> evtType, KeyModifier... modifiers) {
        List<KeyModifier> ml = Arrays.asList(modifiers);

        return new KeyEvent( null,
                target, // EventTarget
                evtType,  // eventType
                null,     // Character (unused unless KeyCode == KEY_TYPED
                null,     // text
                keyCode, // KeyCode
                ml.contains(KeyModifier.SHIFT),    // shiftDown
                ml.contains(KeyModifier.CTRL),     // ctrlDown
                ml.contains(KeyModifier.ALT),      // altDown
                ml.contains(KeyModifier.META)      // metaData
                ); 
    }
}
