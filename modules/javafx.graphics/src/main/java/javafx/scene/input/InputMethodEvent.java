/*
 * Copyright (c) 2009, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.NamedArg;
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
 * {@link javafx.scene.control.TextInputControl}, when the text under composition
 * (composed text) is generated/changed/removed, the input method commits
 * the result text, or the input method caret position changes.
 * <p>
 * On receiving this event, the application is supposed to display
 * the composed text with any visual feedback attributes to the user.
 * <p>
 * Note: this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#INPUT_METHOD ConditionalFeature.INPUT_METHOD}
 * for more information.
 * @since JavaFX 2.0
 */
public final class InputMethodEvent extends InputEvent{

    private static final long serialVersionUID = 20121107L;

    /**
     * The only valid EventType for the InputMethodEvent.
     */
    public static final EventType<InputMethodEvent> INPUT_METHOD_TEXT_CHANGED =
            new EventType<>(InputEvent.ANY, "INPUT_METHOD_TEXT_CHANGED");

    /**
     * Common supertype for all input method event types.
     * @since JavaFX 8.0
     */
    public static final EventType<InputMethodEvent> ANY = INPUT_METHOD_TEXT_CHANGED;

    /**
     * Constructs new InputMethodEvent event.
     * @param source the source of the event. Can be null.
     * @param target the target of the event. Can be null.
     * @param eventType The type of the event.
     * @param composed the text under composition
     * @param committed the text that is committed as a result of composition
     * @param caretPosition the current position of the caret.
     * @since JavaFX 8.0
     */
    public InputMethodEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target, @NamedArg("eventType") EventType<InputMethodEvent> eventType,
            @NamedArg("composed") List<InputMethodTextRun> composed, @NamedArg("committed") String committed,
            @NamedArg("caretPosition") int caretPosition) {
        super(source, target, eventType);
        this.composed = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(composed));
        this.committed = committed;
        this.caretPosition = caretPosition;
    }

    /**
     * Constructs new InputMethodEvent event with empty source and target.
     * @param eventType The type of the event.
     * @param composed the text under composition
     * @param committed the text that is committed as a result of composition
     * @param caretPosition the current position of the caret.
     * @since JavaFX 8.0
     */
    public InputMethodEvent(@NamedArg("eventType") EventType<InputMethodEvent> eventType,
            @NamedArg("composed") List<InputMethodTextRun> composed, @NamedArg("committed") String committed,
            @NamedArg("caretPosition") int caretPosition) {
        this(null, null, eventType, composed, committed, caretPosition);
    }


    /**
     * The text under composition.  This text should be displayed with the
     * appropriate visual feedback that represents the {@link InputMethodHighlight}s
     * attached to each run.
     *
     * @defaultValue null
     */
    private transient ObservableList<InputMethodTextRun> composed;

    /**
     * Gets the text under composition.  This text should be displayed with the
     * appropriate visual feedback that represents the {@link InputMethodHighlight}s
     * attached to each run.
     *
     * @return The text under composition
     */
    public final ObservableList<InputMethodTextRun> getComposed() {
        return composed;
    }

    /**
     * The text that is committed by the input method as the result of the
     * composition.
     *
     * @defaultValue empty string
     */
    private final String committed;

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
    private final int caretPosition;

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

    @Override
    public InputMethodEvent copyFor(Object newSource, EventTarget newTarget) {
        return (InputMethodEvent) super.copyFor(newSource, newTarget);
    }

    @Override
    public EventType<InputMethodEvent> getEventType() {
        return (EventType<InputMethodEvent>) super.getEventType();
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        oos.writeObject(new ArrayList(composed));
    }

    private void readObject(ObjectInputStream ois) throws IOException,
            ClassNotFoundException {
        ois.defaultReadObject();
        ArrayList<InputMethodTextRun> o = (ArrayList)ois.readObject();
        composed = FXCollections.unmodifiableObservableList(FXCollections.observableArrayList(o));
    }

}
