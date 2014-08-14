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

import java.time.LocalDate;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;

/**
 * Experimental API - Do not use (will be removed).
 *
 * @treatAsPrivate
 */
public enum AccessibleAttribute {

    /**
     * Returns the accelerator for the Node.
     * Type: KeyCombination
     */
    ACCELERATOR(KeyCombination.class),

    /**
     * Returns the bounds for the Node.
     * Type: Bounds
     */
    BOUNDS(Bounds.class),

    /**
     * Returns the array of bounding rectangles for the given char range.
     * Parameter: Integer range start
     * Parameter: Integer range length
     * Type: Bounds[]
     */
    BOUNDS_FOR_RANGE(Bounds[].class),

    /**
     * Returns the caret offset for the node.
     * Type: Integer
     */
    CARET_OFFSET(Integer.class),

    /**
     * Returns the children for the Node.
     * Type: ObservableList&lt;Node&gt;
     */
    CHILDREN(ObservableList.class),

    /**
     * Returns the column at the given index
     * Parameter: Integer
     * Type: Node
     */
    COLUMN_AT_INDEX(Node.class),

    /**
     * Returns the cell at the given row and column indices
     * Parameter: Integer, Integer
     * Type: Node
     */
    CELL_AT_ROW_COLUMN(Node.class),

    /**
     * Returns the column count
     * Type: Integer
     */
    COLUMN_COUNT(Integer.class),

    /**
     * Returns the column index of a cell
     * Type: Integer
     */
    COLUMN_INDEX(Integer.class),

    /**
     * Returns the contents of a scroll pane
     * Type: Node
     */
    CONTENTS(Node.class),

    /**
     * Returns true if the Node is disabled.
     * Type: Boolean
     */
    DISABLED(Boolean.class),

    /**
     * Depth of a row in a disclosure hierarchy.
     * Type: Integer
     */
    DISCLOSURE_LEVEL(Integer.class),

    /**
     * Returns the local date for the node.
     * Type: LocalDate
     */
    DATE(LocalDate.class),

    /**
     * Returns true if the node is editable.
     * Type: Boolean
     */
    EDITABLE(Boolean.class),

    /**
     * Indicates if a popup is expanded.
     * Type: Boolean
     */
    EXPANDED(Boolean.class),

    /**
     * Returns the focus item.
     * Type: Node
     *
     * Used for controls with items such TabPaneView, TableView, ListView, etc.
     * It returns the exact Node within the control that has the focus.
     */
    FOCUS_ITEM(Node.class),

    /**
     * Returns the focus Node.
     * Type: Node
     *
     * This attribute is requested to the Scene, where it maps to {@link Scene#focusOwnerProperty()}
     * The Scene can delegate the request to its current transient focus container.
     */
    FOCUS_NODE(Node.class),

    /**
     * Returns true if the Node is focused.
     * Type: Boolean
     */
    FOCUSED(Boolean.class),

    /**
     * Returns the font for the node
     * Type: Font
     */
    FONT(Font.class),

    /**
     * Returns the header for the node
     * Type: Node
     */
    HEADER(Node.class),

    /**
     * Returns the help for the Node.
     * Type: String
     */
    HELP(String.class),

    /**
     * Returns the horizontal scroll bar of a scroll pane
     * Type: Node
     */
    HORIZONTAL_SCROLLBAR(Node.class),

    /**
     * Returns the indeterminate state for the node.
     * Type: Boolean
     */
    INDETERMINATE(Boolean.class),

    /**
     * Returns the item at the given index
     * Parameter: Integer
     * Type: Node
     */
    ITEM_AT_INDEX(Node.class),

    /**
     * Returns the item count
     * Type: Integer
     */
    ITEM_COUNT(Integer.class),

    /**
     * Returns the index of a row or column
     * Type: Integer
     */
    INDEX(Integer.class),

    /**
     * Returns a Node that is a label for this control, or null.
     * Type: Node
     */
    LABELED_BY(Node.class),

    /**
     * Indicates whether a TreeItem is a leaf element or not.
     * Type: Boolean
     */
    LEAF(Boolean.class),

    /**
     * Returns the line start of the given line index.
     * Parameter: Integer - line index
     * Type: Integer
     */
    LINE_END(Integer.class),

    /**
     * Returns the line index of the given character offset.
     * Parameter: Integer - character offset.
     * Type: Integer
     */
    LINE_FOR_OFFSET(Integer.class),

    /**
     * Returns the line end of the given line index.
     * Parameter: Integer - line index
     * Type: Integer
     */
    LINE_START(Integer.class),

