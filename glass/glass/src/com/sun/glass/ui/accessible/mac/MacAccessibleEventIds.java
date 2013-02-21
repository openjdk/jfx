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
public class MacAccessibleEventIds {
    
    public enum MacEventId {
        MAIN_WINDOW_CHANGED,
        FOCUSED_WINDOW_CHANGED,
        FOCUSED_UI_ELEMENT_CHANGED,
        // Window-change notifications
        WINDOW__CREATED,
        WINDOW_DEMINIATURIZED,
        WINDOW_MINIATURIZED,
        WINDOW_MOVED,
        WINDOW_RESIZED,
        // Application notifications
        APPLICATION_ACTIVATED,
        APPLICATION_DEACTIVATED,
        APPLICATION_HIDDEN,
        APPLICATION_SHOWN,
        // Drawer and sheet notifications
        DRAWER_CREATED,
        SHEET_CREATED,
        // Element notifications
        CREATED,
        MOVED,
        RESIZED,
        TITLE_CHANGED,
        UI_ELEMENT_DESTROYED,
        VALUE_CHANGED,
        // Miscellaneous notifications
        HELP_TAG_CREATED,
        ROW_COUNT_CHANGED,
        SELECTED_CHILDREN_CHANGED,
        SELECTED_COLUMNS_CHANGED,
        SELECTED_ROWS_CHANGED,
        SELECTED_TEXT_CHANGED,
        ROW_EXPANDED,
        ROW_COLLAPSED,
        SELECTED_CELLS_CHANGED,
        UNITS_CHANGED,
        SELECTED_CHILDREN_MOVED
    }
    
}
