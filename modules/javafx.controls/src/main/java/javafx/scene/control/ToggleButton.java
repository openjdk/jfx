/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.scene.traversal.ParentTraversalEngine;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WritableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.css.PseudoClass;

import javafx.scene.control.skin.ToggleButtonSkin;

import javafx.css.StyleableProperty;

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
 * <pre><code> ToggleButton tb1 = new ToggleButton("toggle button 1");
 * ToggleButton tb2 = new ToggleButton("toggle button 2");
 * ToggleButton tb3 = new ToggleButton("toggle button 3");
 * ToggleGroup group = new ToggleGroup();
 * tb1.setToggleGroup(group);
 * tb2.setToggleGroup(group);
 * tb3.setToggleGroup(group);</code></pre>
 *
 * <img src="doc-files/ToggleButton.png" alt="Image of the ToggleButton control">
 *
 * <p>
 * MnemonicParsing is enabled by default for ToggleButton.
 * </p>
 * @since JavaFX 2.0
 */

// TODO Mention the semantics when binding "selected" on multiple toggle buttons
// which are all on the same toggle group, and how the selected state on the
// toggle group is affected or not in such a case.

 public class ToggleButton extends ButtonBase implements Toggle {

    /* *************************************************************************
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
        setAccessibleRole(AccessibleRole.TOGGLE_BUTTON);
        // alignment is styleable through css. Calling setAlignment
        // makes it look to css like the user set the value and css will not
        // override. Initializing alignment by calling set on the
        // CssMetaData ensures that css will be able to override the value.
        ((StyleableProperty<Pos>)(WritableValue<Pos>)alignmentProperty()).applyStyle(null, Pos.CENTER);
        setMnemonicParsing(true);     // enable mnemonic auto-parsing by default
    }
    /* *************************************************************************
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
                    final boolean selected = get();
                    final ToggleGroup tg = getToggleGroup();
                    // Note: these changes need to be done before selectToggle/clearSelectedToggle since
                    // those operations change properties and can execute user code, possibly modifying selected property again
                    pseudoClassStateChanged(PSEUDO_CLASS_SELECTED, selected);
                    notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTED);
                    if (tg != null) {
                        if (selected) {
                            tg.selectToggle(ToggleButton.this);
                        } else if (tg.getSelectedToggle() == ToggleButton.this) {
                            tg.clearSelectedToggle();
                        }
                    }
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
                private ChangeListener<Toggle> listener = (o, oV, nV) ->
                    ParentHelper.getTraversalEngine(ToggleButton.this).setOverriddenFocusTraversability(nV != null ? isSelected() : null);

                @Override protected void invalidated() {
                    final ToggleGroup tg = get();
                    if (tg != null && !tg.getToggles().contains(ToggleButton.this)) {
                        if (old != null) {
                            old.getToggles().remove(ToggleButton.this);
                        }
                        tg.getToggles().add(ToggleButton.this);
                        final ParentTraversalEngine parentTraversalEngine = new ParentTraversalEngine(ToggleButton.this);
                        ParentHelper.setTraversalEngine(ToggleButton.this, parentTraversalEngine);
                        // If there's no toggle selected, do not override
                        parentTraversalEngine.setOverriddenFocusTraversability(tg.getSelectedToggle() != null ? isSelected() : null);
                        tg.selectedToggleProperty().addListener(listener);
                    } else if (tg == null) {
                        old.selectedToggleProperty().removeListener(listener);
                        old.getToggles().remove(ToggleButton.this);
                        ParentHelper.setTraversalEngine(ToggleButton.this, null);
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

    /* *************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void fire() {
        // TODO (aruiz): if (!isReadOnly(isSelected()) {
        if (!isDisabled()) {
            setSelected(!isSelected());
            fireEvent(new ActionEvent());
        }
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ToggleButtonSkin(this);
    }


    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "toggle-button";
    private static final PseudoClass PSEUDO_CLASS_SELECTED =
            PseudoClass.getPseudoClass("selected");

    /**
     * Returns the initial alignment state of this control, for use
     * by the JavaFX CSS engine to correctly set its initial value. This method
     * is overridden to use Pos.CENTER initially.
     *
     * @since 9
     */
    @Override protected Pos getInitialAlignment() {
        return Pos.CENTER;
    }


    /* *************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        switch (attribute) {
            case SELECTED: return isSelected();
            default: return super.queryAccessibleAttribute(attribute, parameters);
        }
    }
}
