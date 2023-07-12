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

/**
 * This enum describes the accessible role for a {@link Node}.
 *
 * The role is used by assistive technologies such as screen readers
 * to decide the set of actions and attributes for a node.  For example,
 * when the screen reader needs the current value of a slider, it
 * will request it using the value attribute.  When the screen reader
 * changes the value of the slider, it will use an action to set
 * the current value of the slider.  The slider must respond
 * appropriately to both these requests.
 *
 * @see Node#setAccessibleRole(AccessibleRole)
 * @see Node#getAccessibleRole()
 * @see AccessibleAttribute#ROLE
 * @see Node#queryAccessibleAttribute(AccessibleAttribute, Object...)
 * @see Node#executeAccessibleAction(AccessibleAction, Object...)
 *
 * @since JavaFX 8u40
 */
public enum AccessibleRole {

    /**
     * Button role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    BUTTON,

    /**
     * Check Box role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * <li> {@link AccessibleAttribute#INDETERMINATE} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    CHECK_BOX,

    /**
     * Check Menu Item role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#ACCELERATOR} </li>
     * <li> {@link AccessibleAttribute#MNEMONIC} </li>
     * <li> {@link AccessibleAttribute#DISABLED} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    CHECK_MENU_ITEM,

    /**
     * Combo Box role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#EXPANDED} </li>
     * <li> {@link AccessibleAttribute#EDITABLE} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#EXPAND} </li>
     * <li> {@link AccessibleAction#COLLAPSE} </li>
     * </ul>
     */
    COMBO_BOX,

    /**
     * Context Menu role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#PARENT_MENU} </li>
     * <li> {@link AccessibleAttribute#VISIBLE} </li>
     * </ul>
     */
    CONTEXT_MENU,

    /**
     * Date Picker role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#DATE} </li>
     * </ul>
     */
    DATE_PICKER,

    /**
     * Decrement Button role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    DECREMENT_BUTTON,

    /**
     * Hyperlink role.
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#VISITED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    HYPERLINK,

    /**
     * Increment Button role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    INCREMENT_BUTTON,

    /**
     * Image View role.
     * <p>
     * Attributes:
     * Actions:
     * <p>
     * It is strongly recommended that a text description of the image be provided
     * for each {@link javafx.scene.image.ImageView}.  This can be done by setting either
     * {@link Node#accessibleTextProperty()} for the {@link javafx.scene.image.ImageView}
     * or by using {@link AccessibleAttribute#LABELED_BY}.
     * </p>
     */
    IMAGE_VIEW,

    /**
     * List View role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#ITEM_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#ITEM_COUNT} </li>
     * <li> {@link AccessibleAttribute#SELECTED_ITEMS} </li>
     * <li> {@link AccessibleAttribute#MULTIPLE_SELECTION} </li>
     * <li> {@link AccessibleAttribute#VERTICAL_SCROLLBAR} </li>
     * <li> {@link AccessibleAttribute#HORIZONTAL_SCROLLBAR} </li>
     * <li> {@link AccessibleAttribute#FOCUS_ITEM} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#SHOW_ITEM} </li>
     * <li> {@link AccessibleAction#SET_SELECTED_ITEMS} </li>
     * </ul>
     */
    LIST_VIEW,

    /**
     * List Item role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#INDEX} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#REQUEST_FOCUS} </li>
     * </ul>
     */
    LIST_ITEM,

    /**
     * Menu role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#ACCELERATOR} </li>
     * <li> {@link AccessibleAttribute#MNEMONIC} </li>
     * <li> {@link AccessibleAttribute#DISABLED} </li>
     * <li> {@link AccessibleAttribute#SUBMENU} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    MENU,

    /**
     * Menu Bar role.
     * <p>
     * Attributes:
     * </p>
     * <p>
     * Actions:
     * </p>
    */
    MENU_BAR,

