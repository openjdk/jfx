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

package javafx.scene.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableProperty;

/**
 * A {@code ToggleButton} is a specialized control which has the ability to be
 * selected. Typically a {@code ToggleButton} is rendered similarly to a Button.
 * However, they are two different types of Controls. A Button is a "command"
 * button which invokes a function when clicked. A {@code ToggleButton} on the
 * other hand is simply a control with a Boolean indicating whether it has been
 * selected.
 * <p>
 * {@code ToggleButton} can also be placed in groups. By default, a
 * {@code ToggleButton} is not in a group. When in groups, only one
 * {@code ToggleButton} at a time within that group can be selected. To put two
 * {@code ToggleButtons} in the same group, simply assign them both the same
 * value for {@link ToggleGroup}.
 * </p>
 * <p>
 * Unlike {@link RadioButton RadioButtons}, {@code ToggleButtons} in a
 * {@code ToggleGroup} do not attempt to force at least one selected
 * {@code ToggleButton} in the group. That is, if a {@code ToggleButton} is
 * selected, clicking on it will cause it to become unselected. With
 * {@code RadioButton}, clicking on the selected button in the group will have
 * no effect.
 * </p>
 *
 * <p>Example:</p>
 * <pre><code>
 * ToggleButton tb1 = new ToggleButton("toggle button 1");
 * ToggleButton tb2 = new ToggleButton("toggle button 2");
 * ToggleButton tb3 = new ToggleButton("toggle button 3");
 * ToggleGroup group = new ToggleGroup();
 * tb1.setToggleGroup(group);
 * tb2.setToggleGroup(group);
 * tb3.setToggleGroup(group);
 * </code></pre>
 *
 * <p>
 * MnemonicParsing is enabled by default for ToggleButton.
 * </p>
 */

// TODO Mention the semantics when binding "selected" on multiple toggle buttons
// which are all on the same toggle group, and how the selected state on the
// toggle group is affected or not in such a case.

 public class ToggleButton extends ButtonBase implements Toggle {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a toggle button with an empty string for its label.
     */
    public ToggleButton() {
        initialize();
    }

    /**
     * Creates a toggle button with the specified text as its label.
     *
     * @param text A text string for its label.
     */
    public ToggleButton(String text) {
        setText(text);
        initialize();
    }

    /**
     * Creates a toggle button with the specified text and icon for its label.
     *
     * @param text A text string for its label.
     * @param graphic the icon for its label.
     */
    public ToggleButton(String text, Node graphic) {
        setText(text);
        setGraphic(graphic);
        initialize();
    }

    private void initialize() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        // alignment is styleable through css. Calling setAlignment
        // makes it look to css like the user set the value and css will not 
        // override. Initializing alignment by calling set on the 
        // StyleableProperty ensures that css will be able to override the value.
        final StyleableProperty prop = StyleableProperty.getStyleableProperty(alignmentProperty());
        prop.set(this, Pos.CENTER);
        setMnemonicParsing(true);     // enable mnemonic auto-parsing by default
    }
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * Indicates whether this toggle button is selected. This can be manipulated
     * programmatically.
     */
    private BooleanProperty selected;
    public final void setSelected(boolean value) {
        selectedProperty().set(value);
    }

    public final boolean isSelected() {
        return selected == null ? false : selected.get();
    }

    public final BooleanProperty selectedProperty() {
        if (selected == null) {
            selected = new BooleanPropertyBase() {
                @Override protected void invalidated() {
                    if (getToggleGroup() != null) {
                        if (get()) {
                            getToggleGroup().selectToggle(ToggleButton.this);
                        } else if (getToggleGroup().getSelectedToggle() == ToggleButton.this) {
                            getToggleGroup().clearSelectedToggle();
                        }
                    }
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_SELECTED);
                }

                @Override
                public Object getBean() {
                    return ToggleButton.this;
                }

                @Override
                public String getName() {
                    return "selected";
                }
            };
        }
        return selected;
    }
    /**
     * The {@link ToggleGroup} to which this {@code ToggleButton} belongs. A
     * {@code ToggleButton} can only be in one group at any one time. If the
     * group is changed, then the button is removed from the old group prior to
     * being added to the new group.
     */
    private ObjectProperty<ToggleGroup> toggleGroup;
    public final void setToggleGroup(ToggleGroup value) {
        toggleGroupProperty().set(value);
    }

    public final ToggleGroup getToggleGroup() {
        return toggleGroup == null ? null : toggleGroup.get();
    }

    public final ObjectProperty<ToggleGroup> toggleGroupProperty() {
        if (toggleGroup == null) {
            toggleGroup = new ObjectPropertyBase<ToggleGroup>() {                
                private ToggleGroup old;
                @Override protected void invalidated() {
                    final ToggleGroup tg = get();
                    if (tg != null && !tg.getToggles().contains(ToggleButton.this)) {
                        if (old != null) {
                            old.getToggles().remove(ToggleButton.this);
                        }
                        tg.getToggles().add(ToggleButton.this);
                    } else if (tg == null) {
                        old.getToggles().remove(ToggleButton.this);
                    }
                    old = tg;
                }

                @Override
                public Object getBean() {
                    return ToggleButton.this;
                }

                @Override
                public String getName() {
                    return "toggleGroup";
                }
            };
        }
        return toggleGroup;
    }

    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void fire() {
        // TODO (aruiz): if (!isReadOnly(isSelected()) {
        setSelected(!isSelected());
        fireEvent(new ActionEvent());
    }

    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "toggle-button";
    private static final String PSEUDO_CLASS_SELECTED = "selected";

    private static final long SELECTED_PSEUDOCLASS_STATE = StyleManager.getInstance().getPseudoclassMask("selected");

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        if (isSelected()) mask |= SELECTED_PSEUDOCLASS_STATE;
        return mask;
    }
    
     /**
      * Not everything uses the default value of false for alignment. 
      * This method provides a way to have them return the correct initial value.
      * @treatAsPrivate implementation detail
      */
    @Deprecated @Override
    protected Pos impl_cssGetAlignmentInitialValue() {
        return Pos.CENTER;
    }
        
}
