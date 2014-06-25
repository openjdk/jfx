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

/**
 * Experimental API - Do not use (will be removed).
 *
 * @treatAsPrivate
 */
public enum Role {

    /**
     * Button.
     * Attributes: TITLE
     * Actions: FIRE
     */
    BUTTON,

    /**
     * Check Box
     * Attributes: TITLE, SELECTED, INDETERMINATE
     * Actions: FIRE
     */
    CHECK_BOX,

    /**
     * Check Menu Item.
     * Attributes: TITLE, SELECTED, ACCELERATOR, MNEMONIC, DISABLED
     * Actions: FIRE
     */
    CHECK_MENU_ITEM,

    /**
     * Combo Box.
     * Attributed:
     * Actions:
     */
    COMBO_BOX,

    /**
     * Context Menu.
     * Attributed:
     * Actions:
     */
    CONTEXT_MENU,

    /**
     * Date Picker.
     * Attributed: DATE, TITLE
     * Actions:
     */
    DATE_PICKER,

    /**
     * Decrement Button.
     *
     */
    DECREMENT_BUTTON,

    /**
     * Used by TreeView / TreeTableView to represent the arrow to the side of branches
     * Attributes: (none)
     * Actions: Fire
     */
    DISCLOSURE_NODE,

    /**
     * Header.
     * Attributes: TITLE
     * Actions: (none)
     */
    HEADER,

    /**
     * Hyperlink.
     * Attributes: TITLE
     * Actions: FIRE
     */
    HYPERLINK,

    /**
     * Increment Button.
     *
     */
    INCREMENT_BUTTON,

    /**
     * Image View.
     * Attributes: TITLE
     * Actions: (none)
     */
    IMAGE_VIEW,

    /**
     * List View.
     * Attributes: ROW_AT_INDEX, ROW_COUNT, SELECTED_ROWS, MULTIPLE_SELECTION,
     *             VERTICAL_SCROLLBAR, HORIZONTAL_SCROLLBAR
     * Actions: SCROLL_TO_INDEX
     */
    LIST_VIEW,

    /**
     * List Item.
     * Attributes: TITLE, INDEX, SELECTED
     * Actions: SELECT, ADD_TO_SELECTION, REMOVE_FROM_SELECTION
     */
    LIST_ITEM,

    /**
     * Menu.
     * Attributes: TITLE, MENU, ACCELERATOR, MNEMONIC, DISABLED
     * Actions: FIRE
     */
    MENU,

    /**
     * Menu Bar.
     * Attributed:
     * Actions:
     */
    MENU_BAR,

    /**
     * Menu Button.
     * Attributed: TITLE.
     * Actions: FIRE.
     */
    MENU_BUTTON,

    /**
     * Menu Item.
     * Attributed: TITLE, ACCELERATOR, MNEMONIC, DISABLED
     * Actions: FIRE
     */
    MENU_ITEM,

    /**
     * Node.
     * Attributes: PARENT, ROLE, SCENE, BOUNDS, DISABLED, FOCUS, VISIBLE, LABELED_BY
     * Actions: (none)
     */
    NODE,

    /**
     * Page in a Pagination control.
     * Attributes: TITLE, Selected
     * Actions: SELECT
     */
    PAGE_ITEM,

    /**
     * Pagination.
     * Attributes: PAGES, SELECTED_PAGE
     * Actions: (none)
     */
    PAGINATION,

    /**
     * Parent.
     * Attributes: CHILDREN
     * Actions: (none)
     */
    PARENT,

    /**
     * Password Field.
     * Attributes: (none)
     * Actions: (none)
     */
    PASSWORD_FIELD,

    /**
     * Progress Indicator.
     * Attributes: VALUE, MAX_VALUE, MIN_VALUE, INDETERMINATE
     * Actions: (none)
     */
    PROGRESS_INDICATOR,

    /**
     * Radio Button.
     * Attributes: TITLE, SELECTED
     * Actions: FIRE
     */
    RADIO_BUTTON,

    /**
     * Radio Menu Item.
     * Attributes: TITLE, SELECTED, ACCELERATOR, MNEMONIC, DISABLED
     * Actions: FIRE
     */
    RADIO_MENU_ITEM,

    /**
     * Slider.
     * Attributes: VALUE, MAX_VALUE, MIN_VALUE, ORIENTATION
     * Actions: INCREMENT, DECREMENT, SET_VALUE
     */
    SLIDER,

    /**
     * Text.
     * Attributes: TITLE, SELECTION_START, SELECTION_SET
     * Actions: SET_TITLE
     */
    TEXT,

    /**
     * Text Area.
     * Attributes: TITLE, SELECTION_START, SELECTION_SET
     * Actions: SET_TITLE
     */
    TEXT_AREA,

    /**
     * Text Field.
     * Attributes: TITLE, SELECTION_START, SELECTION_SET
     * Actions: SET_TITLE
     */
    TEXT_FIELD,