    /**
     * Menu Button role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    MENU_BUTTON,

    /**
     * Menu Item role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#ACCELERATOR} </li>
     * <li> {@link AccessibleAttribute#MNEMONIC} </li>
     * <li> {@link AccessibleAttribute#DISABLED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    MENU_ITEM,

    /**
     * Node role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#ROLE} </li>
     * <li> {@link AccessibleAttribute#PARENT} </li>
     * <li> {@link AccessibleAttribute#SCENE} </li>
     * <li> {@link AccessibleAttribute#BOUNDS} </li>
     * <li> {@link AccessibleAttribute#DISABLED} </li>
     * <li> {@link AccessibleAttribute#FOCUSED} </li>
     * <li> {@link AccessibleAttribute#VISIBLE} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#REQUEST_FOCUS} </li>
     * </ul>
     * Optional Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#LABELED_BY} </li>
     * <li> {@link AccessibleAttribute#ROLE_DESCRIPTION} </li>
     * <li> {@link AccessibleAttribute#HELP} </li>
     * </ul>
     * Optional Actions:
     * <ul>
     * <li> {@link AccessibleAction#SHOW_MENU} </li>
     * </ul>
     */
    NODE,

    /**
     * Page role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#REQUEST_FOCUS} </li>
     * </ul>
     */
    PAGE_ITEM,

    /**
     * Pagination role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#ITEM_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#ITEM_COUNT} </li>
     * <li> {@link AccessibleAttribute#FOCUS_ITEM} </li>
     * </ul>
     * Actions:
     */
    PAGINATION,

    /**
     * Parent role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#CHILDREN} </li>
     * </ul>
     * Actions:
     */
    PARENT,

    /**
     * Password Field role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} - must return null or empty string </li>
     * </ul>
     * Actions:
     */
    PASSWORD_FIELD,

    /**
     * Progress Indicator role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#VALUE} </li>
     * <li> {@link AccessibleAttribute#MIN_VALUE} </li>
     * <li> {@link AccessibleAttribute#MAX_VALUE} </li>
     * <li> {@link AccessibleAttribute#INDETERMINATE} </li>
     * </ul>
     * Actions:
     */
    PROGRESS_INDICATOR,

    /**
     * Radio Button role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    RADIO_BUTTON,

    /**
     * Radio Menu Item role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#ACCELERATOR} </li>
     * <li> {@link AccessibleAttribute#MNEMONIC} </li>
     * <li> {@link AccessibleAttribute#DISABLED} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    RADIO_MENU_ITEM,

    /**
     * Slider role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#VALUE} </li>
     * <li> {@link AccessibleAttribute#MIN_VALUE} </li>
     * <li> {@link AccessibleAttribute#MAX_VALUE} </li>
     * <li> {@link AccessibleAttribute#ORIENTATION} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#INCREMENT} </li>
     * <li> {@link AccessibleAction#DECREMENT} </li>
     * <li> {@link AccessibleAction#SET_VALUE} </li>
     * </ul>
     */
    SLIDER,

    /**
     * Spinner role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#INCREMENT} </li>
     * <li> {@link AccessibleAction#DECREMENT} </li>
     * </ul>
     */
    SPINNER,

    /**
     * Text role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#FONT} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#SET_TEXT} </li>
     * </ul>
     */
    TEXT,

    /**
     * Text Area role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#FONT} </li>
     * <li> {@link AccessibleAttribute#EDITABLE} </li>
     * <li> {@link AccessibleAttribute#SELECTION_START} </li>
     * <li> {@link AccessibleAttribute#SELECTION_END} </li>
     * <li> {@link AccessibleAttribute#CARET_OFFSET} </li>
     * <li> {@link AccessibleAttribute#OFFSET_AT_POINT} </li>
     * <li> {@link AccessibleAttribute#LINE_START} </li>
     * <li> {@link AccessibleAttribute#LINE_END} </li>
     * <li> {@link AccessibleAttribute#LINE_FOR_OFFSET} </li>
     * <li> {@link AccessibleAttribute#BOUNDS_FOR_RANGE} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#SET_TEXT} </li>
     * <li> {@link AccessibleAction#SET_TEXT_SELECTION} </li>
     * </ul>
     */
    TEXT_AREA,

    /**
     * Text Field role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#FONT} </li>
     * <li> {@link AccessibleAttribute#EDITABLE} </li>
     * <li> {@link AccessibleAttribute#SELECTION_START} </li>
     * <li> {@link AccessibleAttribute#SELECTION_END} </li>
     * <li> {@link AccessibleAttribute#CARET_OFFSET} </li>
     * <li> {@link AccessibleAttribute#OFFSET_AT_POINT} </li>
     * <li> {@link AccessibleAttribute#BOUNDS_FOR_RANGE} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#SET_TEXT} </li>
     * <li> {@link AccessibleAction#SET_TEXT_SELECTION} </li>
     * </ul>
     */
    TEXT_FIELD,

