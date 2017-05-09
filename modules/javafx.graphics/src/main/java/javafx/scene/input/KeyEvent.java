/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import com.sun.javafx.tk.Toolkit;
import javafx.beans.NamedArg;
import javafx.event.EventTarget;
import javafx.event.EventType;

import com.sun.javafx.scene.input.KeyCodeMap;
import javafx.event.Event;
import javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits;
import javafx.scene.input.ScrollEvent.VerticalTextScrollUnits;

/**
 * An event which indicates that a keystroke occurred in a {@link javafx.scene.Node}.
 * <p>
 * This event is generated when a key is pressed, released, or typed.
 * Depending on the type of the event it is passed
 * to {@link javafx.scene.Node#onKeyPressedProperty onKeyPressed}, {@link javafx.scene.Node#onKeyTypedProperty onKeyTyped}
 * or {@link javafx.scene.Node#onKeyReleasedProperty onKeyReleased} function.
 *
 * <p>
 * <em>"Key typed" events</em> are higher-level and generally do not depend on
 * the platform or keyboard layout.  They are generated when a Unicode character
 * is entered, and are the preferred way to find out about character input.
 * In the simplest case, a key typed event is produced by a single key press
 * (e.g., 'a').  Often, however, characters are produced by series of key
 * presses (e.g., SHIFT + 'a'), and the mapping from key pressed events to
 * key typed events may be many-to-one or many-to-many.  Key releases are not
 * usually necessary to generate a key typed event, but there are some cases
 * where the key typed event is not generated until a key is released (e.g.,
 * entering ASCII sequences via the Alt-Numpad method in Windows).
 * No key typed events are generated for keys that don't generate Unicode
 * characters (e.g., action keys, modifier keys, etc.).
 *
 * <p>
 * The {@code character} variable always contains a valid Unicode character(s)
 * or CHAR_UNDEFINED. Character input is reported by key typed events;
 * key pressed and key released events are not necessarily associated
 * with character input. Therefore, the {@code character} variable
 * is guaranteed to be meaningful only for key typed events.
 *
 * <p>
 * For key pressed and key released events, the {@code code} variable contains
 * the event's key code.  For key typed events, the {@code code} variable
 * always contains {@code KeyCode.UNDEFINED}.
 *
 * <p>
 * <em>"Key pressed" and "key released" events</em> are lower-level and depend
 * on the platform and keyboard layout. They are generated whenever a key is
 * pressed or released, and are the only way to find out about keys that don't
 * generate character input (e.g., action keys, modifier keys, etc.). The key
 * being pressed or released is indicated by the code variable, which contains
 * a virtual key code.
 *
 * <p>
 * For triggering context menus see the {@link ContextMenuEvent}.
 * @since JavaFX 2.0
 */
public final class KeyEvent extends InputEvent {

    private static final long serialVersionUID = 20121107L;

    /**
     * Common supertype for all key event types.
     */
    public static final EventType<KeyEvent> ANY =
            new EventType<KeyEvent>(InputEvent.ANY, "KEY");

    /**
     * This event occurs when a key has been pressed.
     */
    public static final EventType<KeyEvent> KEY_PRESSED =
            new EventType<KeyEvent>(KeyEvent.ANY, "KEY_PRESSED");

    /**
     * This event occurs when a key has been released.
     */
    public static final EventType<KeyEvent> KEY_RELEASED =
            new EventType<KeyEvent>(KeyEvent.ANY, "KEY_RELEASED");

    /**
     * This event occurs when a character-generating key was typed
     * (pressed and released).  The event contains the {@code character}
     * field containing the typed string, the {@code code} and {@code text}
     * fields are not used.
     */
    public static final EventType<KeyEvent> KEY_TYPED =
            new EventType<KeyEvent>(KeyEvent.ANY, "KEY_TYPED");

