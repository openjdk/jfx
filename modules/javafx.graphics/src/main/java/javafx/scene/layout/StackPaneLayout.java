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

import java.util.List;
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
 * A layout which lays out its children in a back-to-front stack.
 * <p>
 * The z-order of the children is defined by the order of the children list
 * with the 0th child being the bottom and last child on top.  If a border and/or
 * padding have been set, the children will be laid out within those insets.
 * <p>
 * This layout will attempt to resize each child to fill its content area.
 * If the child could not be sized to fill the layout (either because it was
 * not resizable or its max size prevented it) then it will be aligned within
 * the area using the alignment property, which defaults to Pos.CENTER.
 *
 * <h2>Resizable Range</h2>
 *
 * This layout computes its valid range based on its content as outlined in the table below.
 *
 * <table border="1">
 * <caption>Resize Table</caption>
 * <tr><td></td><th scope="col">width</th><th scope="col">height</th></tr>
 * <tr><th scope="row">minimum</th>
 * <td>left/right insets plus the largest of the children's min widths.</td>
 * <td>top/bottom insets plus the largest of the children's min heights.</td></tr>
 * <tr><th scope="row">preferred</th>
 * <td>left/right insets plus the largest of the children's pref widths.</td>
 * <td>top/bottom insets plus the largest of the children's pref heights.</td></tr>
 * <tr><th scope="row">maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * This layout's unbounded maximum width and height are an indication that
 * it may be resized beyond its preferred size to fill whatever space is assigned
 * to it.
 */
class StackPaneLayout implements Layoutable {
    private final Callback<Layoutable, Insets> marginLookup;
    private final Callback<Layoutable, Pos> alignmentLookup;

    private RenderScaleContext renderScaleContext = RenderScaleContext.DEFAULT;
    private List<? extends Layoutable> children = List.of();
    private Pos alignment = Pos.CENTER;
    private Insets insets = Insets.EMPTY;
    private boolean snapToPixel = true;
    private Snapper snapper = Snapper.createSnapper(renderScaleContext);

    private double x;
    private double y;
    private double w;
    private double h;

    private boolean biasDirty = true;
    private Orientation bias;

    StackPaneLayout(Callback<Layoutable, Insets> marginLookup, Callback<Layoutable, Pos> alignmentLookup) {
        this.marginLookup = Objects.requireNonNull(marginLookup, "marginLookup");
        this.alignmentLookup = Objects.requireNonNull(alignmentLookup, "alignmentLookup");
    }

    void setChildren(List<? extends Layoutable> children) {
        this.children = Objects.requireNonNull(children, "children");

        invalidate();
    }

    void setAlignment(Pos alignment) {
        this.alignment = alignment == null ? Pos.CENTER : alignment;
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

    @Override
    public Orientation getContentBias() {
        if (biasDirty) {
            bias = null;

            for (Layoutable child : children) {
                Orientation contentBias = child.getContentBias();

                if (contentBias != null) {
                    bias = contentBias;

                    if (contentBias == Orientation.HORIZONTAL) {
                        break;
                    }
                }
            }

            biasDirty = false;
        }

        return bias;
    }

    @Override
    public double minWidth(double height) {
        // TODO pre-existing bug, insets not snapped anywhere
        return insets.getLeft()
            + LayoutSupport.computeMaxMinAreaWidth(snapper, children, marginLookup, height, true)
            + insets.getRight();
    }

    @Override
    public double minHeight(double width) {
        return insets.getTop()
            + LayoutSupport.computeMaxMinAreaHeight(snapper, children, marginLookup, width, true, alignment.getVpos())
            + insets.getBottom();
    }

    @Override
    public double prefWidth(double height) {
        return insets.getLeft()
            + LayoutSupport.computeMaxPrefAreaWidth(snapper, children, marginLookup, (height == -1) ? -1 : (height - insets.getTop() - insets.getBottom()), true)
            + insets.getRight();
    }

    @Override
    public double prefHeight(double width) {
        return insets.getTop()
            + LayoutSupport.computeMaxPrefAreaHeight(snapper, children, marginLookup, (width == -1) ? -1 : (width - insets.getLeft() - insets.getRight()), true, alignment.getVpos())
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
        resizeRelocate(x, y, w, h);
    }

    @Override
    public void relocate(double x, double y) {
        resizeRelocate(x, y, w, h);
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
        HPos alignHpos = alignment.getHpos();
        VPos alignVpos = alignment.getVpos();
        double top = insets.getTop();
        double right = insets.getRight();
        double left = insets.getLeft();
        double bottom = insets.getBottom();
        double contentWidth = w - left - right;
        double contentHeight = h - top - bottom;
        double baselineOffset = alignVpos == VPos.BASELINE
            ? LayoutSupport.getAreaBaselineOffset(snapper, children, marginLookup, _ -> contentWidth, contentHeight, true)
            : 0;

        for (int i = 0, size = children.size(); i < size; i++) {
            Layoutable child = children.get(i);
            Pos childAlignment = alignmentLookup.call(child);

            LayoutSupport.layoutInArea(
                snapper, child, x + left, y + top,
                contentWidth, contentHeight,
                baselineOffset, marginLookup.call(child), true, true,
                childAlignment != null ? childAlignment.getHpos() : alignHpos,
                childAlignment != null ? childAlignment.getVpos() : alignVpos
            );
        }
    }

    private void updateSnapper() {
        this.snapper = snapToPixel ? Snapper.createSnapper(renderScaleContext) : Snapper.NO_SNAPPING;
    }
}