    /**
     * Toggle Button role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * </ul>
     */
    TOGGLE_BUTTON,

    /**
     * Tooltip role.
     * <p>
     * Attributes:
     * Actions:
     */
    TOOLTIP,

    /**
     * Scroll Bar role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#VALUE} </li>
     * <li> {@link AccessibleAttribute#MAX_VALUE} </li>
     * <li> {@link AccessibleAttribute#MIN_VALUE} </li>
     * <li> {@link AccessibleAttribute#ORIENTATION} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#INCREMENT} </li>
     * <li> {@link AccessibleAction#DECREMENT} </li>
     * <li> {@link AccessibleAction#BLOCK_INCREMENT} </li>
     * <li> {@link AccessibleAction#BLOCK_DECREMENT} </li>
     * <li> {@link AccessibleAction#SET_VALUE} </li>
     * </ul>
     */
    SCROLL_BAR,

    /**
     * Scroll Pane role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#CONTENTS} </li>
     * <li> {@link AccessibleAttribute#HORIZONTAL_SCROLLBAR} </li>
     * <li> {@link AccessibleAttribute#VERTICAL_SCROLLBAR} </li>
     * </ul>
     * Actions:
     */
    SCROLL_PANE,

    /**
     * Split Menu Button role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#EXPANDED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#FIRE} </li>
     * <li> {@link AccessibleAction#EXPAND} </li>
     * <li> {@link AccessibleAction#COLLAPSE} </li>
     * </ul>
     */
    SPLIT_MENU_BUTTON,

    /**
     * Tab Item role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#REQUEST_FOCUS} </li>
     * </ul>
     */
    TAB_ITEM,

    /**
     * Tab Pane role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#ITEM_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#ITEM_COUNT} </li>
     * <li> {@link AccessibleAttribute#FOCUS_ITEM} </li>
     * </ul>
     * Actions:
     */
    TAB_PANE,

    /**
     * Table Cell role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#ROW_INDEX} </li>
     * <li> {@link AccessibleAttribute#COLUMN_INDEX} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#REQUEST_FOCUS} </li>
     * </ul>
     */
    TABLE_CELL,

    /**
     * Table Column role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#INDEX} </li>
     * </ul>
     * Actions:
     */
    TABLE_COLUMN,

    /**
     * Table Row role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#INDEX} </li>
     * </ul>
     * Actions:
     */
    TABLE_ROW,

    /**
     * Table View role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#ROW_COUNT} </li>
     * <li> {@link AccessibleAttribute#ROW_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#COLUMN_COUNT} </li>
     * <li> {@link AccessibleAttribute#COLUMN_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#SELECTED_ITEMS} </li>
     * <li> {@link AccessibleAttribute#CELL_AT_ROW_COLUMN} </li>
     * <li> {@link AccessibleAttribute#HEADER} </li>
     * <li> {@link AccessibleAttribute#MULTIPLE_SELECTION} </li>
     * <li> {@link AccessibleAttribute#VERTICAL_SCROLLBAR} </li>
     * <li> {@link AccessibleAttribute#HORIZONTAL_SCROLLBAR} </li>
     * <li> {@link AccessibleAttribute#FOCUS_ITEM} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#SHOW_ITEM} </li>
     * <li> {@link AccessibleAction#SET_SELECTED_ITEMS} </li>
     * </ul>
     */
    TABLE_VIEW,

    /**
     * Thumb role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#VALUE} </li>
     * </ul>
     * Actions:
     */
    THUMB,

    /**
     * Titled Pane role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#EXPANDED} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#EXPAND} </li>
     * <li> {@link AccessibleAction#COLLAPSE} </li>
     * </ul>
     */
    TITLED_PANE,

    /**
     * Tool Bar role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#OVERFLOW_BUTTON} </li>
     * </ul>
     * Actions:
     */
    TOOL_BAR,

    /**
     * Tree Item role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#INDEX} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * <li> {@link AccessibleAttribute#EXPANDED} </li>
     * <li> {@link AccessibleAttribute#LEAF} </li>
     * <li> {@link AccessibleAttribute#DISCLOSURE_LEVEL} </li>
     * <li> {@link AccessibleAttribute#TREE_ITEM_COUNT} </li>
     * <li> {@link AccessibleAttribute#TREE_ITEM_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#TREE_ITEM_PARENT} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#EXPAND} </li>
     * <li> {@link AccessibleAction#COLLAPSE} </li>
     * <li> {@link AccessibleAction#REQUEST_FOCUS} </li>
     * </ul>
     */
    TREE_ITEM,

