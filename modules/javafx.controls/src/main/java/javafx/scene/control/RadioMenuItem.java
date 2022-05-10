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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;

/**
 * <p>
 * A RadioMenuItem is a {@link MenuItem} that can be toggled (it uses
 * the {@link javafx.scene.control.Toggle Toggle} mixin). This means that
 * RadioMenuItem has an API very similar in nature to other controls that use
 * {@link javafx.scene.control.Toggle Toggle}, such as
 * {@link javafx.scene.control.RadioButton} and
 * {@link javafx.scene.control.ToggleButton}. RadioMenuItem is
 * specifically designed for use within a {@code Menu}, so refer to that class
 * API documentation for more information on how to add a RadioMenuItem into it.
 * <p>
 * To create a simple, ungrouped RadioMenuItem, do the following:
 *
<pre><code>RadioMenuItem radioItem = new RadioMenuItem("radio text");
radioItem.setSelected(false);
radioItem.setOnAction(e {@literal ->} System.out.println("radio toggled"));</code></pre>
 *
 * <p>
 * The problem with the example above is that this offers no benefit over using
 * a normal MenuItem. As already mentioned, the purpose of a
 * RadioMenuItem is to offer
 * multiple choices to the user, and only allow for one of these choices to be
 * selected at any one time (i.e. the selection should be <i>mutually exclusive</i>).
 * To achieve this, you can place zero or more RadioMenuItem's into groups. When
 * in groups, only one RadioMenuItem at a time within that group can be selected.
 * To put two RadioMenuItem instances into the same group, simply assign them
 * both the same value for {@code toggleGroup}. For example:
 *
<pre><code>ToggleGroup toggleGroup = new ToggleGroup();

RadioMenuItem radioItem1 = new RadioMenuItem("Option 1");
radioItem1.setOnAction(e {@literal ->} System.out.println("radio1 toggled"));
radioItem1.setToggleGroup(toggleGroup);

RadioMenuItem radioItem2 = new RadioMenuItem("Option 2");
radioItem2.setOnAction(e {@literal ->} System.out.println("radio2 toggled"));
radioItem2.setToggleGroup(toggleGroup);

Menu menu = new Menu("Selection");
menu.getItems().addAll(radioItem1, radioItem2);
MenuBar menuBar = new MenuBar(menu);</code></pre>
 *
 * <img src="doc-files/RadioMenuItem.png" alt="Image of the RadioMenuItem control">
 *
 * <p>
 * In this example, with both RadioMenuItem's assigned to the same
 * {@link javafx.scene.control.ToggleGroup ToggleGroup}, only one item may be
 * selected at any one time, and should
 * the selection change, the ToggleGroup will take care of deselecting the
 * previous item.
 *
 * @see MenuItem
 * @see Menu
 * @since JavaFX 2.0
 */
public class RadioMenuItem extends MenuItem implements Toggle {

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructs a RadioMenuItem with no display text.
     */
    public RadioMenuItem() {
        this(null,null);
    }

    /**
     * Constructs a RadioMenuItem and sets the display text with the specified text.
     * @param text the display text
     */
    public RadioMenuItem(String text) {
        this(text,null);
    }

    /**
     * Constructs a RadioMenuItem and sets the display text with the specified text
     * and sets the graphic {@link Node} to the given node.
     * @param text the display text
     * @param graphic the graphic node
     */
    public RadioMenuItem(String text, Node graphic) {
        super(text,graphic);
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- Toggle Group
    /**
     * Represents the {@link ToggleGroup} that this RadioMenuItem belongs to.
     */
    private ObjectProperty<ToggleGroup> toggleGroup;
    @Override public final void setToggleGroup(ToggleGroup value) {
        toggleGroupProperty().set(value);
    }

    @Override public final ToggleGroup getToggleGroup() {
        return toggleGroup == null ? null : toggleGroup.get();
    }

    @Override public final ObjectProperty<ToggleGroup> toggleGroupProperty() {
        if (toggleGroup == null) {
            toggleGroup = new ObjectPropertyBase<ToggleGroup>() {
                private ToggleGroup old;
                @Override protected void invalidated() {
                    if (old != null) {
                        old.getToggles().remove(RadioMenuItem.this);
                    }
                    old = get();
                    if (get() != null && !get().getToggles().contains(RadioMenuItem.this)) {
                        get().getToggles().add(RadioMenuItem.this);
                    }
                }

                @Override
                public Object getBean() {
                    return RadioMenuItem.this;
                }

                @Override
                public String getName() {
                    return "toggleGroup";
                }
            };
        }
        return toggleGroup;
    }


    // --- Selected
    private BooleanProperty selected;
    @Override public final void setSelected(boolean value) {
        selectedProperty().set(value);
    }

    @Override public final boolean isSelected() {
        return selected == null ? false : selected.get();
    }

    @Override public final BooleanProperty selectedProperty() {
        if (selected == null) {
            selected = new BooleanPropertyBase() {
                @Override protected void invalidated() {
                    if (getToggleGroup() != null) {
                        if (get()) {
                            getToggleGroup().selectToggle(RadioMenuItem.this);
                        } else if (getToggleGroup().getSelectedToggle() == RadioMenuItem.this) {
                            getToggleGroup().clearSelectedToggle();
                        }
                    }

                    if (isSelected()) {
                        getStyleClass().add(STYLE_CLASS_SELECTED);
                    } else {
                        getStyleClass().remove(STYLE_CLASS_SELECTED);
                    }
                }

                @Override
                public Object getBean() {
                    return RadioMenuItem.this;
                }

                @Override
                public String getName() {
                    return "selected";
                }
            };
        }
        return selected;
    }



    /* *************************************************************************
     *                                                                         *
     * Inherited Public API                                                    *
     *                                                                         *
     **************************************************************************/


    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "radio-menu-item";
    private static final String STYLE_CLASS_SELECTED = "selected";
}
