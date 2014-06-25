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

package javafx.scene.accessibility;

import java.time.LocalDate;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;

/**
 * Experimental API - Do not use (will be removed).
 *
 * @treatAsPrivate
 */
public enum Attribute {

    /**
     * Returns the accelerator for the Node.
     * Type: KeyCombination
     */
    ACCELERATOR("Accelerator", KeyCombination.class),

    /**
     * Returns the bounds for the Node.
     * Type: Bounds
     */
    BOUNDS("Bounds", Bounds.class),

    /**
     * Returns the array of bounding rectangles for the given char range.
     * Parameter: Integer range start
     * Parameter: Integer range length
     * Type: Bounds[]
     */
    BOUNDS_FOR_RANGE("BoundsForRange", Bounds[].class),

    /**
     * Returns the caret offset for the node.
     * Type: Integer
     */
    CARET_OFFSET("CaretOffset", Integer.class),

    /**
     * Returns the children for the Node.
     * Type: ObservableList&lt;Node&gt;
     */
    CHILDREN("Children", ObservableList.class),

    /**
     * Returns the column at the given index
     * Parameter: Integer
     * Type: Node
     */
    COLUMN_AT_INDEX("ColumnAtIndex", Node.class),

    /**
     * Returns the cell at the given row and column indices
     * Parameter: Integer, Integer
     * Type: Node
     */
    CELL_AT_ROW_COLUMN("CellAtRowColumn", Node.class),

    /**
     * Returns the column count
     * Type: Integer
     */
    COLUMN_COUNT("ColumnCount", Integer.class),

    /**
     * Returns the column index of a cell
     * Type: Integer
     */
    COLUMN_INDEX("ColumnIndex", Integer.class),

    /**
     * Returns the contents of a scroll pane
     * Type: Node
     */
    CONTENTS("Contents", Node.class),

    /**
     * Returns the description for the Node.
     * Type: String
     */
    DESCRIPTION("Description", String.class),

    /**
     * Returns true if the Node is disabled.
     * Type: Boolean
     */
    DISABLED("Disabled", Boolean.class),

    /**
     * Depth of a row in a disclosure hierarchy.
     * Type: Integer
     */
    DISCLOSURE_LEVEL("DisclosureLevel", Integer.class),

    /**
     * Returns the local date for the node.
     * Type: LocalDate
     */
    DATE("Date", LocalDate.class),

    /**
     * Returns true if the node is editable.
     * Type: Boolean
     */
    EDITABLE("Editable", Boolean.class),

    /**
     * Indicates if a popup is expanded.
     * Type: Boolean
     */
    EXPANDED("Expanded", Boolean.class),

    /**
     * Returns the focus item.
     * Type: Node
     *
     * Used for controls with items such TabPaneView, TableView, ListView, etc.
     * It returns the exact Node within the control that has the focus.
     */
    FOCUS_ITEM("FocusItem", Node.class),

    /**
     * Returns the focus Node.
     * Type: Node
     *
     * This attribute is requested to the Scene, where it maps to {@link Scene#focusOwnerProperty()}
     * The Scene can delegate the request to its current transient focus container.
     */
    FOCUS_NODE("FocusNode", Node.class),

    /**
     * Returns true if the Node is focused.
     * Type: Boolean
     */
    FOCUSED("Focused", Boolean.class),

    /**
     * Returns the font for the node
     * Type: Font
     */
    FONT("Font", Font.class),

    /**
     * Returns the header for the node
     * Type: Node
     */
    HEADER("Header", Node.class),

    /**
     * Returns the help for the Node.
     * Type: String
     */
    HELP("Help", String.class),

    /**
     * Returns the horizontal scroll bar of a scroll pane
     * Type: Node
     */
    HORIZONTAL_SCROLLBAR("HorizontalScrollBar", Node.class),

    /**
     * Returns the indeterminate state for the node.
     * Type: Boolean
     */
    INDETERMINATE("Indeterminate", Boolean.class),

    /**
     * Returns the index of a row or column
     * Type: Integer
     */
    INDEX("Index", Integer.class),

    /**
     * Returns a Node that is a label for this control, or null.
     * Type: Node
     */
    LABELED_BY("LabeledBy", Node.class),

    /**
     * Indicates whether a TreeItem is a leaf element or not.
     * Type: Boolean
     */
    LEAF("Leaf", Boolean.class),

    /**
     * Returns the line start of the given line index.
     * Parameter: Integer - line index
     * Type: Integer
     */
    LINE_END("LineEnd", Integer.class),

    /**
     * Returns the line index of the given character offset.
     * Parameter: Integer - character offset.
     * Type: Integer
     */
    LINE_FOR_OFFSET("LineOffset", Integer.class),

    /**
     * Returns the line end of the given line index.
     * Parameter: Integer - line index
     * Type: Integer
     */
    LINE_START("LineStart", Integer.class),

    /**
     * Returns the min value for the node.
     * Type: Double
     */
    MIN_VALUE("MinValue", Double.class),

    /**
     * Returns the max value for the node.
     * Type: Double
     */
    MAX_VALUE("MaxValue", Double.class),

    /**
     * Returns the Menu.
     * Type: Node
     */
    MENU("Menu", Node.class),

    /**
     * Returns the owner Menu.
     * Type: Node
     */
    MENU_FOR("MenuFor", Node.class),