    /**
     * Constructs a new {@code KeyEvent} event from the specified parameters.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param character The character or sequence of characters associated with the event
     * @param text A String describing the key code
     * @param code The integer key code
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @since JavaFX 8.0
     */
    public KeyEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target, @NamedArg("eventType") EventType<KeyEvent> eventType, @NamedArg("character") String character,
            @NamedArg("text") String text, @NamedArg("code") KeyCode code, @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown) {
        super(source, target, eventType);
        boolean isKeyTyped = eventType == KEY_TYPED;

        this.character = isKeyTyped ? character : KeyEvent.CHAR_UNDEFINED;
        this.text = isKeyTyped ? "" : text;
        this.code = isKeyTyped ? KeyCode.UNDEFINED : code;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
    }

    /**
     * Constructs a new {@code KeyEvent} event from the specified parameters,
     * with a {@code null} source and target.
     * @param eventType The type of the event.
     * @param character The character or sequence of characters associated with the event
     * @param text A String describing the key code
     * @param code The integer key code
     * @param shiftDown true if shift modifier was pressed.
     * @param controlDown true if control modifier was pressed.
     * @param altDown true if alt modifier was pressed.
     * @param metaDown true if meta modifier was pressed.
     * @since JavaFX 8.0
     */
    public KeyEvent(@NamedArg("eventType") EventType<KeyEvent> eventType, @NamedArg("character") String character,
            @NamedArg("text") String text, @NamedArg("code") KeyCode code, @NamedArg("shiftDown") boolean shiftDown, @NamedArg("controlDown") boolean controlDown,
            @NamedArg("altDown") boolean altDown, @NamedArg("metaDown") boolean metaDown) {
        super(eventType);
        boolean isKeyTyped = eventType == KEY_TYPED;

        this.character = isKeyTyped ? character : KeyEvent.CHAR_UNDEFINED;
        this.text = isKeyTyped ? "" : text;
        this.code = isKeyTyped ? KeyCode.UNDEFINED : code;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.metaDown = metaDown;
    }

    /**
     * KEY_PRESSED and KEY_RELEASED events which do not map to a valid Unicode
     * character use this for the keyChar value.
     */
    public static final String CHAR_UNDEFINED = KeyCode.UNDEFINED.ch;

    /**
     * The Unicode character or sequence of characters associated with the key
     * typed event. Contains multiple elements if the key produced a single
     * Unicode character from outside of the Basic Multilingual Plane which
     * needs to be encoded by the corresponding surrogate pair in Java or if
     * the key produced multiple Unicode characters itself.
     * <p>
     * For example, {@code character} will have the value "A" for a key typed
     * event generated by pressing SHIFT + 'a'.
     * For key pressed and key released events, {@code character} is always
     * {@code CHAR_UNDEFINED}.
     */
    private final String character;

    /**
     * The Unicode character or sequence of characters associated with the key
     * typed event. Contains multiple elements if the key produced a single
     * Unicode character from outside of the Basic Multilingual Plane which
     * needs to be encoded by the corresponding surrogate pair in Java or if
     * the key produced multiple Unicode characters itself.
     * <p>
     * For example, {@code character} will have the value "A" for a key typed
     * event generated by pressing SHIFT + 'a'.
     * For key pressed and key released events, {@code character} is always
     * {@code CHAR_UNDEFINED}.
     *
     * @return The Unicode character(s) associated with the key typed event
     */
    public final String getCharacter() {
        return character;
    }

    /**
     * A String describing the key code, such as "HOME", "F1" or "A",
     * for key pressed and key released events.
     * For key typed events, {@code text} is always the empty string.
     */
    private final String text;

    /**
     * A String describing the key code, such as "HOME", "F1" or "A",
     * for key pressed and key released events.
     * For key typed events, {@code text} is always the empty string.
     *
     * @return A String describing the key code
     */
    public final String getText() {
        return text;
    }

    /**
     * The integer key code associated with the key in this key
     * pressed or key released event.
     * For key typed events, {@code code} is always {@code KeyCode.UNDEFINED}.
     */
    private final KeyCode code;

    /**
     * The key code associated with the key in this key pressed or key released
     * event. For key typed events, {@code code} is always {@code KeyCode.UNDEFINED}.
     *
     * @return The key code associated with the key in this event,
     * {@code KeyCode.UNDEFINED} for key typed event
     */
    public final KeyCode getCode() {
        return code;
    }

    /**
     * Returns whether or not the Shift modifier is down on this event.
     */
    private final boolean shiftDown;

    /**
     * Returns whether or not the Shift modifier is down on this event.
     * @return whether or not the Shift modifier is down on this event.
     */
    public final boolean isShiftDown() {
        return shiftDown;
    }

    /**
     * Returns whether or not the Control modifier is down on this event.
     */
    private final boolean controlDown;

    /**
     * Returns whether or not the Control modifier is down on this event.
     * @return whether or not the Control modifier is down on this event.
     */
    public final boolean isControlDown() {
        return controlDown;
    }

    /**
     * Returns whether or not the Alt modifier is down on this event.
     */
    private final boolean altDown;

    /**
     * Returns whether or not the Alt modifier is down on this event.
     * @return whether or not the Alt modifier is down on this event.
     */
    public final boolean isAltDown() {
        return altDown;
    }

    /**
     * Returns whether or not the Meta modifier is down on this event.
     */
    private final boolean metaDown;

    /**
     * Returns whether or not the Meta modifier is down on this event.
     * @return whether or not the Meta modifier is down on this event.
     */
    public final boolean isMetaDown() {
        return metaDown;
    }


    /**
     * Returns whether or not the host platform common shortcut modifier is
     * down on this event. This common shortcut modifier is a modifier key which
     * is used commonly in shortcuts on the host platform. It is for example
     * {@code control} on Windows and {@code meta} (command key) on Mac.
     *
     * @return {@code true} if the shortcut modifier is down, {@code false}
     *      otherwise
     */
    public final boolean isShortcutDown() {
        switch (Toolkit.getToolkit().getPlatformShortcutKey()) {
            case SHIFT:
                return shiftDown;

            case CONTROL:
                return controlDown;

            case ALT:
                return altDown;

            case META:
                return metaDown;

            default:
                return false;
        }
    }

    /**
     * Returns a string representation of this {@code KeyEvent} object.
     * @return a string representation of this {@code KeyEvent} object.
     */
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("KeyEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", character = ").append(getCharacter());
        sb.append(", text = ").append(getText());
        sb.append(", code = ").append(getCode());

        if (isShiftDown()) {
            sb.append(", shiftDown");
        }
        if (isControlDown()) {
            sb.append(", controlDown");
        }
        if (isAltDown()) {
            sb.append(", altDown");
        }
        if (isMetaDown()) {
            sb.append(", metaDown");
        }
        if (isShortcutDown()) {
            sb.append(", shortcutDown");
        }

        return sb.append("]").toString();
    }

    @Override
    public KeyEvent copyFor(Object newSource, EventTarget newTarget) {
        return (KeyEvent) super.copyFor(newSource, newTarget);
    }

    /**
     * Creates a copy of the given event with the given fields substituted.
     * @param source the new source of the copied event
     * @param target the new target of the copied event
     * @param type the new event type.
     * @return the event copy with the fields substituted
     * @since JavaFX 8.0
     */
    public KeyEvent copyFor(Object source, EventTarget target, EventType<KeyEvent> type) {
        KeyEvent e = copyFor(source, target);
        e.eventType = type;
        return e;
    }

    @Override
    public EventType<KeyEvent> getEventType() {
        return (EventType<KeyEvent>) super.getEventType();
    }



}
