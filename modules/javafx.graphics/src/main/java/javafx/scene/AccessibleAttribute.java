/*
 * Copyright (c) 2014, 2023, Oracle and/or its affiliates. All rights reserved.
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
 * This enum describes the attributes that an assistive technology
 * such as a screen reader can request from the scene graph.
 *
 * The {@link AccessibleRole} dictates the set of attributes that
 * the screen reader will request for a particular control.  For
 * example, a slider is expected to return a double that represents
 * the current value.
 * <p>
 * Attributes may have any number of parameters, depending on the particular attribute.</p>
 * <p>
 * When the value of an attribute is changed by a node, it must notify the assistive technology
 * using {@link Node#notifyAccessibleAttributeChanged(AccessibleAttribute)}.  This will allow
 * the screen reader to inform the user that a value has changed.  The most common form of
 * notification is focus change.
 * </p>
 *
 * @see Node#queryAccessibleAttribute(AccessibleAttribute, Object...)
 * @see Node#notifyAccessibleAttributeChanged(AccessibleAttribute)
 * @see AccessibleRole
 * @see AccessibleAttribute#ROLE
 *
 * @since JavaFX 8u40
 */
public enum AccessibleAttribute {

    /**
     * Returns the accelerator for the node.
     * <ul>
     * <li>Used by: Menu, MenuItem, RadioMenuItem, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link KeyCombination} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    ACCELERATOR(KeyCombination.class),

    /**
     * Returns the bounds for the node.
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Bounds} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    BOUNDS(Bounds.class),

    /**
     * Returns the array of bounding rectangles for the given character range.
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Bounds}[] </li>
     * <li>Parameters:
     *   <ul>
     *    <li>{@link Integer} the start offset </li>
     *    <li>{@link Integer} the end offset </li>
     *   </ul>
     * </li>
     * </ul>
     */
    BOUNDS_FOR_RANGE(Bounds[].class),

    /**
     * Returns the caret offset for the node.
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    CARET_OFFSET(Integer.class),

    /**
     * Returns the children for the node.
     * <ul>
     * <li>Used by: Parent </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link javafx.collections.ObservableList}&lt;{@link Node}&gt; </li>
     * <li>Parameters: </li>
     * </ul>
     */
    CHILDREN(ObservableList.class),

    /**
     * Returns the column at the given index.
     * <ul>
     * <li>Used by: TableView and TreeTableView </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters:
     *   <ul>
     *    <li>{@link Integer} the index </li>
     *   </ul>
     * </li>
     * </ul>
     */
    COLUMN_AT_INDEX(Node.class),

    /**
     * Returns the cell at the given row and column indices.
     * <ul>
     * <li>Used by: TableView and TreeTableView </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters:
     *   <ul>
     *    <li>{@link Integer} the row index </li>
     *    <li>{@link Integer} the column index </li>
     *   </ul>
     * </li>
     * </ul>
     */
    CELL_AT_ROW_COLUMN(Node.class),

    /**
     * Returns the column count for the node.
     * <ul>
     * <li>Used by: TableView and TreeTableView </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    COLUMN_COUNT(Integer.class),

    /**
     * Returns the column index for the node.
     * <ul>
     * <li>Used by: TableCell and TreeTableCell </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    COLUMN_INDEX(Integer.class),

    /**
     * Returns the contents of the node.
     * <ul>
     * <li>Used by: ScrollPane </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    CONTENTS(Node.class),

    /**
     * Returns true if the node is disabled, otherwise false.
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    DISABLED(Boolean.class),

    /**
     * Returns the depth of a row in the disclosure hierarchy.
     * <ul>
     * <li>Used by: TreeItem and TreeTableRow </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    DISCLOSURE_LEVEL(Integer.class),

    /**
     * Returns the local date for the node.
     * <ul>
     * <li>Used by: DatePicker </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link LocalDate} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    DATE(LocalDate.class),

    /**
     * Returns true if the node is editable, otherwise false.
     * <ul>
     * <li>Used by: TextField, ComboBox, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    EDITABLE(Boolean.class),

    /**
     * Returns true if the node is expanded, otherwise false.
     * <ul>
     * <li>Used by: TreeItem, TitledPane, and others </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    EXPANDED(Boolean.class),

    /**
     * Returns the focus item.
     * <p>
     * Used for controls such as TabPane, TableView, ListView
     * where the assistive technology focus is different from the
     * normal focus node.  For example, a table control will have focus,
     * while a cell inside the table might have the screen reader focus.
     * </p>
     * <ul>
     * <li>Used by: ListView, TabPane, and others </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    FOCUS_ITEM(Node.class),

    /**
     * Returns the focus node.
     * Type: Node
     * <p>
     * When this attribute is requested from the Scene, the default implementation
     * returns {@link Scene#focusOwnerProperty()}.
     * </p>
     * <ul>
     * <li>Used by: Scene </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    FOCUS_NODE(Node.class),

    /**
     * Returns true if the node is focused, otherwise false.
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    FOCUSED(Boolean.class),

    /**
     * Returns the font for the node.
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Font} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    FONT(Font.class),

    /**
     * Returns the header for the node.
     * <ul>
     * <li>Used by: TableView and TreeTableView </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    HEADER(Node.class),

    /**
     * Returns the help text for the node.
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link String} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    HELP(String.class),

    /**
     * Returns the horizontal scroll bar for the node.
     * <ul>
     * <li>Used by: ListView, ScrollPane, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    HORIZONTAL_SCROLLBAR(Node.class),

    /**
     * Returns true of the node is indeterminaite, otherwise false.
     * <ul>
     * <li>Used by: CheckBox and ProgressIndicator </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    INDETERMINATE(Boolean.class),

    /**
     * Returns {@link ToggleState toggle state} of CheckBox of CheckBoxTreeItem.
     * <ul>
     * <li>Used by: CheckBoxTreeItem</li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link ToggleState}
     *   <ul>
     *    <li>{@link ToggleState#UNCHECKED ToggleState.UNCHECKED}: control is not selected</li>
     *    <li>{@link ToggleState#CHECKED ToggleState.CHECKED}: control is selected</li>
     *    <li>{@link ToggleState#INDETERMINATE ToggleState.INDETERMINATE}:
     *                                     selection state of control cannot be determined</li>
     *   </ul>
     * </li>
     * <li>Parameters: </li>
     * </ul>
     *
     * @see ToggleState
     * @since 21
     */
    TOGGLE_STATE(ToggleState.class),

