/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

/**
 * The {@code AccessibleAction} enum defines the actions assistive technology,
 * such as screen readers, can request nodes in the scene graph.<br>
 * The actions each node must support depends on its {@link AccessibleRole}.
 * <p>Action can be augmented by parameters or not.</p>
 *
 * @see Node#executeAccessibleAction(AccessibleAction, Object...)
 * @see AccessibleRole
 * @see AccessibleAttribute#ROLE
 * 
 * @since JavaFX 8u40
 */
public enum AccessibleAction {

    /**
     * Decrements the node by its larger block decrement value.
     * A smaller decrement can be performed by using {@link #DECREMENT}.
     * <p>Used by Slider, ScrollBar, etc </p>
     * 
     * Parameters:
     * <ul>
     * </ul>
     */
    BLOCK_DECREMENT,

    /**
     * Increments the node by its larger block increment value. 
     * A smaller increment can be performed by using {@link #INCREMENT}.
     * <p>Used by Slider, ScrollBar, etc </p>
     * 
     * Parameters:
     * <ul>
     * </ul>
     */
    BLOCK_INCREMENT,

    /**
     * Request the node to collapse itself.
     * <p>Used by TreeItem, TitledPane, etc </p>
     * 
     * Parameters:
     * <ul>
     * </ul>
     */
    COLLAPSE,

    /**
     * Decrements the node by its smaller unit decrement value.
     * A larger decrement can be performed by using {@link #BLOCK_DECREMENT}.
     * <p>Used by Slider, ScrollBar, etc </p>
     * 
     * Parameters:
     * <ul>
     * </ul>
     */
    DECREMENT,

    /**
     * Request the node to expand itself.
     * <p>Used by TreeItem, TitledPane, etc </p>
     * 
     * Parameters:
     * <ul>
     * </ul>
     */
    EXPAND,

    /**
     * Fire the node.
     * <p>Used by Button, Hyperlink, etc </p>
     * 
     * Parameters:
     * <ul>
     * </ul>
     */
    FIRE,

    /**
     * Increments the node by its smaller unit increment value.
     * A larger increment can be performed by using {@link #BLOCK_INCREMENT}.
     * <p>Used by Slider, ScrollBar, etc </p>
     * 
     * Parameters:
     * <ul>
     * </ul>
     */
    INCREMENT,

    /**
     * Indicate the node to request focus.
     * <p>Used by Node, TabItem, etc </p>
     * 
     * Parameters:
     * <ul>
     * </ul>
     */
    REQUEST_FOCUS,

    /**
     * Requests the view to show an item, scrolling if required.
     * <p>Used by ListView, TreeView, etc </p>
     * 
     * Parameters:
     * <ul>
     * <li> {@link Node} the item to show </li>
     * </ul>
     */
    SHOW_ITEM,

    /**
     * Requests the view to show the given text range, scrolling if required.
     * <p>Used by TextField and TextArea. </p>
     * 
     * Parameters:
     * <ul>
     * <li> {@link java.lang.Integer} the start offset </li>
     * <li> {@link java.lang.Integer} the end offset </li>
     * </ul>
     */
    SHOW_TEXT_RANGE,

    /**
     * Requests the view to sets its selection to the given items.
     * Parameter: ObservableList&lt;Node&gt;
     * <p>Used by ListView, TreeView, etc </p>
     * 
     * Parameters:
     * <ul>
     * <li> {@link javafx.collections.ObservableList}&lt;{@link Node}&gt; the items to select </li>
     * </ul>
     */
    SET_SELECTED_ITEMS,

    /**
     * Sets the text selection for a node.
     * <p>Used by TextField and TextArea. </p>
     * 
     * Parameters:
     * <ul>
     * <li> {@link java.lang.Integer} the start offset </li>
     * <li> {@link java.lang.Integer} the end offset </li>
     * </ul>
     */
    SET_TEXT_SELECTION,

    /**
     * Sets the text for a node.
     * <p>Used by TextField and TextArea. </p>
     * 
     * Parameters:
     * <ul>
     * <li> {@link String} the new text</li>
     * </ul>
     */
    SET_TEXT,

    /**
     * Sets the value for a node.
     * <p>Used by Slider, Scrollbars, etc </p>
     * 
     * Parameters:
     * <ul>
     * <li> {@link java.lang.Double} the new value </li>
     * </ul>
     */
    SET_VALUE,

    /**
     * Request the receiver to show its menu.
     * <p>Used by Node. </p>
     * 
     * Parameters:
     * <ul>
     * </ul>
     */
    SHOW_MENU,
}
