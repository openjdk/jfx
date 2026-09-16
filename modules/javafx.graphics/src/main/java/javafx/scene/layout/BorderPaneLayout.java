/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import java.util.Objects;

import com.sun.javafx.scene.layout.Snapper;

import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.util.Callback;

/**
 * A layout which lays out its children in top, left, right, bottom, and center positions.
 * <p>
 * The top and bottom children will be resized to their preferred heights and
 * extend the width of the layout.  The left and right children will be resized
 * to their preferred widths and extend the length between the top and bottom elements.
 * And the center child will be resized to fill the available space in the middle.
 * Any of the positions may be null.
 * <p>
 * This layout honors the minimum, preferred, and maximum sizes of its children.
 * If the child's resizable range prevents it from being resized to fit within its
 * position, it will be aligned relative to the space using a default alignment
 * as follows:
 * <ul>
 * <li>top: Pos.TOP_LEFT</li>
 * <li>bottom: Pos.BOTTOM_LEFT</li>
 * <li>left: Pos.TOP_LEFT</li>
 * <li>right: Pos.TOP_RIGHT</li>
 * <li>center: Pos.CENTER</li>
 * </ul>
 * These default alignments may be overridden on individual children by setting
 * the child's alignment constraint.
 *
 * <h2>Resizable Range</h2>
 * <p>
 * This layout computes its valid range based on its content as outlined in the table below.
 * </p>
 *
 * <table border="1">
 * <caption>BorderPane Resize Table</caption>
 * <tr><td></td><th scope="col">width</th><th scope="col">height</th></tr>
 * <tr><th scope="row">minimum</th>
 * <td>left/right insets plus width required to display right/left children at their pref widths and top/bottom/center with at least their min widths</td>
 * <td>top/bottom insets plus height required to display top/bottom children at their pref heights and left/right/center with at least their min heights</td></tr>
 * <tr><th scope="row">preferred</th>
 * <td>left/right insets plus width required to display top/right/bottom/left/center children with at least their pref widths</td>
 * <td>top/bottom insets plus height required to display top/right/bottom/left/center children with at least their pref heights</td></tr>
 * <tr><th scope="row">maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * This layout's unbounded maximum width and height are an indication that
 * it may be resized beyond its preferred size to fill whatever space is assigned
 * to it.
 */
class BorderPaneLayout implements Layoutable {
    private final Callback<Layoutable, Insets> marginLookup;
    private final Callback<Layoutable, Pos> alignmentLookup;

    private RenderScaleContext renderScaleContext = RenderScaleContext.DEFAULT;
    private Layoutable center;
    private Layoutable top;
    private Layoutable right;
    private Layoutable bottom;
    private Layoutable left;
    private Insets insets = Insets.EMPTY;
    private boolean snapToPixel = true;
    private Snapper snapper = Snapper.createSnapper(renderScaleContext);

    private double x;
    private double y;
    private double w;
    private double h;

    private boolean biasDirty = true;
    private Orientation bias;

    BorderPaneLayout(Callback<Layoutable, Insets> marginLookup, Callback<Layoutable, Pos> alignmentLookup) {
        this.marginLookup = Objects.requireNonNull(marginLookup, "marginLookup");
        this.alignmentLookup = Objects.requireNonNull(alignmentLookup, "alignmentLookup");
    }

    void setPositions(Layoutable center, Layoutable top, Layoutable right, Layoutable bottom, Layoutable left) {
        this.center = center;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;

        invalidate();
    }

    void setInsets(Insets insets) {
        this.insets = insets == null ? Insets.EMPTY : insets;
    }

    void setSnapToPixel(boolean snapToPixel) {
        if (this.snapToPixel != snapToPixel) {
            this.snapToPixel = snapToPixel;

            updateSnapper();
        }
    }

    void setRenderScaleContext(RenderScaleContext renderScaleContext) {
        if (!this.renderScaleContext.equals(renderScaleContext)) {
            this.renderScaleContext = Objects.requireNonNull(renderScaleContext, "renderScaleContext");

            updateSnapper();
        }
    }