     /**
     * Check Box Tree Item role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#INDEX} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * <li> {@link AccessibleAttribute#EXPANDED} </li>
     * <li> {@link AccessibleAttribute#LEAF} </li>
     * <li> {@link AccessibleAttribute#DISCLOSURE_LEVEL} </li>
     * <li> {@link AccessibleAttribute#TREE_ITEM_COUNT} </li>
     * <li> {@link AccessibleAttribute#TREE_ITEM_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#TREE_ITEM_PARENT} </li>
     * <li> {@link AccessibleAttribute#TOGGLE_STATE} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#EXPAND} </li>
     * <li> {@link AccessibleAction#COLLAPSE} </li>
     * <li> {@link AccessibleAction#REQUEST_FOCUS} </li>
     * </ul>
     *
     * @since 21
     */
    CHECK_BOX_TREE_ITEM,

    /**
     * Tree Table Cell role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#SELECTED} </li>
     * <li> {@link AccessibleAttribute#ROW_INDEX} </li>
     * <li> {@link AccessibleAttribute#COLUMN_INDEX} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#REQUEST_FOCUS} </li>
     * </ul>
     */
    TREE_TABLE_CELL,

    /**
     * Tree Table Row role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#INDEX} </li>
     * <li> {@link AccessibleAttribute#EXPANDED} </li>
     * <li> {@link AccessibleAttribute#LEAF} </li>
     * <li> {@link AccessibleAttribute#DISCLOSURE_LEVEL} </li>
     * <li> {@link AccessibleAttribute#TREE_ITEM_COUNT} </li>
     * <li> {@link AccessibleAttribute#TREE_ITEM_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#TREE_ITEM_PARENT} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#EXPAND} </li>
     * <li> {@link AccessibleAction#COLLAPSE} </li>
     * </ul>
     */
    TREE_TABLE_ROW,

    /**
     * Tree Table View role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#ROW_COUNT} </li>
     * <li> {@link AccessibleAttribute#ROW_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#COLUMN_COUNT} </li>
     * <li> {@link AccessibleAttribute#COLUMN_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#SELECTED_ITEMS} </li>
     * <li> {@link AccessibleAttribute#CELL_AT_ROW_COLUMN} </li>
     * <li> {@link AccessibleAttribute#HEADER} </li>
     * <li> {@link AccessibleAttribute#MULTIPLE_SELECTION} </li>
     * <li> {@link AccessibleAttribute#VERTICAL_SCROLLBAR} </li>
     * <li> {@link AccessibleAttribute#HORIZONTAL_SCROLLBAR} </li>
     * <li> {@link AccessibleAttribute#FOCUS_ITEM} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#SHOW_ITEM} </li>
     * <li> {@link AccessibleAction#SET_SELECTED_ITEMS} </li>
     * </ul>
     */
    TREE_TABLE_VIEW,

    /**
     * Tree View role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#ROW_COUNT} </li>
     * <li> {@link AccessibleAttribute#ROW_AT_INDEX} </li>
     * <li> {@link AccessibleAttribute#SELECTED_ITEMS} </li>
     * <li> {@link AccessibleAttribute#MULTIPLE_SELECTION} </li>
     * <li> {@link AccessibleAttribute#VERTICAL_SCROLLBAR} </li>
     * <li> {@link AccessibleAttribute#HORIZONTAL_SCROLLBAR} </li>
     * <li> {@link AccessibleAttribute#FOCUS_ITEM} </li>
     * </ul>
     * Actions:
     * <ul>
     * <li> {@link AccessibleAction#SHOW_ITEM} </li>
     * <li> {@link AccessibleAction#SET_SELECTED_ITEMS} </li>
     * </ul>
     */
    TREE_VIEW,

    /**
     * Dialog role.
     * <p>
     * Attributes:
     * <ul>
     * <li> {@link AccessibleAttribute#TEXT} </li>
     * <li> {@link AccessibleAttribute#ROLE_DESCRIPTION} </li>
     * <li> {@link AccessibleAttribute#CHILDREN} </li>
     * </ul>
     *
     * @since 20
     */
    DIALOG
}
