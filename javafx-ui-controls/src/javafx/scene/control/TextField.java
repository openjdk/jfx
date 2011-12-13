/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import com.sun.javafx.binding.ExpressionHelper;

/**
 * Text input component that allows a user to enter a single line of
 * unformatted text. Unlike in previous releases of JavaFX, support for multi-line
 * input is not available as part of the TextField control, however this is 
 * the sole-purpose of the {@link TextArea} control. Additionally, if you want
 * a form of rich-text editing, there is also the
 * {@link javafx.scene.web.HTMLEditor HTMLEditor} control.
 * 
 * <p>TextField supports the notion of showing {@link #promptTextProperty() prompt text}
 * to the user when there is no {@link #textProperty() text} already in the 
 * TextField (either via the user, or set programmatically). This is a useful
 * way of informing the user as to what is expected in the text field, without
 * having to resort to {@link Tooltip tooltips} or on-screen {@link Label labels}.
 * 
 * @see TextArea
 */
public class TextField extends TextInputControl {
    // Text field content
    private static final class TextFieldContent implements Content {
        private ExpressionHelper<String> helper = null;
        private StringBuilder characters = new StringBuilder();

        @Override public String get(int start, int end) {
            return characters.substring(start, end);
        }

        @Override public void insert(int index, String text, boolean notifyListeners) {
            text = TextInputControl.filterInput(text, true, true);
            characters.insert(index, text);
            if (notifyListeners) {
                ExpressionHelper.fireValueChangedEvent(helper);
            }
        }

        @Override public void delete(int start, int end, boolean notifyListeners) {
            characters.delete(start, end);
            if (notifyListeners) {
                ExpressionHelper.fireValueChangedEvent(helper);
            }
        }

        @Override public int length() {
            return characters.length();
        }

        @Override public String get() {
            return characters.toString();
        }

        @Override public void addListener(ChangeListener<? super String> changeListener) {
            helper = ExpressionHelper.addListener(helper, this, changeListener);
        }

        @Override public void removeListener(ChangeListener<? super String> changeListener) {
            helper = ExpressionHelper.removeListener(helper, changeListener);
        }

        @Override public String getValue() {
            return get();
        }

        @Override public void addListener(InvalidationListener listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override public void removeListener(InvalidationListener listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }
    }

    /**
     * The default value for {@link #prefColumnCount}.
     */
    public static final int DEFAULT_PREF_COLUMN_COUNT = 12;

    /**
     * Creates a {@code TextField} with empty text content.
     */
    public TextField() {
        this("");
    }

    /**
     * Creates a {@code TextField} with initial text content.
     *
     * @param text A string for text content.
     */
    public TextField(String text) {
        super(new TextFieldContent());
        getStyleClass().add("text-field");
        setText(text);
    }

    /**
     * Returns the character sequence backing the text field's content.
     */
    public CharSequence getCharacters() {
        return ((TextFieldContent)getContent()).characters;
    }


    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The {@code TextField}'s prompt text to display, or
     * <tt>null</tt> if no prompt text is displayed.
     */
    private StringProperty promptText = new SimpleStringProperty(this, "promptText", "") {
        @Override protected void invalidated() {
            // Strip out newlines
            String txt = get();
            if (txt != null && txt.contains("\n")) {
                txt = txt.replace("\n", "");
                set(txt);
            }
        }
    };
    public final StringProperty promptTextProperty() { return promptText; }
    public final String getPromptText() { return promptText.get(); }
    public final void setPromptText(String value) { promptText.set(value); }


    /**
     * The preferred number of text columns. This is used for
     * calculating the {@code TextField}'s preferred width.
     */
    private IntegerProperty prefColumnCount = new IntegerPropertyBase(DEFAULT_PREF_COLUMN_COUNT) {
        @Override
        public void set(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("value cannot be negative.");
            }

            super.set(value);
        }

        @Override
        public Object getBean() {
            return TextField.this;
        }

        @Override
        public String getName() {
            return "prefColumnCount";
        }
    };
    public final IntegerProperty prefColumnCountProperty() { return prefColumnCount; }
    public final int getPrefColumnCount() { return prefColumnCount.getValue(); }
    public final void setPrefColumnCount(int value) { prefColumnCount.setValue(value); }


    /**
     * The action handler associated with this text field, or
     * <tt>null</tt> if no action handler is assigned.
     */
    private ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override
        protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return TextField.this;
        }

        @Override
        public String getName() {
            return "onAction";
        }
    };
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }
    public final EventHandler<ActionEvent> getOnAction() { return onActionProperty().get(); }
    public final void setOnAction(EventHandler<ActionEvent> value) { onActionProperty().set(value); }
}
