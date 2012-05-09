/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package javafx.scene.input;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;

/**
 * An event which indicates that the underlying input method notifies its
 * text change in a {@link Node}.
 * <p>
 * This event is delivered to the {@link Node} object that extends 
 * {@link javafx.scene.control.TextInput}, when the text under composition (composed text) is
 * generated/changed/removed, the input method commits
 * the result text, or the input method caret position changes.
 * <p>
 * On receiving this event, the application is supposed to display
 * the composed text with any visual feedback attributes to the user.
 * <p>
 * Note: this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#INPUT_METHOD ConditionalFeature.INPUT_METHOD}
 * for more information.
 */
public class InputMethodEvent extends InputEvent {
    /**
     * The only valid EventType for the InputMethodEvent.
     */
    public static final EventType<InputMethodEvent> INPUT_METHOD_TEXT_CHANGED =
            new EventType<InputMethodEvent>(InputEvent.ANY, "INPUT_METHOD_TEXT_CHANGED");

    private InputMethodEvent(final EventType<? extends InputMethodEvent> eventType) {
        super(eventType);
    }

    private InputMethodEvent(final Object source,
                             final EventTarget target,
                             final EventType<? extends InputMethodEvent> eventType) {
        super(source, target, eventType);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static InputMethodEvent impl_copy(EventTarget target,
                                             InputMethodEvent evt) {
        return (InputMethodEvent) evt.copyFor(evt.source, target);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static InputMethodEvent impl_inputMethodEvent(EventTarget target,
            ObservableList<InputMethodTextRun> composed, String committed,
            int caretPosition, EventType<? extends InputMethodEvent> eventType) {
        InputMethodEvent e = new InputMethodEvent(null, target, eventType);
        e.getComposed().addAll(composed);
        e.committed = committed;
        e.caretPosition = caretPosition;
        return e;
    }

    /**
     * The text under composition.  This text should be displayed with the
     * appropriate visual feedback that represents the {@link InputMethodHighlight}s
     * attached to each run.
     *
     * @defaultValue null
     */
    private ObservableList<InputMethodTextRun> composed;

    /**
     * Gets the text under composition.  This text should be displayed with the
     * appropriate visual feedback that represents the {@link InputMethodHighlight}s
     * attached to each run.
     *
     * @return The text under composition
     */
    public final ObservableList<InputMethodTextRun> getComposed() {
        if (composed == null) {
            composed = FXCollections.observableArrayList();
        }
        return composed;
    }

    /**
     * The text that is committed by the input method as the result of the
     * composition.
     *
     * @defaultValue empty string
     */
    private String committed = "";

    /**
     * Gets the text that is committed by the input method as the result of the
     * composition.
     *
     * @return The committed text
     */
    public final String getCommitted() {
        return committed;
    }

    /**
     * The input method caret position within the composed text.
     * If the position is -1, the caret should be invisible.
     *
     * @defaultValue 0
     */
    private int caretPosition;

    /**
     * The input method caret position within the composed text.
     * If the position is -1, the caret should be invisible.
     *
     * @return The input method caret position within the composed text
     */
    public final int getCaretPosition() {
        return caretPosition;
    }

    /**
     * Returns a string representation of this {@code InputMethodEvent} object.
     * @return a string representation of this {@code InputMethodEvent} object.
     */ 
    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("InputMethodEvent [");

        sb.append("source = ").append(getSource());
        sb.append(", target = ").append(getTarget());
        sb.append(", eventType = ").append(getEventType());
        sb.append(", consumed = ").append(isConsumed());

        sb.append(", composed = ").append(getComposed());
        sb.append(", committed = ").append(getCommitted());
        sb.append(", caretPosition = ").append(getCaretPosition());

        return sb.append("]").toString();
    }

}
