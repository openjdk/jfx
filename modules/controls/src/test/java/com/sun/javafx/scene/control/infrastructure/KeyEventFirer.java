/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.scene.control.infrastructure;

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

    public void doKeyTyped(KeyCode keyCode, KeyModifier... modifiers) {
        fireEvents(createEvent(keyCode, KeyEvent.KEY_TYPED, modifiers));
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

        return new KeyEvent(null,
                target,                            // EventTarget
                evtType,                           // eventType
                evtType == KeyEvent.KEY_TYPED ? keyCode.impl_getChar() : null,  // Character (unused unless evtType == KEY_TYPED)
                keyCode.impl_getChar(),            // text
                keyCode,                           // KeyCode
                ml.contains(KeyModifier.SHIFT),    // shiftDown
                ml.contains(KeyModifier.CTRL),     // ctrlDown
                ml.contains(KeyModifier.ALT),      // altDown
                ml.contains(KeyModifier.META)      // metaData
                ); 
    }
}
