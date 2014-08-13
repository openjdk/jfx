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
 * Experimental API - Do not use (will be removed).
 *
 * @treatAsPrivate
 */
public enum AccessibleRole {

    /**
     * Button.
     * Attributes: TEXT
     * Actions: FIRE
     */
    BUTTON,

    /**
     * Check Box
     * Attributes: TEXT, SELECTED, INDETERMINATE
     * Actions: FIRE
     */
    CHECK_BOX,

    /**
     * Check Menu Item.
     * Attributes: TEXT, ACCELERATOR, MNEMONIC, DISABLED, SELECTED
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
     * Attributed: PARENT_MENU, VISIBLE
     * Actions: (none)
     */
    CONTEXT_MENU,

    /**
     * Date Picker.
     * Attributed: DATE, TEXT
     * Actions:
     */
    DATE_PICKER,

    /**
     * Decrement Button.
     *
     */
    DECREMENT_BUTTON,

    /**
     * Header.
     * Attributes: TEXT
     * Actions: (none)
     */
    HEADER,

    /**
     * Hyperlink.
     * Attributes: TEXT
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
     * Attributes: TEXT
     * Actions: (none)
     */
    IMAGE_VIEW,

    /**
     * List View.
     * Attributes: ITEM_AT_INDEX, ITEM_COUNT, SELECTED_ITEMS, MULTIPLE_SELECTION,
     *             VERTICAL_SCROLLBAR, HORIZONTAL_SCROLLBAR
     * Actions: SCROLL_TO_INDEX, SET_SELECTED_ITEMS
     */
    LIST_VIEW,

    /**
     * List Item.
     * Attributes: TEXT, INDEX, SELECTED
     * Actions: (none)
     */
    LIST_ITEM,

    /**
     * Menu.
     * Attributes: TEXT, ACCELERATOR, MNEMONIC, DISABLED, SUBMENU
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
     * Attributed: TEXT.
     * Actions: FIRE.
     */
    MENU_BUTTON,

    /**
     * Menu Item.
     * Attributed: TEXT, ACCELERATOR, MNEMONIC, DISABLED
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
     * Attributes: TEXT, SELECTED
     * Actions: (none)
     */
    PAGE_ITEM,

    /**
     * Pagination.
     * Attributes: ITEM_AT_INDEX, ITEM_COUNT
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
     * Attributes: TEXT, SELECTED
     * Actions: FIRE
     */
    RADIO_BUTTON,

    /**
     * Radio Menu Item.
     * Attributes: TEXT, ACCELERATOR, MNEMONIC, DISABLED, SELECTED
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
     * Spinner.
     * Attributes: TEXT
     * Actions: INCREMENT, DECREMENT
     */
    SPINNER,

    /**
     * Text.
     * Attributes: TEXT, SELECTION_START, SELECTION_SET
     * Actions: SET_TEXT
     */
    TEXT,

    /**
     * Text Area.
     * Attributes: TEXT, SELECTION_START, SELECTION_SET
     * Actions: SET_TEXT, SET_TEXT_SELECTION
     */
    TEXT_AREA,

    /**
     * Text Field.
     * Attributes: TEXT, SELECTION_START, SELECTION_SET
     * Actions: SET_TEXT, SET_TEXT_SELECTION
     */
    TEXT_FIELD,

    /**
     * Toggle Button.
     * Attributes: TEXT, SELECTED
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
     * Attributes: TEXT, EXPANDED.
     * Actions: FIRE, EXPAND, COLLAPSE.
     */
    SPLIT_MENU_BUTTON,

    /**
     * Tab Item.
     * Attributes: TEXT, SELECTED
     * Actions: (none)
     */
    TAB_ITEM,

    /**
     * Tab Pane.
     * Attributes: ITEM_AT_INDEX, ITEM_COUNT
     * Actions: (none)
     */
    TAB_PANE,

    /**
     * Table Cell.
     * Attributes: TEXT, ROW_INDEX, COLUMN_INDEX, SELECTED
     * Actions: (none)
     */
    TABLE_CELL,

    /**
     * Table Column.
     * Attributes: TEXT, INDEX
     * Actions: (none)
     */
    TABLE_COLUMN,

    /**
     * Table Row.
     * Attributes: TEXT, INDEX
     * Actions: (none)
     */
    TABLE_ROW,

    /**
     * Table View
     * Attributes: ROW_COUNT, ROW_AT_INDEX, COLUMN_COUNT, COLUMN_AT_INDEX
     *             SELECTED_ITEMS, CELL_AT_ROWCOLUMN, HEADER,
     *             MULTIPLE_SELECTION, VERTICAL_SCROLLBAR, HORIZONTAL_SCROLLBAR
     * Actions: SCROLL_TO_INDEX, SET_SELECTED_ITEMS
     */
    TABLE_VIEW,

    /**
     * Thumb.
     * Attributes: VALUE
     * Actions: (none)
     */
    THUMB,

    /**
     * Titled Pane.
     * Attributes: TEXT, EXPANDED
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
     * Attributes: TEXT, SELECTED, INDEX, EXPANDED, LEAF, DISCLOSURE_LEVEL,
     *             TREE_ITEM_COUNT, TREE_ITEM_AT_INDEX, TREE_ITEM_PARENT
     * Actions: EXPAND, COLLAPSE
     */
    TREE_ITEM,

    /**
     * Tree Table Cell.
     * Attributes: TEXT, ROW_INDEX, COLUMN_INDEX, SELECTED
     * Actions: (none)
     */
    TREE_TABLE_CELL,

    /**
     * Tree Table Row.
     * Attributes: INDEX, EXPANDED, LEAF, DISCLOSURE_LEVEL
     *             TREE_ITEM_COUNT, TREE_ITEM_AT_INDEX, TREE_ITEM_PARENT
     * Actions: EXPAND, COLLAPSE
     */
    TREE_TABLE_ROW,

    /**
     * Tree Table View.
     * Attributes: ROW_COUNT, ROW_AT_INDEX, COLUMN_COUNT, COLUMN_AT_INDEX,
     *             SELECTED_ITEMS, CELL_AT_ROWCOLUMN, HEADER,
     *             MULTIPLE_SELECTION, VERTICAL_SCROLLBAR, HORIZONTAL_SCROLLBAR
     * Actions: SCROLL_TO_INDEX, SET_SELECTED_ITEMS
     */
    TREE_TABLE_VIEW,

    /**
     * Tree View.
     * Attributes: ROW_COUNT, ROW_AT_INDEX, SELECTED_ITEMS,
     *             MULTIPLE_SELECTION, VERTICAL_SCROLLBAR, HORIZONTAL_SCROLLBAR
     * Actions: SCROLL_TO_INDEX, SET_SELECTED_ITEMS
     */
    TREE_VIEW,
}