    /**
     * Returns the mnemonic for the node.
     * Type: String
     */
    MNEMONIC("Mnemonic", String.class),

    /**
     * Returns whether the control allows for multiple selection.
     * Type: Boolean
     */
    MULTIPLE_SELECTION("MultipleSelection", Boolean.class),

    /**
     * Returns the Node at the given point location.
     * Type: Node
     * Parameters: Point2D
     */
    NODE_AT_POINT("NodeAtPoint", Node.class),

    /**
     * Returns the char offset at the given point location.
     * Type: Integer
     * Parameters: Point2D
     */
    OFFSET_AT_POINT("OffsetAtPoint", Integer.class),

    /**
     * Returns the orientation of a node
     * Type: Orientation
     */
    ORIENTATION("Orientation", Orientation.class),

    /**
     * Return the overflow button for the Node.
     * Type: Node
     */
    OVERFLOW_BUTTON("Overflow Button", Node.class),

    /**
     * Returns the pages for the Node.
     * Type: ObservableList&lt;Node&gt;
     */
    PAGES("Pages", ObservableList.class),

    /**
     * Returns the parent for the Node.
     * Type: Parent
     */
    PARENT("Parent", Parent.class),

    /**
     * Returns the role for the Node.
     * Type: Role
     */
    ROLE("Role", Role.class),

    /**
     * Returns the row at the given index
     * Parameter: Integer
     * Type: Node
     */
    ROW_AT_INDEX("RowAtIndex", Node.class),

    /**
     * Returns the row count
     * Type: Integer
     */
    ROW_COUNT("RowCount", Integer.class),

    /**
     * Returns the row index of a cell
     * Type: Integer
     */
    ROW_INDEX("RowIndex", Integer.class),

    /**
     * Returns the scene for the Node.
     * Type: Scene
     */
    SCENE("Scene", Scene.class),

    /**
     * Returns if the item is selected (in a radio group for example)
     * Type: Boolean
     */
    SELECTED("Selected", Boolean.class),

    /**
     * Returns the list of selected cells
     * Type: ObservableList&lt;Node&gt;
     */
    SELECTED_CELLS("SelectedCells", ObservableList.class),

    /**
     * Returns the selected pagination page item.
     * Type: Node
     */
    SELECTED_PAGE("SelectedPage", Node.class),

    /**
     * Returns the list of selected rows
     * Type: ObservableList&lt;Node&gt;
     */
    SELECTED_ROWS("SelectedRows", ObservableList.class),

    /**
     * Returns the selected tab item.
     * Type: Node
     */
    SELECTED_TAB("SelectedTab", Node.class),

    /**
     * Returns the selection end for the node.
     * Type: Integer
     */
    SELECTION_END("SelectionEnd", Integer.class),

    /**
     * Returns the selection start for the node.
     * Type: Integer
     */
    SELECTION_START("SelectionStart", Integer.class),

    /**
     * Returns the tabs for the Node.
     * Type: ObservableList&lt;Node&gt;
     */
    TABS("Tabs", ObservableList.class),

    /**
     * Returns the title for the Node.
     * E.g.
     * <ul>
     * <li>ComboBox returns a string representation of its
     * currently selected item.
     * <li>TextField returns the text currently entered into it.
     * </ul>
     * Type: String
     */
    TITLE("Title", String.class),

    /**
     * Returns a tree item (Role.TREE_ITEM or Role.TREE_TABLE_ITEM) at the given
     * index, relative to the tree item that this is called on. If this is called
     * on a container (e.g. TreeView or TreeTableView), it will be called on the
     * root tree item.
     * Parameter: Integer
     * Type: Node
     */
    TREE_ITEM_AT_INDEX("TreeItemAtIndex", Node.class),

    /**
     * Returns the number of tree items that are expanded descendants of the current
     * tree item. If requested on a container (e.g. TreeView or TreeTableView),
     * this will return the count from the root tree item.
     * Type: Node
     */
    TREE_ITEM_COUNT("TreeItemCount", Integer.class),

    /**
     * Returns the parent of a Role.TREE_ITEM (as another Role.TREE_ITEM, or if
     * there is no parent (e.g. it is the root node)), then return the parent
     * node whatever it is (most probably Role.TREE_VIEW or Role.TREE_TABLE_VIEW)
     * Type: Node
     */
    TREE_ITEM_PARENT("TreeItemParent", Node.class),

    /**
     * Returns the value for the node.
     * Type: Double
     */
    VALUE("Value", Double.class),

    /**
     * Returns the vertical scroll bar of a scroll pane
     * Type: Node
     */
    VERTICAL_SCROLLBAR("VerticalScrollBar", Node.class),

    /**
     * Returns if the visibility for the node.
     * Type: Boolean
     */
    VISIBLE("VISIBLE", Boolean.class),

    /**
     * Indicates whether a Hyperlink has been visited or not.
     * This is an undocumented Mac-only attribute.
     * Type: Boolean
     */
    VISITED("Visited", Boolean.class),
    ;

    private String name;
    private Class<?> returnClass;

    Attribute(String name, Class<?> returnClass) {
        this.name = name;
        this.returnClass = returnClass;
    }

    public String getName() {
        return name;
    }

    public Class<?> getReturnType() {
        return returnClass;
    }
}