    /**
     * Returns the min value for the node.
     * Type: Double
     */
    MIN_VALUE(Double.class),

    /**
     * Returns the max value for the node.
     * Type: Double
     */
    MAX_VALUE(Double.class),

    /**
     * Returns the mnemonic for the node.
     * Type: String
     */
    MNEMONIC(String.class),

    /**
     * Returns whether the control allows for multiple selection.
     * Type: Boolean
     */
    MULTIPLE_SELECTION(Boolean.class),

    /**
     * Returns the Node at the given point location.
     * Type: Node
     * Parameters: Point2D
     */
    NODE_AT_POINT(Node.class),

    /**
     * Returns the char offset at the given point location.
     * Type: Integer
     * Parameters: Point2D
     */
    OFFSET_AT_POINT(Integer.class),

    /**
     * Returns the orientation of a node
     * Type: Orientation
     */
    ORIENTATION(Orientation.class),

    /**
     * Return the overflow button for the Node.
     * Type: Node
     */
    OVERFLOW_BUTTON(Node.class),

    /**
     * Returns the parent for the Node.
     * Type: Parent
     */
    PARENT(Parent.class),

    /**
     * Returns the parent menu.
     * Type: Node
     */
    PARENT_MENU(Node.class),

    /**
     * Returns the role for the Node.
     * Type: Role
     */
    ROLE(AccessibleRole.class),

    /**
     * Returns the role description for the Node.
     * Type: String
     */
    ROLE_DESCRIPTION(String.class),

    /**
     * Returns the row at the given index
     * Parameter: Integer
     * Type: Node
     */
    ROW_AT_INDEX(Node.class),

    /**
     * Returns the row count
     * Type: Integer
     */
    ROW_COUNT(Integer.class),

    /**
     * Returns the row index of a cell
     * Type: Integer
     */
    ROW_INDEX(Integer.class),

    /**
     * Returns the scene for the Node.
     * Type: Scene
     */
    SCENE(Scene.class),

    /**
     * Returns if the item is selected (in a radio group for example)
     * Type: Boolean
     */
    SELECTED(Boolean.class),

    /**
     * Returns the list of selected items
     * Type: ObservableList&lt;Node&gt;
     */
    SELECTED_ITEMS(ObservableList.class),

    /**
     * Returns the selection end for the node.
     * Type: Integer
     */
    SELECTION_END(Integer.class),

    /**
     * Returns the selection start for the node.
     * Type: Integer
     */
    SELECTION_START(Integer.class),

    /**
     * Returns the sub menu.
     * Type: Node
     */
    SUBMENU(Node.class),

    /**
     * Returns the text for the Node.
     * E.g.
     * <ul>
     * <li>ComboBox returns a string representation of its
     * currently selected item.
     * <li>TextField returns the text currently entered into it.
     * </ul>
     * Type: String
     */
    TEXT(String.class),

    /**
     * Returns a tree item (Role.TREE_ITEM or Role.TREE_TABLE_ITEM) at the given
     * index, relative to the tree item that this is called on. If this is called
     * on a container (e.g. TreeView or TreeTableView), it will be called on the
     * root tree item.
     * Parameter: Integer
     * Type: Node
     */
    TREE_ITEM_AT_INDEX(Node.class),

    /**
     * Returns the number of tree items that are expanded descendants of the current
     * tree item. If requested on a container (e.g. TreeView or TreeTableView),
     * this will return the count from the root tree item.
     * Type: Node
     */
    TREE_ITEM_COUNT(Integer.class),

    /**
     * Returns the parent of a Role.TREE_ITEM (as another Role.TREE_ITEM, or if
     * there is no parent (e.g. it is the root node)), then return the parent
     * node whatever it is (most probably Role.TREE_VIEW or Role.TREE_TABLE_VIEW)
     * Type: Node
     */
    TREE_ITEM_PARENT(Node.class),

    /**
     * Returns the value for the node.
     * Type: Double
     */
    VALUE(Double.class),

    /**
     * Returns the vertical scroll bar of a scroll pane
     * Type: Node
     */
    VERTICAL_SCROLLBAR(Node.class),

    /**
     * Returns if the visibility for the node.
     * Type: Boolean
     */
    VISIBLE(Boolean.class),

    /**
     * Indicates whether a Hyperlink has been visited or not.
     * This is an undocumented Mac-only attribute.
     * Type: Boolean
     */
    VISITED(Boolean.class),
    ;

    private Class<?> returnClass;

    AccessibleAttribute(Class<?> returnClass) {
        this.returnClass = returnClass;
    }

    public Class<?> getReturnType() {
        return returnClass;
    }
}
