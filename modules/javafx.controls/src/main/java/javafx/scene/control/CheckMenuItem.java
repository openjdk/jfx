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
import javafx.scene.Node;

/**
 * <p>
 * A {@link MenuItem} that can be toggled between selected and unselected states.
 * It is intended that CheckMenuItem be used in conjunction with the
 * {@link Menu} or {@link ContextMenu} controls.
 * <p>
 * Creating and inserting a CheckMenuItem into a Menu is shown below.
<pre><code>CheckMenuItem subsystem1 = new CheckMenuItem("Enabled");
subsystem1.setOnAction(e {@literal ->} System.out.println("subsystem1 #1 Enabled!"));

Menu menu = new Menu("Subsystems");
menu.getItems().add(subsystem1);
MenuBar menuBar = new MenuBar(menu);</code></pre>
 *
 * <img src="doc-files/CheckMenuItem.png" alt="Image of the CheckMenuItem control">
 *
 * <p>
 * Of course, the approach shown above separates out the definition of the
 * CheckMenuItem from the Menu, but this needn't be so.
 * <p>
 * To ascertain the current state of the CheckMenuItem, you should refer to the
 * {@link #selectedProperty() selected} boolean. An example use case may be the following example:
<pre><code>final checkMenuItem = new CheckMenuItem("Show Widget");
subsystem1.setOnAction(e {@literal ->} System.out.println("Show the widget!"));
private final BooleanProperty widgetShowing();
public final boolean isWidgetShowing() { return widgetShowing.get(); )
public final void setWidgetShowing(boolean value) {
    widgetShowingProperty().set(value);
}
public final BooleanProperty widgetShowingProperty() {
    if (widgetShowing == null) {
        widgetShowing = new SimpleBooleanProperty(this, "widgetShowing", true);
    }
    return widgetShowing;
}

widgetShowing.bind(checkMenuItem.selected);</code></pre>
 *
 * <p>
 * Typically a CheckMenuItem will be rendered such that, when selected, it shows
 * a check (or tick) mark in the area normally reserved for the MenuItem
 * graphic. Of course, this will vary depending on the skin and styling specified.
 *
 * @see Menu
 * @see MenuItem
 * @see RadioMenuItem
 *
 * @since JavaFX 2.0
 */
public class CheckMenuItem extends MenuItem {

    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    /**
     * Creates an empty {@code CheckMenuItem}.
     */
    public CheckMenuItem() {
        this(null,null);
    }

    /**
     * Constructs a CheckMenuItem and sets the display text with the specified text.
     * @param text the display text
     */
    public CheckMenuItem(String text) {
        this(text,null);
    }

    /**
     * Constructs a CheckMenuItem and sets the display text with the specified text
     * and sets the graphic {@link Node} to the given node.
     * @param text the display text
     * @param graphic the graphic Node
     */
    public CheckMenuItem(String text, Node graphic) {
        super(text,graphic);
        getStyleClass().add(DEFAULT_STYLE_CLASS);
    }



    /* *************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * Represents the current state of this CheckMenuItem. Bind to this to be
     * informed whenever the user interacts with the CheckMenuItem (and causes the
     * selected state to be toggled).
     *
     * @defaultValue false
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
                    // force validation
                    get();

                    // update the styleclass
                    if (isSelected()) {
                        getStyleClass().add(STYLE_CLASS_SELECTED);
                    } else {
                        getStyleClass().remove(STYLE_CLASS_SELECTED);
                    }
                }

                @Override
                public Object getBean() {
                    return CheckMenuItem.this;
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
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "check-menu-item";
    private static final String STYLE_CLASS_SELECTED = "selected";
}
