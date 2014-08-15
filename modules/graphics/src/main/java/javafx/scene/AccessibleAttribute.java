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
 * The {@code AccessibleAttribute} enum defines the attributes assistive technology,
 * such as screen readers, can request nodes in the scene graph.<br>
 * The attributes each node must support depends on its {@link AccessibleRole}.
 * <p>Attributes can be augmented by parameters or not.</p>
 * <p>The node is responsible by
 *  notifying the assistive technology using {@link Node#notifyAccessibleAttributeChanged(AccessibleAttribute)}
 *  when the value of some attributes changes.
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
     * Returns the accelerator for the node.</p>
     * <ul>
     * <li>Used by: Menu, MenuItem, RadioMenuItem, etc. </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link KeyCombination} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    ACCELERATOR(KeyCombination.class),

    /**
     * Returns the bounds for the node.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Bounds} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    BOUNDS(Bounds.class),

    /**
     * Returns the array of bounding rectangles for the given character range.</p>
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
     * Returns the caret offset for the node.</p>
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    CARET_OFFSET(Integer.class),

    /**
     * Returns the children for the node.</p>
     * <ul>
     * <li>Used by: Parent </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link javafx.collections.ObservableList}&lt;{@link Node}&gt; </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    CHILDREN(ObservableList.class),

    /**
     * Returns the column at the given index.</p>
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
     * Returns the cell at the given row and column indices.</p>
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
     * Returns the column count for the node.</p>
     * <ul>
     * <li>Used by: TableView and TreeTableView </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    COLUMN_COUNT(Integer.class),

    /**
     * Returns the column index for the node.</p>
     * <ul>
     * <li>Used by: TableCell and TreeTableCell </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    COLUMN_INDEX(Integer.class),

    /**
     * Returns the contents of the node.</p>
     * <ul>
     * <li>Used by: ScrollPane </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    CONTENTS(Node.class),

    /**
     * Returns if the node is disabled.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    DISABLED(Boolean.class),

    /**
     * Returns the depth of a row in a disclosure hierarchy.</p>
     * <ul>
     * <li>Used by: TreeItem and TreeTableRow </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    DISCLOSURE_LEVEL(Integer.class),

    /**
     * Returns the local date for the node.</p>
     * <ul>
     * <li>Used by: DatePicker </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link LocalDate} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    DATE(LocalDate.class),

    /**
     * Returns if the node is editable.</p>
     * <ul>
     * <li>Used by: TextField, ComboBox, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    EDITABLE(Boolean.class),

    /**
     * Returns if the node is expanded.</p>
     * <ul>
     * <li>Used by: TreeItem, TitledPane, etc </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    EXPANDED(Boolean.class),

    /**
     * Returns the focus item.
     * <p>
     * Used for controls with items such TabPane, TableView, ListView, etc.
     * It returns the exact Node within the control that has the focus.
     * </p>
     * <ul>
     * <li>Used by: ListView, TabPane, etc </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    FOCUS_ITEM(Node.class),

    /**
     * Returns the focus node.
     * Type: Node
     * <p>
     * This attribute is requested to the Scene, where it maps to {@link Scene#focusOwnerProperty()}.
     * </p>
     * <ul>
     * <li>Used by: Scene </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    FOCUS_NODE(Node.class),

    /**
     * Returns if the node is focused.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    FOCUSED(Boolean.class),

    /**
     * Returns the font for the node.</p>
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Font} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    FONT(Font.class),

    /**
     * Returns the header for the node.</p>
     * <ul>
     * <li>Used by: TableView and TreeTableView </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    HEADER(Node.class),

    /**
     * Returns the help for the node.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link String} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    HELP(String.class),

    /**
     * Returns the horizontal scroll bar of the node.</p>
     * <ul>
     * <li>Used by: ListView, ScrollPane, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    HORIZONTAL_SCROLLBAR(Node.class),

    /**
     * Returns the indeterminate state for the node.</p>
     * <ul>
     * <li>Used by: CheckBox and ProgressIndicator </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    INDETERMINATE(Boolean.class),

    /**
     * Returns the item at the given index.</p>
     * <ul>
     * <li>Used by: TabPane, ListView, etc </li>
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
     * Returns the item count for the node.</p>
     * <ul>
     * <li>Used by: TabPane, ListView, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    ITEM_COUNT(Integer.class),

    /**
     * Returns the index for the node.</p>
     * <ul>
     * <li>Used by: ListItem, TableRow, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    INDEX(Integer.class),

    /**
     * Returns the node that is a label for this node.
     * <p>When {@link javafx.scene.control.Label#labelFor} is set
     * it provides its content to {@code LABELED_BY}.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    LABELED_BY(Node.class),

    /**
     * Returns if the node is a leaf element.</p>
     * <ul>
     * <li>Used by: TreeItem and TreeTableRow </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    LEAF(Boolean.class),

    /**
     * Returns the line end offset of the given line index.</p>
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
     * Returns the line index of the given character offset.</p>
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
     * Returns the line start offset of the given line index.</p>
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
     * Returns the minimum value for the node.</p>
     * <ul>
     * <li>Used by: Slider, ScrollBar, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Double} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    MIN_VALUE(Double.class),

    /**
     * Returns the maximum value for the node.</p>
     * <ul>
     * <li>Used by: Slider, ScrollBar, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Double} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    MAX_VALUE(Double.class),

    /**
     * Returns the mnemonic for the node.</p>
     * <ul>
     * <li>Used by: Menu, MenuItem, CheckMenuItem, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link String} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    MNEMONIC(String.class),

    /**
     * Returns if the node allows for multiple selection.</p>
     * <ul>
     * <li>Used by: ListView, TableView, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    MULTIPLE_SELECTION(Boolean.class),

    /**
     * Returns the node at the given point location.</p>
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
     * Returns the character offset at the given point location.</p>
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
     * Returns the orientation of the node.</p>
     * <ul>
     * <li>Used by: ScrolBar and Slider </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link javafx.geometry.Orientation} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    ORIENTATION(Orientation.class),

    /**
     * Return the overflow button for the node.</p>
     * <ul>
     * <li>Used by: Toolbar </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    OVERFLOW_BUTTON(Node.class),

    /**
     * Returns the parent for the node.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Parent} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    PARENT(Parent.class),

    /**
     * Returns the parent menu for the node.</p>
     * <ul>
     * <li>Used by: ContextMenu </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    PARENT_MENU(Node.class),

    /**
     * Returns the role for the node.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link AccessibleRole} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    ROLE(AccessibleRole.class),

    /**
     * Returns the role description for the node.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link String} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    ROLE_DESCRIPTION(String.class),

    /**
     * Returns the row at the given index.</p>
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
     * Returns the row count for the node.</p>
     * <ul>
     * <li>Used by: TableView, TreeView, and TreeTableView </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    ROW_COUNT(Integer.class),

    /**
     * Returns the row index of the node.</p>
     * <ul>
     * <li>Used by: TableCell, TreeItem, and TreeTableCell </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    ROW_INDEX(Integer.class),

    /**
     * Returns the scene for the node.</p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Scene} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    SCENE(Scene.class),

    /**
     * Returns if the node is selected.</p>
     * <ul>
     * <li>Used by: CheckBox, TreeItem, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    SELECTED(Boolean.class),

    /**
     * Returns the list of selected items for the node.</p>
     * <ul>
     * <li>Used by: ListView, TableView, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link javafx.collections.ObservableList}&lt;{@link Node}&gt; </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    SELECTED_ITEMS(ObservableList.class),

    /**
     * Returns the text selection end offset for the node.</p>
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    SELECTION_END(Integer.class),

    /**
     * Returns the text selection start offset for the node.</p>
     * <ul>
     * <li>Used by: TextField and TextArea </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    SELECTION_START(Integer.class),

    /**
     * Returns the sub menu for the node.</p>
     * <ul>
     * <li>Used by: Menu </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    SUBMENU(Node.class),

    /**
     * Returns the text for the node.
     * E.g.
     * <ul>
     * <li>ComboBox returns a string representation of its
     * currently selected item.
     * <li>TextField returns the text currently entered into it.
     * </ul>
     * </p>
     * <ul>
     * <li>Used by: Node </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link String} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    TEXT(String.class),

    /**
     * Returns a tree item at the given index, relative to its TREE_ITEM_PARENT.</p>
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
     * Returns the tree item count for the node, relative to its TREE_ITEM_PARENT.</p>
     * <ul>
     * <li>Used by: TreeItem and TreeTableRow </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Integer} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    TREE_ITEM_COUNT(Integer.class),

    /**
     * Returns the parent item for the item, or null if the item is the root.</p>
     * <ul>
     * <li>Used by: TreeItem and TreeTableRow </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    TREE_ITEM_PARENT(Node.class),

    /**
     * Returns the value for the node.</p>
     * <ul>
     * <li>Used by: Slider, ScrollBar, Thumb, etc </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Double} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    VALUE(Double.class),

    /**
     * Returns the vertical scroll bar for the node.</p>
     * <ul>
     * <li>Used by: ListView, ScrollPane, etc </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Node} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    VERTICAL_SCROLLBAR(Node.class),

    /**
     * Returns if the visibility for the node.</p>
     * <ul>
     * <li>Used by: Node and ContextMenu </li>
     * <li>Needs notify: yes </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
     */
    VISIBLE(Boolean.class),

    /**
     * Returns if the node has been visited.</p>
     * <ul>
     * <li>Used by: Hyperlink </li>
     * <li>Needs notify: no </li>
     * <li>Return Type: {@link Boolean} </li>
     * <li>Parameters: <ul></ul></li>
     * </ul>
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
