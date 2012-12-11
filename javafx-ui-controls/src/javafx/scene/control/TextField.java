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

import com.sun.javafx.beans.annotations.DuplicateInBuilderProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WritableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;

import com.sun.javafx.binding.ExpressionHelper;
import com.sun.javafx.css.*;
import com.sun.javafx.css.converters.*;
import com.sun.javafx.scene.control.skin.TextFieldSkin;


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
@DuplicateInBuilderProperties(properties = {"promptText"})
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
            if (!text.isEmpty()) {
                characters.insert(index, text);
                if (notifyListeners) {
                    ExpressionHelper.fireValueChangedEvent(helper);
                }
            }
        }

        @Override public void delete(int start, int end, boolean notifyListeners) {
            if (end > start) {
                characters.delete(start, end);
                if (notifyListeners) {
                    ExpressionHelper.fireValueChangedEvent(helper);
                }
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
     *
     * The action handler is normally called when the user types the ENTER key.
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

    /**
     * Specifies how the text should be aligned when there is empty
     * space within the TextField.
     */
    public final ObjectProperty<Pos> alignmentProperty() {
        if (alignment == null) {
            alignment = new StyleableObjectProperty<Pos>(Pos.CENTER_LEFT) {

                @Override public CssMetaData getCssMetaData() {
                    return StyleableProperties.ALIGNMENT;
                }

                @Override public Object getBean() {
                    return TextField.this;
                }

                @Override public String getName() {
                    return "alignment";
                }
            };
        }
        return alignment;
    }
    private ObjectProperty<Pos> alignment;
    public final void setAlignment(Pos value) { alignmentProperty().set(value); }
    public final Pos getAlignment() { return alignment == null ? Pos.CENTER_LEFT : alignment.get(); }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new TextFieldSkin(this);
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

     /**
      * @treatAsPrivate implementation detail
      */
    private static class StyleableProperties {
        private static final CssMetaData<TextField, Pos> ALIGNMENT =
            new CssMetaData<TextField, Pos>("-fx-alignment",
                new EnumConverter<Pos>(Pos.class), Pos.CENTER_LEFT ) {

            @Override public boolean isSettable(TextField n) {
                return (n.alignment == null || !n.alignment.isBound());
            }

            @Override public WritableValue<Pos> getWritableValue(TextField n) {
                return n.alignmentProperty();
            }
        };

        private static final List<CssMetaData> STYLEABLES;
        static {
            final List<CssMetaData> styleables =
                new ArrayList<CssMetaData>(Control.getClassCssMetaData());
            Collections.addAll(styleables,
                ALIGNMENT
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData> getCssMetaData() {
        return getClassCssMetaData();
    }
}
