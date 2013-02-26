/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.accessible.mac;

/**
 * 
 * Note: This enum must be kept in sync with the native NSDictionary
 */
public class MacAccessibleAttributes {
    
    public enum MacAttribute {
        CHILDREN,
        CONTENTS,
        DESCRIPTION, 
        ENABLED,
        FOCUSED,
        HELP,
        MAX_VALUE,
        MIN_VALUE,
        PARENT,
        POSITION,
        ROLE,
        ROLE_DESCRIPTION,
        SELECTED_CHILDREN,
        SHOWN_MENU,
        SIZE,
        SUBROLE,
        TITLE,
        TOP_LEVEL_UI_ELEMENT,
        VALUE,
        VALUE_DESCRIPTION,
        VISIBLE_CHILDREN,
        WINDOW,
        // Text-specific attributes
        INSERTION_POINT_LINE_NUMBER,
        NUMBER_OF_CHARACTERS,
        SELECTED_TEXT,
        SELECTED_TEXT_RANGE,
        SELECTED_TEXT_RANGES,
        SHARED_CHARACTER_RANGE,
        SHARED_TEXT_UI_ELEMENTS,
        VISIBLE_CHARACTER_RANGE,
        // Text-specific parameterized attributes
        DSTRING_FOR_RANGE_PARAMETERIZED,
        BOUNDS_FOR_RANGE_PARAMETERIZED,
        LINE_FOR_INDEX_PARAMETERIZED,
        RTF_FOR_RANGE_PARAMETERIZED,
        RANGE_FOR_INDEX_PARAMETERIZED,
        RANGE_FOR_LINE_PARAMETERIZED,
        RANGE_FOR_POSITION_PARAMETERIZED,
        STRING_FOR_RANGE_PARAMETERIZED,
        STYLE_RANGE_FOR_INDEX_PARAMETERIZED,
        // Attributes used with attributed strings
        ATTACHMENT_TEXT,
        BACKGROUND_COLOR_TEXT,
        FONT_FAMILY_KEY,
        FONT_NAME_KEY,
        FONT_SIZE_KEY,
        FONT_TEXT,
        FOREGROUND_COLOR_TEXT,
        LINK_TEXT,
        MISSPELLED_TEXT,
        SHADOW_TEXT,
        STRIKETHROUGH_COLOR_TEXT,
        STRIKETHROUGH_TEXT,
        SUPERSCRIPT_TEXT,
        UNDERLINE_COLOR_TEXT,
        UNDERLINE_TEXT,
        VISIBLE_NAME_KEY,
        // Window-specific attributes
        CANCEL_BUTTON,
        CLOSE_BUTTON,
        DEFAULT_BUTTON,
        GROW_AREA,
        MAIN,
        MINIMIZE_BUTTON,
        MINIMIZED,
        MODAL,
        PROXY,
        TOOLBAR_BUTTON,
        ZOOM_BUTTON,
        // Application-specific attributes
        CLEAR_BUTTON,
        COLUMN_TITLES,
        FOCUSED_UI_ELEMENT,
        FOCUSED_WINDOW,
        FRONTMOST,
        HIDDEN,
        MAIN_WINDOW,
        MENU_BAR,
        ORIENTATION,
        SEARCH_BUTTON,
        SEARCH_MENU,
        WINDOWS,
        // Grid view attributes
        COLUMN_COUNT,
        ORDERED_BY_ROW,
        ROW_COUNT,
        // Table view and outline view attributes
        COLUMN_HEADER_UI_ELEMENTS,
        COLUMNS,
        ROW_HEADER_UI_ELEMENTS,
        ROWS,
        SELECTED_COLUMNS,
        SELECTED_ROWS,
        SORT_DIRECTION,
        VISIBLE_COLUMNS,
        VISIBLE_ROWS,
        // Outline view attributes
        DDISCLOSED_BY_ROW,
        DISCLOSED_ROWS,
        DISCLOSING,
        DISCLOSURE_LEVEL,
        // Cell-based table attributes
        SELECTED_CELLS,
        VISIBLE_CELLS,
        // Cell-based table parameterized attributes
        CELL_FOR_COLUMN_AND_ROW_PARAMETERIZED,
        // Cell attributes
        ROW_INDEX_RANGE,
        COLUMN_INDEX_RANGE,
        // Layout area attributes
        HORIZONTAL_UNITS,
        VERTICAL_UNITS,
        HORIZONTAL_UNIT_DESCRIPTION,
        VERTICAL_UNIT_DESCRIPTION,
        // Layout area parameterized attributes
        LAYOUT_POINT_FOR_SCREEN_POINT_PARAMETERIZED,
        LAYOUT_SIZE_FOR_SCREEN_SIZE_PARAMETERIZED,
        SCREEN_POINT_FOR_LAYOUT_POINT_PARAMETERIZED,
        SCREEN_SIZE_FOR_LAYOUT_SIZE_PARAMETERIZED,
        // Slider attributes
        ALLOWED_VALUES,
        LABEL_UI_ELEMENTS,
        LABEL_VALUE,
        // Screen matte attributes
        MATTE_CONTENT_UI_ELEMENT,
        MATTE_HOLE,
        // Ruler view attributes
        MARKER_GROUP_UI_ELEMENT,
        MARKER_TYPE,
        MARKER_TYPE_DESCRIPTION,
        MARKER_UI_ELEMENTS,
        MARKER_VALUES,
        UNIT_DESCRIPTION,
        UNITS,
        // Ruler marker type values
        CENTER_TAB_STOP_MARKER_TYPE_VALUE,
        DECIMAL_TAB_STOP_MARKER_TYPE_VALUE,
        FIRST_LINE_INDENT_MARKER_TYPE_VALUE,
        HEAD_INDENT_MARKER_TYPE_VALUE,
        LEFT_TAB_STOP_MARKER_TYPE_VALUE,
        RIGHT_TAB_STOP_MARKER_TYPE_VALUE,
        TAIL_INDENT_MARKER_TYPE_VALUE,
        UNKNOWN_MARKER_TYPE_VALUE,
        // Linkage elements
        LINKED_UI_ELEMENTS,
        SERVES_AS_TITLE_FOR_UI_ELEMENTS,
        TITLE_UI_ELEMENT,
        // Miscellaneous attributes
        DECREMENT_BUTTON,
        DOCUMENT,
        EDITED,
        EXPANDED,
        FILENAME,
        HEADER,
        HORIZONTAL_SCROLL_BAR,
        INCREMENT_BUTTON,
        INDEX,
        NEXT_CONTENTS,
        OVERFLOW_BUTTON,
        PREVIOUS_CONTENTS,
        SELECTED,
        SPLITTERS,
        TABS,
        URL,
        VERTICAL_SCROLL_BAR,
        WARNING_VALUE,
        CRITICAL_VALUE,
        PLACEHOLDER_VALUE
    }
    
}
