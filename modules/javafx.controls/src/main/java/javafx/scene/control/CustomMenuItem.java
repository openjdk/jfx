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

package javafx.scene.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

/**
 * <p>
 * A {@link MenuItem} that allows for arbitrary nodes to be embedded within it,
 * by assigning a {@link Node} to the {@link #contentProperty() content} property.
 * CustomMenuItem attempts to make arbitrary nodes
 * appear more natural in the Menu. For
 * example, you automatically get hover highlighting when the user hovers their
 * mouse over the node (or navigates to it using keyboard or other alternate means).
 * <p>
 * A useful property of this class is
 * {@link #hideOnClickProperty() hideOnClick}. This boolean specifies whether the CustomMenuItem
 * will be automatically hidden should the user click within the bounds of the
 * CustomMenuItem.
 * This is of particular use when the node inside this CustomMenuItem is a
 * control that requires user interaction, as by setting the hideOnClick
 * property to false means the user can interact with it without the menu hiding.
 * Note that by default hideOnClick is true.
 * <p>
 * An example of how to use CustomMenuItem is shown below.
<pre><code>
CustomMenuItem customMenuItem = new CustomMenuItem(new Slider());
customMenuItem.setHideOnClick(false);
</code></pre>
 * <p>
 * If this CustomMenuItem, with the {@link Slider} within it, is placed in a
 * menu, you'll be presented with a slider that is horizontally centered when
 * the menu is visible, and interacting with the slider will not result in the
 * menu disappearing.
 *
 * @see MenuItem
 * @see Menu
 * @since JavaFX 2.0
 */
public class CustomMenuItem extends MenuItem {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default CustomMenuItem instance.
     */
    public CustomMenuItem() {
        this(null, true);
    }

    /**
     * Constructs a CustomMenuItem and initializes its content with the node specified.
     * @param node to be embedded inside this CustomMenuItem
     */
    public CustomMenuItem(Node node) {
        this(node, true);
    }

    /**
     * Constructs a CustomMenuItem and sets the content to the node specified.
     * @param node to be embedded inside this CustomMenuItem
     * @param hideOnClick if false the menu will not hide when the user interacts with the node.
     */
    public CustomMenuItem(Node node, boolean hideOnClick) {
        getStyleClass().add(DEFAULT_STYLE_CLASS);

        setContent(node);
        setHideOnClick(hideOnClick);
    }



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * The node to display within this CustomMenuItem.
     */
    private ObjectProperty<Node> content;

    public final void setContent(Node value) {
        contentProperty().set(value);
    }

    public final Node getContent() {
        return content == null ? null : content.get();
    }

    public final ObjectProperty<Node> contentProperty() {
        if (content == null) {
            content = new SimpleObjectProperty<Node>(this, "content");
        }
        return content;
    }


    /**
     * If true, this menu item, and all visible menus, will be hidden when this
     * menu item is clicked on.
     *
     * @defaultValue true
     */
    private BooleanProperty hideOnClick;

    public final void setHideOnClick(boolean value) {
        hideOnClickProperty().set(value);
    }

    public final boolean isHideOnClick() {
        return hideOnClick == null ? true : hideOnClick.get();
    }

    public final BooleanProperty hideOnClickProperty() {
        if (hideOnClick == null) {
            hideOnClick = new SimpleBooleanProperty(this, "hideOnClick", true);
        }
        return hideOnClick;
    }



    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "custom-menu-item";
}
