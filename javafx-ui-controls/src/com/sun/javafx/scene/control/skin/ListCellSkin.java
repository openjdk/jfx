/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import javafx.geometry.Orientation;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import com.sun.javafx.scene.control.behavior.ListCellBehavior;

public class ListCellSkin extends CellSkinBase<ListCell, ListCellBehavior> {

    public ListCellSkin(ListCell control) {
        super(control, new ListCellBehavior(control));
    }

    @Override protected double computePrefWidth(double height) {
        double pref = super.computePrefWidth(height);
        ListView listView = getSkinnable().getListView();
        return listView == null ? 0 :
            listView.getOrientation() == Orientation.VERTICAL ? pref : Math.max(pref, getCellSize());
    }
 
    @Override protected double computePrefHeight(double width) {
//        if (cellSizeSet) {
            // Added the comparison between the default cell size and the requested
            // cell size to prevent the issue identified in RT-19873.
            double cellSize = getCellSize();
            return cellSize == DEFAULT_CELL_SIZE ? super.computePrefHeight(width) : cellSize;
//        } else {
//            return Math.max(DEFAULT_CELL_SIZE, super.computePrefHeight(width));
//        }
    }
}
