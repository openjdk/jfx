/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.tk.Toolkit;

/**
 * Keyboard simulator for use with tests.
 */
public class KeyEventFirer {
    private final EventTarget target;
    private final Scene scene;
    private static HashMap<Character,KeyCode> keyCodes;

    /**
     * Instantiates a KeyEventFirer on the given event target. KeyEvents are
     * fired directly onto the target.
     *
     * <p>
     * Beware: using this constructor on an <code>EventTarget</code> of type <code>Node</code>
     * which is not focusOwner may lead
     * to false greens (see https://bugs.openjdk.org/browse/JDK-8231692).
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

    /**
     * Sends a KEY_PRESS event, followed by a KEY_RELEASE event.
     * This might represent an incomplete sequence for keys which also
     * supposed to generate a KEY_TYPED event.
     * @param keyCode the key code
     * @param modifiers the key modifiers
     */
    public void doKeyPress(KeyCode keyCode, KeyModifier... modifiers) {
        fireEvents(createMirroredEvents(keyCode, modifiers));
    }

    public void doKeyTyped(KeyCode keyCode, KeyModifier... modifiers) {
        fireEvents(createEvent(keyCode, KeyEvent.KEY_TYPED, modifiers));
    }

    /**
     * Generates a single KEY_PRESSED event.
     * @param code the key code
     * @param modifiers the key modifiers
     * @return true if the event has been consumed
     */
    public boolean keyPressed(KeyCode code, KeyModifier... modifiers) {
        KeyEvent ev = createEvent(code, KeyEvent.KEY_PRESSED, modifiers);
        return fireAndCheckConsumed(ev);
    }

    /**
     * Generates a single KEY_TYPED event.
     * @param code the key code
     * @param typedChar the character typed
     * @param modifiers the key modifiers
     * @return true if the event has been consumed
     */
    public boolean keyTyped(KeyCode code, String typedChar, KeyModifier... modifiers) {
        KeyEvent ev = new KeyEvent(
            null,
            target, // EventTarget
            KeyEvent.KEY_TYPED,
            typedChar,
            code.getChar(), // text
            code,
            false, // shiftDown
            false, // ctrlDown
            false, // altDown
            false // metaData
        );
        return fireAndCheckConsumed(ev);
    }

    /**
     * Generates a single KEY_RELEASED event.
     * @param code the key code
     * @param modifiers the key modifiers
     * @return true if the event has been consumed
     */
    public boolean keyReleased(KeyCode code, KeyModifier... modifiers) {
        KeyEvent ev = createEvent(code, KeyEvent.KEY_RELEASED, modifiers);
        return fireAndCheckConsumed(ev);
    }

    private boolean fireAndCheckConsumed(KeyEvent event) {
        // dispatcher creates an event copy, so we need to jump over some hoops to get the actual event
        AtomicReference<KeyEvent> ref = new AtomicReference<>();
        EventHandler<KeyEvent> h = new EventHandler<>() {
            @Override
            public void handle(KeyEvent ev) {
                if (equals(event, ev)) {
                    ref.set(ev);
                }
            }

            private boolean equals(KeyEvent a, KeyEvent b) {
                return
                    eq(a.getEventType(), b.getEventType()) &&
                    eq(a.getCode(), b.getCode()) &&
                    eq(a.getCharacter(), b.getCharacter()) &&
                    (a.isAltDown() == b.isAltDown()) &&
                    (a.isControlDown() == b.isControlDown()) &&
                    (a.isMetaDown() == b.isMetaDown()) &&
                    (a.isShiftDown() == b.isShiftDown()) &&
                    (a.isShortcutDown() == b.isShortcutDown());
            }

            private boolean eq(Object a, Object b) {
                if (a == null) {
                    return (b == null);
                }
                return a.equals(b);
            }
        };

        target.addEventFilter(event.getEventType(), h);
        try {
            fireEvents(event);
            Toolkit.getToolkit().firePulse();
            KeyEvent ev = ref.get();
            return ev.isConsumed();
        } finally {
            target.removeEventFilter(event.getEventType(), h);
        }
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
        // WARNING: tests may pass null modifiers!
        List<KeyModifier> ml = Arrays.asList(modifiers);

        return new KeyEvent(null,
            target, // EventTarget
            evtType, // eventType
            evtType == KeyEvent.KEY_TYPED ? keyCode.getChar() : null, // Character (unused unless evtType == KEY_TYPED)
            keyCode.getChar(), // text
            keyCode, // KeyCode
            ml.contains(KeyModifier.SHIFT), // shiftDown
            ml.contains(KeyModifier.CTRL), // ctrlDown
            ml.contains(KeyModifier.ALT), // altDown
            ml.contains(KeyModifier.META) // metaData
        );
    }

