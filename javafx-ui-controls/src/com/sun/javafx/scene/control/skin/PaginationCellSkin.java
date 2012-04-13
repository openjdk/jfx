/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.javafx.scene.control.skin;

import javafx.scene.control.ListView;
import com.sun.javafx.scene.control.PaginationCell;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;

public class PaginationCellSkin extends ListCellSkin {

    public PaginationCellSkin(PaginationCell control) {
        super(control);

        getSkinnable().getListView().layoutBoundsProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable o) {
                requestLayout();
            }
        });
    }

    @Override protected double computePrefWidth(double height) {
        ListView listView = getSkinnable().getListView();
        double nodeWidth = listView == null ? 0 : listView.getLayoutBounds().getWidth();
        for (Node n: getChildren()) {
            nodeWidth = Math.max(nodeWidth, n.prefWidth(height));
        }
        return nodeWidth;
    }

    @Override protected double computePrefHeight(double width) {
        ListView listView = getSkinnable().getListView();
        double nodeHeight = listView == null ? 0 : listView.getLayoutBounds().getHeight();
        for (Node n: getChildren()) {
            nodeHeight = Math.max(nodeHeight, n.prefHeight(width));
        }
        return nodeHeight;
    }
}
