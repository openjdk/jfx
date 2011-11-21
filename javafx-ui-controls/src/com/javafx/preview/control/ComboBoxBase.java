/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.preview.control;

import com.sun.javafx.css.StyleManager;
import javafx.beans.property.*;
import javafx.scene.control.Control;

public abstract class ComboBoxBase<T> extends Control {
    
    public ComboBoxBase() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }
    

    // --- value
    /**
     * The value of this ComboBox is defined as the selected item if the input
     * is not editable, or if it is editable, the most recent user action: 
     * either the text input they have provided (converted via the 
     * StringConverter), or the last item selected from the drop down list.
     */
    public ObjectProperty<T> valueProperty() { return value; }
    private ObjectProperty<T> value = new SimpleObjectProperty<T>(this, "value");
    public final void setValue(T value) { valueProperty().set(value); }
    public final T getValue() { return valueProperty().get(); }
    
    
    // --- editable
    /**
     * Specifies whether the ComboBox allows for user input. When editable is 
     * true, the ComboBox has a text input area that a user may type in to. This
     * input is then available via the {@link #valueProperty() value} property.
     */
    public BooleanProperty editableProperty() { return editable; }
    public final void setEditable(boolean value) { editableProperty().set(value); }
    public final boolean isEditable() { return editableProperty().get(); }
    private BooleanProperty editable = new SimpleBooleanProperty(this, "editable", false) {
        @Override protected void invalidated() {
            impl_pseudoClassStateChanged(PSEUDO_CLASS_EDITABLE);
        }
    };
    
    
    // --- showing
    /**
     * Represents the current state of the ComboBox popup, and whether it is 
     * currently visible on screen (although it may be hidden behind other windows).
     */
    public BooleanProperty showingProperty() { return showing; }
    public final boolean isShowing() { return showingProperty().get(); }
    private BooleanProperty showing = new SimpleBooleanProperty(this, "showing", false) {
        @Override protected void invalidated() {
            impl_pseudoClassStateChanged(PSEUDO_CLASS_SHOWING);
        }
    };
    
    
    // --- prompt text
    /**
     * The {@code ComboBox} prompt text to display, or <tt>null</tt> if no 
     * prompt text is displayed.
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
    
    
    // --- armed
    public BooleanProperty armedProperty() { return armed; }
    private final void setArmed(boolean value) { armedProperty().set(value); }
    public final boolean isArmed() { return armedProperty().get(); }
    private BooleanProperty armed = new SimpleBooleanProperty(this, "armed", false) {
        @Override protected void invalidated() {
            impl_pseudoClassStateChanged(PSEUDO_CLASS_ARMED);
        }
    };
    
    
    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Opens the list popup.
     */
    public void show() {
        if (!isDisabled()) showing.set(true);
    }

    /**
     * Closes the list popup.
     */
    public void hide() {
        showing.set(false);
    }
    
    /**
     * Arms the ComboBox. An armed ComboBox will show a popup list on the next 
     * expected UI gesture.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins or Behaviors. It is not common
     *         for developers or designers to access this function directly.
     */
    public void arm() {
        setArmed(true);
    }

    /**
     * Disarms the ComboBox. See {@link #arm()}.
     *
     * @expert This function is intended to be used by experts, primarily
     *         by those implementing new Skins or Behaviors. It is not common
     *         for developers or designers to access this function directly.
     */
    public void disarm() {
        setArmed(false);
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "combo-box-base";
    
    private static final String PSEUDO_CLASS_EDITABLE = "editable";
    private static final String PSEUDO_CLASS_SHOWING = "showing";
    private static final String PSEUDO_CLASS_ARMED = "armed";
    
    private static final long PSEUDO_CLASS_EDITABLE_MASK
            = StyleManager.getInstance().getPseudoclassMask(PSEUDO_CLASS_EDITABLE);
    private static final long PSEUDO_CLASS_SHOWING_MASK
            = StyleManager.getInstance().getPseudoclassMask(PSEUDO_CLASS_SHOWING);
    private static final long PSEUDO_CLASS_ARMED_MASK
            = StyleManager.getInstance().getPseudoclassMask(PSEUDO_CLASS_ARMED);
    
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        if (isEditable()) mask |= PSEUDO_CLASS_EDITABLE_MASK;
        if (isShowing()) mask |= PSEUDO_CLASS_SHOWING_MASK;
        if (isArmed()) mask |= PSEUDO_CLASS_ARMED_MASK;
        return mask;
    }
}