    /**
     * Simulates typing of the specified text by issuing a sequence of
     * KEY_PRESSED, KEY_TYPED, and KEY_RELEASED events.
     * Supports ASCII only keys.
     *
     * @param items a sequence of Strings and/or {@link KeyCode}s
     */
    public void type(Object ... items) {
        for(Object x: items) {
            if(x instanceof String text) {
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    typeChar(c);
                }
            } else if(x instanceof KeyCode c) {
                typeCode(c);
            }
        }
    }

    private void typeChar(char c) {
        KeyCode code = getKeyCodeFor(c);
        KeyEvent ev;
        //    eventType = KEY_PRESSED, consumed = false, character =  , text = a, code = A]
        //    eventType = KEY_TYPED, consumed = false, character = a, text = , code = UNDEFINED]
        //    eventType = KEY_RELEASED, consumed = false, character =  , text = a, code = A]

        ev = createEvent(code, KeyEvent.KEY_PRESSED);
        fireEvents(ev);
        Toolkit.getToolkit().firePulse();

        ev = new KeyEvent(
            null,
            target, // EventTarget
            KeyEvent.KEY_TYPED,
            String.valueOf(c),
            code.getChar(), // text
            code,
            false, // shiftDown
            false, // ctrlDown
            false, // altDown
            false // metaData
        );
        fireEvents(ev);
        Toolkit.getToolkit().firePulse();

        ev = createEvent(code, KeyEvent.KEY_RELEASED);
        fireEvents(ev);
        Toolkit.getToolkit().firePulse();
    }

    private void typeCode(KeyCode code) {
        KeyEvent ev;
        // eventType = KEY_PRESSED, consumed = false, character =  , text = , code = RIGHT]
        // eventType = KEY_RELEASED, consumed = false, character =  , text = , code = RIGHT]

        ev = createEvent(code, KeyEvent.KEY_PRESSED);
        fireEvents(ev);
        Toolkit.getToolkit().firePulse();

        ev = createEvent(code, KeyEvent.KEY_RELEASED);
        fireEvents(ev);
        Toolkit.getToolkit().firePulse();
    }

    /**
     * Looks up a KeyCode for the given character.
     * @param c character
     * @return KeyCode
     */
    public static KeyCode getKeyCodeFor(char c) {
        if (keyCodes == null) {
            keyCodes = createKeyCodes(
                " ", KeyCode.SPACE,
                "a", KeyCode.A,
                "b", KeyCode.B,
                "c", KeyCode.C,
                "d", KeyCode.D,
                "e", KeyCode.E,
                "f", KeyCode.F,
                "g", KeyCode.G,
                "h", KeyCode.H,
                "i", KeyCode.I,
                "j", KeyCode.J,
                "k", KeyCode.K,
                "l", KeyCode.L,
                "m", KeyCode.M,
                "n", KeyCode.N,
                "o", KeyCode.O,
                "p", KeyCode.P,
                "q", KeyCode.Q,
                "r", KeyCode.R,
                "s", KeyCode.S,
                "t", KeyCode.T,
                "u", KeyCode.U,
                "v", KeyCode.V,
                "w", KeyCode.W,
                "x", KeyCode.X,
                "y", KeyCode.Y,
                "z", KeyCode.Z,
                "0", KeyCode.DIGIT0,
                "1", KeyCode.DIGIT1,
                "2", KeyCode.DIGIT2,
                "3", KeyCode.DIGIT3,
                "4", KeyCode.DIGIT4,
                "5", KeyCode.DIGIT5,
                "6", KeyCode.DIGIT6,
                "7", KeyCode.DIGIT7,
                "8", KeyCode.DIGIT8,
                "9", KeyCode.DIGIT9,
                ".", KeyCode.PERIOD,
                ",", KeyCode.COMMA,
                "\n", KeyCode.ENTER
            );
        }

        KeyCode code = keyCodes.get(c);
        if (code == null) {
            throw new RuntimeException(String.format("character 0x%04x has no corresponding KeyCode", (int)c));
        }
        return code;
    }

    private static HashMap<Character, KeyCode> createKeyCodes(Object ... pairs) {
        HashMap<Character, KeyCode> m = new HashMap<>();
        for(int i=0; i<pairs.length; ) {
            char c = ((String)pairs[i++]).charAt(0);
            KeyCode code = (KeyCode)pairs[i++];
            m.put(c, code);
        }
        return m;
    }
}
