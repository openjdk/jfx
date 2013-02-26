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
 * Note: This enum must be kept in sync with the native NSArray
 */
public class MacAccessibleRoles {
    
    public enum MacRole {
        APPLICATION,
        BROWSER,
        BUSY_INDICATOTR,
        BUTTON,
        CELL,
        CHECK_BOX,
        COLOR_WELL,
        COLUMN,
        COMBO_BOX,
        DISCLOSURE_TRIANGLE,
        DRAWER,
        GRID,
        GROUP,
        GROW_AREA,
        HANDLE,
        HELP_TAG,
        IMAGE,
        INCREMENTOR,
        LAYOUT_AREA,
        LAYOUT_ITEM,
        LINK,
        LIST,
        LEVEL_INDICATOR,
        MATTE,
        MENU_BAR,
        MENU_BUTTON,
        MENU_ITEM,
        MENU,
        OUTLINE,
        POP_UP_BUTTON,
        PROGRESS_INDICATOR,
        RADIO_BUTTON,
        RADIO_GROUP,
        RELEVANCE_INDICATOR,
        ROW,
        RULER_MARKER,
        RULER,
        SCROLL_AREA,
        SCROLL_BAR,
        SHEET,
        SLIDER,
        //SortButton, // Deprecated
        SPLIT_GROUP,
        SPLITTER,
        STATIC_TEXT,
        SYSTEM_WIDE,
        TAB_GROUP,
        TABLE,
        TEXT_AREA,
        TEXT_FIELD,
        TOOL_BAR,
        UNKNOWN,
        VALUE_INDICATOR,
        WINDOW
    }
    
}
