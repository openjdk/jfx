/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class CustomPane extends Pane {

    final int pad = 20;

    /**
     * Creates a CustomPane layout.
     */
    public CustomPane() {
        super();
    }

    /**
     * Creates a Pane layout.
     * @param children The initial set of children for this pane.
     */
    public CustomPane(Node... children) {
        super();
    }

    @Override protected double computeMinWidth(double height) {
        return super.computePrefWidth(height);
    }

    @Override protected double computeMinHeight(double width) {
        return super.computeMinHeight(width);
    }

    @Override protected double computeMaxWidth(double height) {
        return super.computeMaxWidth(height);
    }

    @Override protected double computeMaxHeight(double width) {
        return super.computePrefHeight(width);
    }

    @Override protected double computePrefWidth(double height) {
        double width = 0;
        for (Node c : getChildren()) {
            width = width + c.prefWidth(-1);
        }
        return width;
    }

    double maxHeight = 0;
    @Override protected double computePrefHeight(double width) {
        for (Node c : getChildren()) {
            maxHeight = Math.max(c.prefHeight(-1), maxHeight);
        }
        maxHeight += pad;
        return maxHeight;
    }

    @Override protected void layoutChildren() {
        List<Node> sortedManagedChidlren = new ArrayList<>(getManagedChildren());
        Collections.sort(sortedManagedChidlren, (c1, c2)
                -> Double.valueOf(c2.prefHeight(-1)).compareTo(
                        Double.valueOf(c1.prefHeight(-1))));
        double currentX = pad;
        for (Node c : sortedManagedChidlren) {
            double width = c.prefWidth(-1);
            double height = c.prefHeight(-1);
            layoutInArea(c, currentX, maxHeight - height, width,
                    height, 0, HPos.CENTER, VPos.CENTER);
            currentX = currentX + width + pad;
        }
    }

}
