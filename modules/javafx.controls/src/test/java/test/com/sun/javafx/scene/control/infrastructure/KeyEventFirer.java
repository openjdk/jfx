/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.infrastructure;

import com.sun.javafx.scene.SceneHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;


public class KeyEventFirer {

    private final EventTarget target;
    private final Scene scene;

    /**
     * Instantiates a KeyEventFirer on the given event target. KeyEvents are
     * fired directly onto the target.
     *
     * <p>
     * Beware: using this constructor on an <code>EventTarget</code> of type <code>Node</code>
     * which is not focusOwner may lead
     * to false greens (see https://bugs.openjdk.java.net/browse/JDK-8231692).
     *
     * @param target the target to fire keyEvents onto, must not be null.
     * @throws NullPointerException if target is null.
     */
    public KeyEventFirer(EventTarget target) {
        this(Objects.requireNonNull(target), null);
    }

    /**
     * Instantiates a KeyEventFirer for the given target and scene.
     * Any one of those can be null, but not both. A null/not null scene decides
     * about the delivering path of events. If null, events are delivered
     * via <code>EventUtils.fire(target, keyEvent)</code>, otherwise via
     * <code>SceneHelper.processKeyEvent(scene, keyEvent)</code>.
     * <p>
     * Note that in the latter case, the target doesn't matter - the scene
     * delivers keyEvents to its focusOwner. Calling code is responsible to
     * setup focus state as required.
     *
     * @param target eventTarget used to create the event for and fire events onto
     *    if there's no scene
     * @param scene to use for delivering events to its focusOwner if not null
     *
     * @throws NullPointerException if both target and scene are null
     */
    public KeyEventFirer(EventTarget target, Scene scene) {
        this.target = target;
        this.scene = scene;
        if (target == null && scene == null) {
            throw new NullPointerException("both target and scene are null");
        }
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

    /**
     * Dispatches the given events. The process depends on the state of
     * this firer. If the scene is null, the events are delivered via
     * Event.fireEvent(target,..), otherwise they are delivered via
     * SceneHelper.processKeyEvent.
     *
     * @param events the events to dispatch.
     */
    private void fireEvents(KeyEvent... events) {
        for (KeyEvent evt : events) {
            if (scene != null) {
                SceneHelper.processKeyEvent(scene, evt);
            } else {
                Event.fireEvent(target, evt);
            }
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
                evtType == KeyEvent.KEY_TYPED ? keyCode.getChar() : null,  // Character (unused unless evtType == KEY_TYPED)
                keyCode.getChar(),            // text
                keyCode,                           // KeyCode
                ml.contains(KeyModifier.SHIFT),    // shiftDown
                ml.contains(KeyModifier.CTRL),     // ctrlDown
                ml.contains(KeyModifier.ALT),      // altDown
                ml.contains(KeyModifier.META)      // metaData
                );
    }
}