    /**
     * Returns the item at the given index.
     * <ul>
     * <li>Used by: TabPane, ListView, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters:
     *   <ul>
     *   <li> {@link Integer} the index </li>
     *   </ul>
     * </li>
     * </ul>
     */
    ITEM_AT_INDEX(Node.class),

    /**
     * Returns the item count for the node.
     * <ul>
     * <li>Used by: TabPane, ListView, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    ITEM_COUNT(Integer.class),

    /**
     * Returns the index for the node.
     * <ul>
     * <li>Used by: ListItem, TableRow, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    INDEX(Integer.class),

    /**
     * Returns the node that is the label for this node.
     * <p>When {@link javafx.scene.control.Label#labelForProperty() labelFor} is set,
     * the default implementation of {@code LABELED_BY} uses this
     * relationship to return the appropriate node to the screen
     * reader.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    LABELED_BY(Node.class),

    /**
     * Returns true if the node is a leaf element, otherwise false.
     * <ul>
     * <li>Used by: TreeItem and TreeTableRow </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    LEAF(Boolean.class),

    /**
     * Returns the line end offset of the given line index.
     * <ul>
     * <li>Used by: TextArea </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters:
     *   <ul>
     *   <li> {@link Integer} the line index </li>
     *   </ul>
     * </li>
     * </ul>
     */
    LINE_END(Integer.class),

    /**
     * Returns the line index of the given character offset.
     * <ul>
     * <li>Used by: TextArea </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters:
     *   <ul>
     *   <li> {@link Integer} the character offset </li>
     *   </ul>
     * </li>
     * </ul>
     */
    LINE_FOR_OFFSET(Integer.class),

    /**
     * Returns the line start offset of the given line index.
     * <ul>
     * <li>Used by: TextArea </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters:
     *   <ul>
     *   <li> {@link Integer} the line index </li>
     *   </ul>
     * </li>
     * </ul>
     */
    LINE_START(Integer.class),

    /**
     * Returns the minimum value for the node.
     * <ul>
     * <li>Used by: Slider, ScrollBar, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Double} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    MIN_VALUE(Double.class),

    /**
     * Returns the maximum value for the node.
     * <ul>
     * <li>Used by: Slider, ScrollBar, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Double} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    MAX_VALUE(Double.class),

    /**
     * Returns the mnemonic for the node.
     * <ul>
     * <li>Used by: Menu, MenuItem, CheckMenuItem, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link String} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    MNEMONIC(String.class),

    /**
     * Returns true if the node allows for multiple selection, otherwise false.
     * <ul>
     * <li>Used by: ListView, TableView, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    MULTIPLE_SELECTION(Boolean.class),

    /**
     * Returns the node at the given location.
     * <ul>
     * <li>Used by: Scene </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters:
     *   <ul>
     *   <li> {@link javafx.geometry.Point2D} the point location </li>
     *   </ul>
     * </li>
     * </ul>
     */
    NODE_AT_POINT(Node.class),

    /**
     * Returns the character offset at the given location.
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters:
     *   <ul>
     *   <li> {@link javafx.geometry.Point2D} the point location </li>
     *   </ul>
     * </li>
     * </ul>
     */
    OFFSET_AT_POINT(Integer.class),