    /**
     * Toggle Button.
     * Attributes: TITLE, SELECTED
     * Action: FIRE
     */
    TOGGLE_BUTTON,
    
    /**
     * Tooltip.
     * Attributes: (none)
     * Action: (none)
     */
    TOOLTIP,

    /**
     * Scroll Bar.
     * Attributes: VALUE, MAX_VALUE, MIN_VALUE, ORIENTATION
     * Actions: BLOCK_INCREMENT, INCREMENT, DECREMENT, BLOCK_DECREMENT, SET_VALUE
     */
    SCROLL_BAR,

    /**
     * Scroll Pane.
     * Attributes: CONTENTS, HORIZONTAL_SCROLLBAR, VERTICAL_SCROLLBAR
     * Actions: (none)
     */
    SCROLL_PANE,

    /**
     * Split Menu Button.
     * Attributes: TITLE, EXPANDED.
     * Actions: FIRE, EXPAND, COLLAPSE.
     */
    SPLIT_MENU_BUTTON,

    /**
     * Tab Item.
     * Attributes: TITLE, SELECTED
     * Actions: SELECT
     */
    TAB_ITEM,

    /**
     * Tab Pane.
     * Attributes: TABS, SELECTED_TAB
     * Actions: (none)
     */
    TAB_PANE,

    /**
     * Table Cell.
     * Attributes: TITLE, ROW_INDEX, COLUMN_INDEX, SELECTED
     * Actions: SELECT, ADD_TO_SELECTION, REMOVE_FROM_SELECTION
     */
    TABLE_CELL,

    /**
     * Table Column.
     * Attributes: TITLE, INDEX
     * Actions: (none)
     */
    TABLE_COLUMN,

    /**
     * Table Row.
     * Attributes: TITLE, INDEX
     * Actions: (none)
     */
    TABLE_ROW,

    /**
     * Table View
     * Attributes: ROW_COUNT, COLUMN_COUNT, SELECTED_CELLS, CELL_AT_ROWCOLUMN, MULTIPLE_SELECTION,
     *             VERTICAL_SCROLLBAR, HORIZONTAL_SCROLLBAR
     * Attributes for header support: COLUMN_AT_INDEX, HEADER
     * Actions: SCROLL_TO_INDEX
     */
    TABLE_VIEW,

    /**
     * Thumb.
     * Actions: MOVE
     */
    THUMB,

    /**
     * Titled Pane.
     * Attributes: TITLE, EXPANDED
     * Actions: EXPAND, COLLAPSE
     */
    TITLED_PANE,

    /**
     * Tool Bar.
     * Attributes: OVERFLOW_BUTTON
     * Actions: (none)
     */
    TOOL_BAR,

    /**
     * Tree Item.
     * Attributes: TITLE, INDEX, SELECTED, EXPANDED, LEAF,
     *             TREE_ITEM_COUNT, TREE_ITEM_AT_INDEX, TREE_ITEM_PARENT,
     *             DISCLOSURE_LEVEL
     * Actions: SELECT, ADD_TO_SELECTION, REMOVE_FROM_SELECTION, EXPAND, COLLAPSE
     */
    TREE_ITEM,

    /**
     * Tree Table Cell.
     * Attributes: TITLE, ROW_INDEX, COLUMN_INDEX, SELECTED
     * Actions: SELECT, ADD_TO_SELECTION, REMOVE_FROM_SELECTION
     */
    TREE_TABLE_CELL,

    /**
     * Tree Table Row.
     * Attributes: TITLE, INDEX, SELECTED, EXPANDED, LEAF,
     *             TREE_ITEM_COUNT, TREE_ITEM_AT_INDEX,TREE_ITEM_PARENT,
     *             DISCLOSURE_LEVEL
     * Actions: SELECT, ADD_TO_SELECTION, REMOVE_FROM_SELECTION, EXPAND, COLLAPSE
     */
    TREE_TABLE_ROW,

    /**
     * Tree Table View.
     * Attributes: ROW_COUNT, TREE_ITEM_AT_INDEX, COLUMN_COUNT,
     *             SELECTED_CELLS, CELL_AT_ROWCOLUMN, MULTIPLE_SELECTION,
     *             VERTICAL_SCROLLBAR, HORIZONTAL_SCROLLBAR
     * Attributes for header support: COLUMN_AT_INDEX, HEADER
     * Actions: SCROLL_TO_INDEX
     */
    TREE_TABLE_VIEW,

    /**
     * Tree View.
     * Attributes: ROW_COUNT, TREE_ITEM_AT_INDEX, SELECTED_ROWS, MULTIPLE_SELECTION,
     *             VERTICAL_SCROLLBAR, HORIZONTAL_SCROLLBAR
     * Actions: SCROLL_TO_INDEX
     */
    TREE_VIEW,
}