    void invalidate() {
        this.biasDirty = true;
        this.bias = null;
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    /**
     * @return null unless the center, right, bottom, left or top has a content bias.
     */
    @Override
    public Orientation getContentBias() {
        if (biasDirty) {
            bias = computeContentBias();

            biasDirty = false;
        }

        return bias;
    }

    private Orientation computeContentBias() {
        if (center != null && center.getContentBias() != null) {
            return center.getContentBias();
        }

        if (right != null && right.getContentBias() == Orientation.VERTICAL) {
            return right.getContentBias();
        }

        if (left != null && left.getContentBias() == Orientation.VERTICAL) {
            return left.getContentBias();
        }

        if (bottom != null && bottom.getContentBias() == Orientation.HORIZONTAL) {
            return bottom.getContentBias();
        }

        if (top != null && top.getContentBias() == Orientation.HORIZONTAL) {
            return top.getContentBias();
        }

        return null;
    }

    @Override
    public double minWidth(double height) {
        double topMinWidth = getAreaWidth(top, -1, true);
        double bottomMinWidth = getAreaWidth(bottom, -1, true);

        double leftPrefWidth;
        double rightPrefWidth;
        double centerMinWidth;

        if (height != -1 && (childHasContentBias(left, Orientation.VERTICAL) ||
                             childHasContentBias(right, Orientation.VERTICAL) ||
                             childHasContentBias(center, Orientation.VERTICAL))) {
            double topPrefHeight = getAreaHeight(top, -1, false);
            double bottomPrefHeight = getAreaHeight(bottom, -1, false);
            double middleAreaHeight = Math.max(0, height - topPrefHeight - bottomPrefHeight);

            leftPrefWidth = getAreaWidth(left, middleAreaHeight, false);
            rightPrefWidth = getAreaWidth(right, middleAreaHeight, false);
            centerMinWidth = getAreaWidth(center, middleAreaHeight, true);
        }
        else {
            leftPrefWidth = getAreaWidth(left, -1, false);
            rightPrefWidth = getAreaWidth(right, -1, false);
            centerMinWidth = getAreaWidth(center, -1, true);
        }

        // TODO pre-existing bug: insets not snapped
        return insets.getLeft()
            + Math.max(leftPrefWidth + centerMinWidth + rightPrefWidth, Math.max(topMinWidth, bottomMinWidth))
            + insets.getRight();
    }

    @Override
    public double minHeight(double width) {
        // Bottom and top are always at their pref height
        double topPrefHeight = getAreaHeight(top, width, false);
        double bottomPrefHeight = getAreaHeight(bottom, width, false);

        double leftMinHeight = getAreaHeight(left, -1, true);
        double rightMinHeight = getAreaHeight(right, -1, true);

        double centerMinHeight;

        if (width != -1 && childHasContentBias(center, Orientation.HORIZONTAL)) {
            double leftPrefWidth = getAreaWidth(left, -1, false);
            double rightPrefWidth = getAreaWidth(right, -1, false);

            centerMinHeight = getAreaHeight(center, Math.max(0, width - leftPrefWidth - rightPrefWidth) , true);
        }
        else {
            centerMinHeight = getAreaHeight(center, -1, true);
        }

        double middleAreaMinHeigh = Math.max(centerMinHeight, Math.max(rightMinHeight, leftMinHeight));

        return insets.getTop()
            + topPrefHeight + middleAreaMinHeigh + bottomPrefHeight
            + insets.getBottom();
    }

    @Override
    public double prefWidth(double height) {
        double topPrefWidth = getAreaWidth(top, -1, false);
        double bottomPrefWidth = getAreaWidth(bottom, -1, false);

        double leftPrefWidth;
        double rightPrefWidth;
        double centerPrefWidth;

        if (height != -1 && (childHasContentBias(left, Orientation.VERTICAL) ||
                             childHasContentBias(right, Orientation.VERTICAL) ||
                             childHasContentBias(center, Orientation.VERTICAL))) {
            double topPrefHeight = getAreaHeight(top, -1, false);
            double bottomPrefHeight = getAreaHeight(bottom, -1, false);
            double middleAreaHeight = Math.max(0, height - topPrefHeight - bottomPrefHeight);

            leftPrefWidth = getAreaWidth(left, middleAreaHeight, false);
            rightPrefWidth = getAreaWidth(right, middleAreaHeight, false);
            centerPrefWidth = getAreaWidth(center, middleAreaHeight, false);
        }
        else {
            leftPrefWidth = getAreaWidth(left, -1, false);
            rightPrefWidth = getAreaWidth(right, -1, false);
            centerPrefWidth = getAreaWidth(center, -1, false);
        }

        return insets.getLeft()
            + Math.max(leftPrefWidth + centerPrefWidth + rightPrefWidth, Math.max(topPrefWidth,bottomPrefWidth))
            + insets.getRight();
    }

    @Override
    public double prefHeight(double width) {
        double topPrefHeight = getAreaHeight(top, width, false);
        double bottomPrefHeight = getAreaHeight(bottom, width, false);
        double leftPrefHeight = getAreaHeight(left, -1, false);
        double rightPrefHeight = getAreaHeight(right, -1, false);
        double centerPrefHeight;

        if (width != -1 && childHasContentBias(center, Orientation.HORIZONTAL)) {
            double leftPrefWidth = getAreaWidth(left, -1, false);
            double rightPrefWidth = getAreaWidth(right, -1, false);

            centerPrefHeight = getAreaHeight(center, Math.max(0, width - leftPrefWidth - rightPrefWidth), false);
        }
        else {
            centerPrefHeight = getAreaHeight(center, -1, false);
        }

        double middleAreaPrefHeigh = Math.max(centerPrefHeight, Math.max(rightPrefHeight, leftPrefHeight));

        return insets.getTop()
            + topPrefHeight + middleAreaPrefHeigh + bottomPrefHeight
            + insets.getBottom();
    }

    @Override
    public double maxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    public double maxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public double getBaselineOffset() {
        return Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT;
    }

    @Override
    public Bounds getLayoutBounds() {
        return new BoundingBox(x, y, w, h);
    }

    @Override
    public void resize(double width, double height) {
        resizeRelocate(this.x, this.y, width, height);
    }

    @Override
    public void relocate(double xPos, double yPos) {
        resizeRelocate(xPos, yPos, this.w, this.h);
    }

    @Override
    public void resizeRelocate(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.w = width;
        this.h = height;

        layoutChildren();
    }

    private void layoutChildren() {
        double width = w;
        double height = h;

        double insideX = insets.getLeft();
        double insideY = insets.getTop();
        double insideWidth = width - insideX - insets.getRight();
        double insideHeight = height - insideY - insets.getBottom();
        Layoutable c = center;
        Layoutable r = right;
        Layoutable b = bottom;
        Layoutable l = left;
        Layoutable t = top;
        double topHeight = 0;

        if (t != null) {
            Insets topMargin = getNodeMargin(t);
            double adjustedWidth = adjustWidthByMargin(insideWidth, topMargin);
            double adjustedHeight = adjustHeightByMargin(insideHeight, topMargin);

            topHeight = LayoutUtils.boundedSizeWithBias(snapper, t, adjustedWidth, adjustedHeight, true, false).height();
            topHeight = snapper.snapSpaceY(topMargin.getBottom()) + topHeight + snapper.snapSpaceY(topMargin.getTop());

            Pos alignment = alignmentLookup.call(t);
            LayoutUtils.layoutInArea(
                snapper, t, insideX, insideY, insideWidth, topHeight, 0 /*ignore baseline*/,
                topMargin,
                alignment != null ? alignment.getHpos() : HPos.LEFT,
                alignment != null ? alignment.getVpos() : VPos.TOP
            );
        }

        double bottomHeight = 0;

        if (b != null) {
            Insets bottomMargin = getNodeMargin(b);
            double adjustedWidth = adjustWidthByMargin(insideWidth, bottomMargin);
            double adjustedHeight = adjustHeightByMargin(insideHeight - topHeight, bottomMargin);

            bottomHeight = LayoutUtils.boundedSizeWithBias(snapper, b, adjustedWidth, adjustedHeight, true, false).height();
            bottomHeight = snapper.snapSpaceY(bottomMargin.getBottom()) + bottomHeight + snapper.snapSpaceY(bottomMargin.getTop());

            Pos alignment = alignmentLookup.call(b);
            LayoutUtils.layoutInArea(
                snapper, b, insideX, insideY + insideHeight - bottomHeight,
                insideWidth, bottomHeight, 0 /*ignore baseline*/,
                bottomMargin,
                alignment != null ? alignment.getHpos() : HPos.LEFT,
                alignment != null ? alignment.getVpos() : VPos.BOTTOM
            );
        }

        double leftWidth = 0;

        if (l != null) {
            Insets leftMargin = getNodeMargin(l);
            double adjustedWidth = adjustWidthByMargin(insideWidth, leftMargin);
            double adjustedHeight = adjustHeightByMargin(insideHeight - topHeight - bottomHeight, leftMargin); // ????

            leftWidth = LayoutUtils.boundedSizeWithBias(snapper, l, adjustedWidth, adjustedHeight, false, true).width();
            leftWidth = snapper.snapSpaceX(leftMargin.getLeft()) + leftWidth + snapper.snapSpaceX(leftMargin.getRight());

            Pos alignment = alignmentLookup.call(l);
            LayoutUtils.layoutInArea(
                snapper, l, insideX, insideY + topHeight,
                leftWidth, insideHeight - topHeight - bottomHeight, 0 /*ignore baseline*/,
                leftMargin,
                alignment != null ? alignment.getHpos() : HPos.LEFT,
                alignment != null ? alignment.getVpos() : VPos.TOP
            );
        }

        double rightWidth = 0;

        if (r != null) {
            Insets rightMargin = getNodeMargin(r);
            double adjustedWidth = adjustWidthByMargin(insideWidth - leftWidth, rightMargin);
            double adjustedHeight = adjustHeightByMargin(insideHeight - topHeight - bottomHeight, rightMargin);

            rightWidth = LayoutUtils.boundedSizeWithBias(snapper, r, adjustedWidth, adjustedHeight, false, true).width();
            rightWidth = snapper.snapSpaceX(rightMargin.getLeft()) + rightWidth + snapper.snapSpaceX(rightMargin.getRight());

            Pos alignment = alignmentLookup.call(r);
            LayoutUtils.layoutInArea(
                snapper, r, insideX + insideWidth - rightWidth, insideY + topHeight,
                rightWidth, insideHeight - topHeight - bottomHeight, 0 /*ignore baseline*/,
                rightMargin,
                alignment != null ? alignment.getHpos() : HPos.RIGHT,
                alignment != null ? alignment.getVpos() : VPos.TOP
            );
        }

        if (c != null) {
            Pos alignment = alignmentLookup.call(c);

            LayoutUtils.layoutInArea(
                snapper, c, insideX + leftWidth, insideY + topHeight,
                insideWidth - leftWidth - rightWidth,
                insideHeight - topHeight - bottomHeight, 0 /*ignore baseline*/,
                getNodeMargin(c),
                alignment != null ? alignment.getHpos() : HPos.CENTER,
                alignment != null ? alignment.getVpos() : VPos.CENTER
            );
        }
    }

    private double getAreaWidth(Layoutable child, double height, boolean minimum) {
        if (child != null) {
            Insets margin = getNodeMargin(child);

            return minimum
                ? LayoutUtils.computeChildMinAreaWidth(snapper, child, -1, margin, height, false)
                : LayoutUtils.computeChildPrefAreaWidth(snapper, child, -1, margin, height, false);
        }

        return 0;
    }

    private double getAreaHeight(Layoutable child, double width, boolean minimum) {
        if (child != null) {
            Insets margin = getNodeMargin(child);

            return minimum
                ? LayoutUtils.computeChildMinAreaHeight(snapper, child, -1, margin, width, true)
                : LayoutUtils.computeChildPrefAreaHeight(snapper, child, -1, margin, width, true);
        }

        return 0;
    }

    private static boolean childHasContentBias(Layoutable child, Orientation orientation) {
        if (child != null) {
            return child.getContentBias() == orientation;
        }

        return false;
    }

    private Insets getNodeMargin(Layoutable child) {
        Insets margin = marginLookup.call(child);

        return margin != null ? margin : Insets.EMPTY;
    }

    private double adjustWidthByMargin(double width, Insets margin) {
        if (margin == null || margin == Insets.EMPTY) {
            return width;
        }

        return width - snapper.snapSpaceX(margin.getLeft()) - snapper.snapSpaceX(margin.getRight());
    }

    private double adjustHeightByMargin(double height, Insets margin) {
        if (margin == null || margin == Insets.EMPTY) {
            return height;
        }

        return height - snapper.snapSpaceY(margin.getTop()) - snapper.snapSpaceY(margin.getBottom());
    }

    private void updateSnapper() {
        this.snapper = snapToPixel ? Snapper.createSnapper(renderScaleContext) : Snapper.NO_SNAPPING;
    }
}