    /**
     * Returns the orientation of the node.
     * <ul>
     * <li>Used by: ScrolBar and Slider </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link javafx.geometry.Orientation} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    ORIENTATION(Orientation.class),

    /**
     * Return the overflow button for the node.
     * <ul>
     * <li>Used by: Toolbar </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    OVERFLOW_BUTTON(Node.class),

    /**
     * Returns the parent for the node.
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Parent} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    PARENT(Parent.class),

    /**
     * Returns the parent menu for the node.
     * <ul>
     * <li>Used by: ContextMenu </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    PARENT_MENU(Node.class),

    /**
     * Returns the role for the node.
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link AccessibleRole} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    ROLE(AccessibleRole.class),

    /**
     * Returns the role description for the node.
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link String} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    ROLE_DESCRIPTION(String.class),

    /**
     * Returns the row at the given index.
     * <ul>
     * <li>Used by: TableView, TreeView, and TreeTableView </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters:
     *   <ul>
     *   <li> {@link Integer} the row index </li>
     *   </ul>
     * </li>
     * </ul>
     */
    ROW_AT_INDEX(Node.class),

    /**
     * Returns the row count for the node.
     * <ul>
     * <li>Used by: TableView, TreeView, and TreeTableView </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    ROW_COUNT(Integer.class),

    /**
     * Returns the row index of the node.
     * <ul>
     * <li>Used by: TableCell, TreeItem, and TreeTableCell </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    ROW_INDEX(Integer.class),

    /**
     * Returns the scene for the node.
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Scene} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    SCENE(Scene.class),

    /**
     * Returns true if the node is selected, otherwise false.
     * <ul>
     * <li>Used by: CheckBox, TreeItem, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    SELECTED(Boolean.class),

    /**
     * Returns the list of selected items for the node.
     * <ul>
     * <li>Used by: ListView, TableView, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link javafx.collections.ObservableList}&lt;{@link Node}&gt; </li>
     * <li>Parameters: </li>
     * </ul>
     */
    SELECTED_ITEMS(ObservableList.class),

    /**
     * Returns the text selection end offset for the node.
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    SELECTION_END(Integer.class),

    /**
     * Returns the text selection start offset for the node.
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    SELECTION_START(Integer.class),

    /**
     * Returns the sub menu for the node.
     * <ul>
     * <li>Used by: Menu </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    SUBMENU(Node.class),

    /**
     * Returns the text for the node.
     * E.g.
     * <ul>
     * <li>ComboBox returns a string representation of the current selected item.
     * <li>TextField returns the contents of the text field.
     * </ul>
     *
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link String} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    TEXT(String.class),

    /**
     * Returns a tree item at the given index, relative to its TREE_ITEM_PARENT.
     * <ul>
     * <li>Used by: TreeItem and TreeTableRow </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters:
     *   <ul>
     *   <li> {@link Integer} the index </li>
     *   </ul>
     * </li>
     * </ul>
     */
    TREE_ITEM_AT_INDEX(Node.class),

    /**
     * Returns the tree item count for the node, relative to its TREE_ITEM_PARENT.
     * <ul>
     * <li>Used by: TreeItem and TreeTableRow </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    TREE_ITEM_COUNT(Integer.class),

    /**
     * Returns the parent item for the item, or null if the item is the root.
     * <ul>
     * <li>Used by: TreeItem and TreeTableRow </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    TREE_ITEM_PARENT(Node.class),

    /**
     * Returns the value for the node.
     * <ul>
     * <li>Used by: Slider, ScrollBar, Thumb, and others </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Double} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    VALUE(Double.class),

    /**
     * Returns the vertical scroll bar for the node.
     * <ul>
     * <li>Used by: ListView, ScrollPane, and others </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    VERTICAL_SCROLLBAR(Node.class),

    /**
     * Returns true if node is visible, otherwise false.
     * <ul>
     * <li>Used by: Node and ContextMenu </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    VISIBLE(Boolean.class),

    /**
     * Returns true if the node has been visited, otherwise false.
     * <ul>
     * <li>Used by: Hyperlink </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: </li>
     * </ul>
     */
    VISITED(Boolean.class),
    ;

    private Class<?> returnClass;

    AccessibleAttribute(Class<?> returnClass) {
        this.returnClass = returnClass;
    }

    /**
     * Gets the type of {@code AccessibleAttribute}.
     * @return the type of {@code AccessibleAttribute}
     */
    public Class<?> getReturnType() {
        return returnClass;
    }

    /**
     * This enum describes the values for {@link AccessibleAttribute#TOGGLE_STATE TOGGLE_STATE} attribute.
     *
     * @since 21
     */
    public enum ToggleState {
        /**
         * Indicates that the toggle control is not selected.
         */
        UNCHECKED,

        /**
         * Indicates that the toggle control is selected.
         */
        CHECKED,

        /**
         * Indicates that the toggle state of the control cannot be determined.
         */
        INDETERMINATE
    }
}
