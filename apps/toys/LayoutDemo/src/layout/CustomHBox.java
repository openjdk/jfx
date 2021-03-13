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

import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.HBox.getMargin;

public class CustomHBox extends HBox {

    private boolean biasDirty = true;
    private boolean performingLayout = false;
    private double minBaselineComplement = Double.NaN;
    private double prefBaselineComplement = Double.NaN;
    private Orientation bias;
    private double[][] tempArray;

    final int pad = 20;

    /**
     * Creates a CustomPane layout.
     */
    public CustomHBox() {
        super();
    }

    /**
     * Creates a Pane layout.
     * @param children The initial set of children for this pane.
     */
    public CustomHBox(Node... children) {
        super();
    }

    private Pos getAlignmentInternal() {
        Pos localPos = getAlignment();
        return localPos == null ? Pos.TOP_LEFT : localPos;
    }

    static double computeXOffset(double width, double contentWidth, HPos hpos) {
        switch(hpos) {
            case LEFT:
                return 0;
            case CENTER:
                return (width - contentWidth) / 2;
            case RIGHT:
                return width - contentWidth;
            default:
                throw new AssertionError("Unhandled hPos");
        }
    }

    private double[][] getAreaWidths(List<Node>managed, double height, boolean minimum) {
        // height could be -1
        double[][] temp = getTempArray(managed.size());
        final double insideHeight = height == -1? -1 : height -
                                     snapSpace(getInsets().getTop()) - snapSpace(getInsets().getBottom());
        final boolean shouldFillHeight = true;
        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            Insets margin = getMargin(child);
//            if (minimum) {
//                temp[0][i] = computeChildMinAreaWidth(child, getMinBaselineComplement(), margin, insideHeight, shouldFillHeight);
//            } else {
//                temp[0][i] = computeChildPrefAreaWidth(child, getPrefBaselineComplement(), margin, insideHeight, shouldFillHeight);
//            }
        }
        return temp;
    }

    private static double sum(double[] array, int size) {
        int i = 0;
        double res = 0;
        while (i != size) {
            res += array[i++];
        }
        return res;
    }

    private double adjustAreaWidths(List<Node>managed, double areaWidths[][], double width, double height) {
        Insets insets = getInsets();
        double top = snapSpace(insets.getTop());
        double bottom = snapSpace(insets.getBottom());

        double contentWidth = sum(areaWidths[0], managed.size()) + (managed.size()-1)*snapSpace(getSpacing());
        double extraWidth = width -
                snapSpace(insets.getLeft()) - snapSpace(insets.getRight()) - contentWidth;

//        if (extraWidth != 0) {
//            final double refHeight = shouldFillHeight() && height != -1? height - top - bottom : -1;
//            double remaining = growOrShrinkAreaWidths(managed, areaWidths, Priority.ALWAYS, extraWidth, refHeight);
//            remaining = growOrShrinkAreaWidths(managed, areaWidths, Priority.SOMETIMES, remaining, refHeight);
//            contentWidth += (extraWidth - remaining);
//        }
        return contentWidth;
    }

    @Override protected void layoutChildren() {
        performingLayout = true;
        List<Node> managed = getManagedChildren();
        Insets insets = getInsets();
        Pos align = getAlignmentInternal();
        HPos alignHpos = align.getHpos();
        VPos alignVpos = align.getVpos();
        double width = getWidth();
        double height = getHeight();
        double top = snapSpace(insets.getTop());
        double left = snapSpace(insets.getLeft());
        double bottom = snapSpace(insets.getBottom());
        double right = snapSpace(insets.getRight());
        double space = snapSpace(getSpacing());
        boolean shouldFillHeight = true;

        final double[][] actualAreaWidths = getAreaWidths(managed, height, false);
        double contentWidth = adjustAreaWidths(managed, actualAreaWidths, width, height);
        double contentHeight = height - top - bottom;

        double x = left + computeXOffset(width - left - right, contentWidth, align.getHpos());
        double y = top;
        double baselineOffset = -1;
        if (alignVpos == VPos.BASELINE) {
            double baselineComplement = 0;
            baselineOffset = 0;
        }

        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            Insets margin = getMargin(child);
            layoutInArea(child, x, y, actualAreaWidths[0][i], contentHeight,
                    baselineOffset, margin, true, shouldFillHeight,
                    alignHpos, alignVpos);
            x += actualAreaWidths[0][i] + space;
        }
        performingLayout = false;
    }

    private double[][] getTempArray(int size) {
        if (tempArray == null) {
            tempArray = new double[2][size]; // First array for the result, second for temporary computations
        } else if (tempArray[0].length < size) {
            tempArray = new double[2][Math.max(tempArray.length * 3, size)];
        }
        return tempArray;

    }

//    @Override protected void layoutChildren() {
//        List<Node> sortedChidlren = new ArrayList<>(getChildren());
//        Collections.sort(sortedChidlren, (c1, c2)
//                -> Double.valueOf(c2.prefHeight(-1)).compareTo(
//                        Double.valueOf(c1.prefHeight(-1))));
//        double currentX = pad;
//        for (Node c : sortedChidlren) {
//            double width = c.prefWidth(-1);
//            double height = c.prefHeight(-1);
//            layoutInArea(c, currentX, maxHeight - height, width,
//                    height, 0, HPos.CENTER, VPos.CENTER);
//            currentX = currentX + width + pad;
//        }
//    }

}
