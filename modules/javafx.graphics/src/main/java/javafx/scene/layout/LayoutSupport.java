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
import java.util.function.Function;

import com.sun.javafx.scene.layout.Snapper;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.util.Callback;

/**
 * Layout math shared between Region and Layoutable-based layouts.
 */
final class LayoutSupport {

    static double computeChildMinAreaWidth(Snapper snapper, Layoutable child, Insets margin) {
        return computeChildMinAreaWidth(snapper, child, -1, margin, -1, false);
    }

    static double computeChildMinAreaWidth(Snapper snapper, Layoutable child, double baselineComplement, Insets margin, double availableHeight, boolean fillHeight) {
        double left = margin != null ? snapper.snapSpaceX(margin.getLeft()) : 0;
        double right = margin != null ? snapper.snapSpaceX(margin.getRight()) : 0;
        double alt = -1;
        if (availableHeight != -1 && child.isResizable() && child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            double top = margin != null ? snapper.snapSpaceY(margin.getTop()) : 0;
            double bottom = margin != null ? snapper.snapSpaceY(margin.getBottom()) : 0;
            double bo = child.getBaselineOffset();
            final double contentHeight = bo == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT && baselineComplement != -1 ?
                    availableHeight - top - bottom - baselineComplement :
                    availableHeight - top - bottom;
            alt = computedBoundedHeight(snapper, child, fillHeight, contentHeight);
        }
        return left + snapper.snapSizeX(child.minWidth(alt)) + right;
    }

    static double computeChildMinAreaHeight(Snapper snapper, Layoutable child, Insets margin) {
        return computeChildMinAreaHeight(snapper, child, -1, margin, -1, false);
    }

    static double computeChildMinAreaHeight(Snapper snapper, Layoutable child, double minBaselineComplement, Insets margin, double availableWidth, boolean fillWidth) {
        double top = margin != null ? snapper.snapSpaceY(margin.getTop()) : 0;
        double bottom = margin != null ? snapper.snapSpaceY(margin.getBottom()) : 0;

        double alt = -1;
        if (availableWidth != -1 && child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            double contentWidth = computeContentWidth(snapper, margin, availableWidth);

            alt = computedBoundedWidth(snapper, child, fillWidth, contentWidth);
        }

        // For explanation, see computeChildPrefAreaHeight
        if (minBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT) {
                return top + snapper.snapSizeY(child.minHeight(alt)) + bottom
                        + minBaselineComplement;
            }

            return baseline + minBaselineComplement;
        }

        return top + snapper.snapSizeY(child.minHeight(alt)) + bottom;
    }

    static double computeChildPrefAreaWidth(Snapper snapper, Layoutable child, Insets margin) {
        return computeChildPrefAreaWidth(snapper, child, -1, margin, -1, false);
    }

    static double computeChildPrefAreaWidth(Snapper snapper, Layoutable child, double baselineComplement, Insets margin, double availableHeight, boolean fillHeight) {
        double left = margin != null ? snapper.snapSpaceX(margin.getLeft()) : 0;
        double right = margin != null ? snapper.snapSpaceX(margin.getRight()) : 0;
        double alt = -1;
        if (availableHeight != -1 && child.isResizable() && child.getContentBias() == Orientation.VERTICAL) {
            double top = margin != null ? snapper.snapSpaceY(margin.getTop()) : 0;
            double bottom = margin != null ? snapper.snapSpaceY(margin.getBottom()) : 0;
            double bo = child.getBaselineOffset();
            final double contentHeight = bo == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT && baselineComplement != -1 ?
                    availableHeight - top - bottom - baselineComplement :
                    availableHeight - top - bottom;
            alt = computedBoundedHeight(snapper, child, fillHeight, contentHeight);
        }
        return left + snapper.snapSizeX(boundedSize(child.minWidth(alt), child.prefWidth(alt), child.maxWidth(alt))) + right;
    }

    static double computeChildPrefAreaHeight(Snapper snapper, Layoutable child, Insets margin) {
        return computeChildPrefAreaHeight(snapper, child, -1, margin, -1, false);
    }

    static double computeChildPrefAreaHeight(Snapper snapper, Layoutable child, double prefBaselineComplement, Insets margin, double availableWidth, boolean fillWidth) {
        double top = margin != null ? snapper.snapSpaceY(margin.getTop()) : 0;
        double bottom = margin != null ? snapper.snapSpaceY(margin.getBottom()) : 0;

        double alt = -1;
        if (availableWidth != -1 && child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) {
            double contentWidth = computeContentWidth(snapper, margin, availableWidth);

            alt = computedBoundedWidth(snapper, child, fillWidth, contentWidth);
        }

        if (prefBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT) {
                // When baseline is same as height, the preferred height of the node will be above the baseline, so we need to add
                // the preferred complement to it
                return top + snapper.snapSizeY(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom
                        + prefBaselineComplement;
            }

            // For all other Nodes, it's just their baseline and the complement.
            // Note that the complement already contain the Node's preferred (or fixed) height
            return top + baseline + prefBaselineComplement + bottom;
        }

        return top + snapper.snapSizeY(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt))) + bottom;
    }

    static double computeChildMaxAreaWidth(Snapper snapper, Node child, double baselineComplement, Insets margin, double availableHeight, boolean fillHeight) {
        double max = child.maxWidth(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }
        double left = margin != null ? snapper.snapSpaceX(margin.getLeft()) : 0;
        double right = margin != null ? snapper.snapSpaceX(margin.getRight()) : 0;
        double alt = -1;
        if (availableHeight != -1 && child.isResizable() && child.getContentBias() == Orientation.VERTICAL) { // width depends on height
            double top = margin != null ? snapper.snapSpaceY(margin.getTop()) : 0;
            double bottom = (margin != null ? snapper.snapSpaceY(margin.getBottom()) : 0);
            double bo = child.getBaselineOffset();
            final double contentHeight = bo == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT && baselineComplement != -1 ?
                    availableHeight - top - bottom - baselineComplement :
                    availableHeight - top - bottom;

            alt = computedBoundedHeight(snapper, child, fillHeight, contentHeight);
            max = child.maxWidth(alt);
        }
        // if min > max, min wins, so still need to call boundedSize()
        return left + snapper.snapSizeX(boundedSize(child.minWidth(alt), max, Double.MAX_VALUE)) + right;
    }

    static double computeChildMaxAreaHeight(Snapper snapper, Node child, double maxBaselineComplement, Insets margin, double availableWidth, boolean fillWidth) {
        double max = child.maxHeight(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }
        double top = margin != null? snapper.snapSpaceY(margin.getTop()) : 0;
        double bottom = margin != null? snapper.snapSpaceY(margin.getBottom()) : 0;
        double alt = -1;
        if (availableWidth != -1 && child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) { // height depends on width
            double contentWidth = computeContentWidth(snapper, margin, availableWidth);

            alt = computedBoundedWidth(snapper, child, fillWidth, contentWidth);
            max = child.maxHeight(alt);
        }
        // For explanation, see computeChildPrefAreaHeight
        if (maxBaselineComplement != -1) {
            double baseline = child.getBaselineOffset();
            if (child.isResizable() && baseline == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT) {
                return top + snapper.snapSizeY(boundedSize(child.minHeight(alt), max, Double.MAX_VALUE)) + bottom
                        + maxBaselineComplement;
            }

            return top + baseline + maxBaselineComplement + bottom;
        }

        // if min > max, min wins, so still need to call boundedSize()
        return top + snapper.snapSizeY(boundedSize(child.minHeight(alt), max, Double.MAX_VALUE)) + bottom;
    }

    /*
     * Definition of used terms:
     *
     * # available width/heights:
     *
     * Sizes provided by the container that may be used as a dependent value when
     * calculating sizes for biased controls. These may be set to -1 to indicate
     * no such information is available. If given, the sizes include the Margin
     * of the child. As such the Margin must be removed before passing these
     * values as a dependent value to min/pref/max width/height functions.
     *
     * # content width/heights:
     *
     * The space allocated to a child, minus its margins. A content size is
     * always a real value (not NaN) and never negative.
     *
     * # bounded width/heights:
     *
     * The space allocated to a child, minus its margins, adjusted according to
     * its constraints (min <= X <= max). A bounded size is always a real value
     * (not NaN) and never negative.
     */

    /*
     * Given a content width, limits it by the child's constraints. The fill boolean
     * controls whether the content width or the child's preferred width is used to compute
     * the bounded width.
     */
    private static double computedBoundedWidth(Snapper snapper, Layoutable child, boolean fill, double contentWidth) {
        double min = child.minWidth(-1);
        double max = child.maxWidth(-1);

        if (fill) {
            return snapper.snapSizeX(boundedSize(min, contentWidth, max));
        }

        return snapper.snapSizeX(boundedSize(min, child.prefWidth(-1), Math.min(max, contentWidth)));
    }

    /*
     * Given a content height, limits it by the child's constraints. The fill boolean
     * controls whether the content height or the child's preferred height is used to compute
     * the bounded height.
     */
    private static double computedBoundedHeight(Snapper snapper, Layoutable child, boolean fill, double contentHeight) {
        double min = child.minHeight(-1);
        double max = child.maxHeight(-1);

        if (fill) {
            return snapper.snapSizeY(boundedSize(min, contentHeight, max));
        }

        return snapper.snapSizeY(boundedSize(min, child.prefHeight(-1), Math.min(max, contentHeight)));
    }

    /*
     * Removes the given Margin (if any) from a width which still includes margins
     * to create a content width.
     */
    private static double computeContentWidth(Snapper snapper, Insets margin, double width) {
        double left = margin != null ? snapper.snapSpaceX(margin.getLeft()) : 0;
        double right = margin != null ? snapper.snapSpaceX(margin.getRight()) : 0;

        return width - left - right;
    }

    /* Max of children's minimum area widths */
    static double computeMaxMinAreaWidth(Snapper snapper, List<? extends Layoutable> children, Callback<Layoutable, Insets> margins, double height,
            boolean fillHeight) {
        return getMaxAreaWidth(snapper, children, margins, new double[] { height }, fillHeight, true);
    }

    /* Max of children's minimum area heights */
    static double computeMaxMinAreaHeight(Snapper snapper, List<? extends Layoutable> children, Callback<Layoutable, Insets> margins, double width,
            boolean fillWidth, VPos valignment) {
        return getMaxAreaHeight(snapper, children, margins, new double[] { width }, fillWidth, true, valignment);
    }

    /* Max of children's pref area widths */
    static double computeMaxPrefAreaWidth(Snapper snapper, List<? extends Layoutable> children, Callback<Layoutable, Insets> margins, double height,
            boolean fillHeight) {
        return getMaxAreaWidth(snapper, children, margins, new double[] { height }, fillHeight, false);
    }

    /* Max of children's pref area heights */
    static double computeMaxPrefAreaHeight(Snapper snapper, List<? extends Layoutable> children, Callback<Layoutable, Insets> margins, double width,
            boolean fillWidth, VPos valignment) {
        return getMaxAreaHeight(snapper, children, margins, new double[] { width }, fillWidth, false, valignment);
    }

    /* Utility method for computing the max of children's min or pref heights, taking into account baseline alignment. */
    static double getMaxAreaHeight(
        Snapper snapper, List<? extends Layoutable> children, Callback<Layoutable, Insets> childMargins, double[] childWidths,
        boolean fillWidth, boolean minimum, VPos valignment
    ) {
        final double singleChildWidth = childWidths == null ? -1 : childWidths.length == 1 ? childWidths[0] : Double.NaN;
        if (valignment == VPos.BASELINE) {
            double maxAbove = 0;
            double maxBelow = 0;
            for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
                final Layoutable child = children.get(i);
                final double childWidth = Double.isNaN(singleChildWidth) ? childWidths[i] : singleChildWidth;
                Insets margin = childMargins.call(child);
                final double top = margin != null ? snapper.snapSpaceY(margin.getTop()) : 0;
                final double bottom = margin != null ? snapper.snapSpaceY(margin.getBottom()) : 0;
                final double baseline = child.getBaselineOffset();

                final double childHeight = minimum ? snapper.snapSizeY(child.minHeight(childWidth)) : snapper.snapSizeY(child.prefHeight(childWidth));
                if (baseline == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT) {
                    maxAbove = Math.max(maxAbove, childHeight + top);
                } else {
                    maxAbove = Math.max(maxAbove, baseline + top);
                    maxBelow = Math.max(maxBelow,
                            snapper.snapSpaceY(minimum ? snapper.snapSizeY(child.minHeight(childWidth)) : snapper.snapSizeY(child.prefHeight(childWidth))) -
                            baseline + bottom);
                }
            }
            return maxAbove + maxBelow; //remind(aim): ceil this value?
        }

        double max = 0;
        for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
            final Layoutable child = children.get(i);
            Insets margin = childMargins.call(child);
            final double childWidth = Double.isNaN(singleChildWidth) ? childWidths[i] : singleChildWidth;

            max = Math.max(max, minimum?
                computeChildMinAreaHeight(snapper, child, -1, margin, childWidth, fillWidth) :
                    computeChildPrefAreaHeight(snapper, child, -1, margin, childWidth, fillWidth));
        }
        return max;
    }

    /* Utility method for computing the max of children's min or pref width, horizontal alignment is ignored for now. */
    static double getMaxAreaWidth(Snapper snapper, List<? extends Layoutable> children,
            Callback<Layoutable, Insets> childMargins, double[] childHeights, boolean fillHeight, boolean minimum) {
        final double singleChildHeight = childHeights == null ? -1 : childHeights.length == 1 ? childHeights[0] : Double.NaN;

        double max = 0;
        for (int i = 0, maxPos = children.size(); i < maxPos; i++) {
            final Layoutable child = children.get(i);
            final Insets margin = childMargins.call(child);
            final double childHeight = Double.isNaN(singleChildHeight) ? childHeights[i] : singleChildHeight;
            max = Math.max(max, minimum?
                computeChildMinAreaWidth(snapper, child, -1, margin, childHeight, fillHeight) :
                    computeChildPrefAreaWidth(snapper, child, -1, margin, childHeight, fillHeight));
        }

        return max;
    }

    static double getAreaBaselineOffset(
        Snapper snapper, List<? extends Layoutable> children, Callback<Layoutable, Insets> margins,
        Function<Integer, Double> positionToWidth, double areaHeight, boolean fillHeight
    ) {
        return getAreaBaselineOffset(snapper, children, margins, positionToWidth, areaHeight, _ -> fillHeight, getMinBaselineComplement(children));
    }

    /**
     * Returns the baseline offset of provided children, with respect to the minimum complement, computed
     * by {@link #getMinBaselineComplement(java.util.List)} from the same set of children.
     *
     * @param snapper the {@link Snapper} to use, cannot be {@code null}
     * @param children the children with baseline alignment
     * @param margins their margins (callback)
     * @param positionToWidth callback for children widths (can return -1 if no bias is used)
     * @param areaHeight height of the area to layout in
     * @param fillHeight callback to specify children that has fillHeight constraint
     * @param minComplement minimum complement
     */
    static double getAreaBaselineOffset(
        Snapper snapper, List<? extends Layoutable> children, Callback<Layoutable, Insets> margins,
        Function<Integer, Double> positionToWidth,
        double areaHeight, Function<Integer, Boolean> fillHeight, double minComplement
    ) {
        double b = 0;
        for (int i = 0; i < children.size(); ++i) {
            Layoutable l = children.get(i);
            Insets margin = margins.call(l);
            double top = margin != null ? snapper.snapSpaceY(margin.getTop()) : 0;
            double bottom = margin != null ? snapper.snapSpaceY(margin.getBottom()) : 0;
            final double bo = l.getBaselineOffset();
            if (bo == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT) {
                double alt = -1;
                if (l.getContentBias() == Orientation.HORIZONTAL) {
                    alt = positionToWidth.apply(i);
                }
                if (fillHeight.apply(i)) {
                    // If the children fills it's height, than it's "preferred" height is the area without the complement and insets
                    b = Math.max(b, top + boundedSize(l.minHeight(alt), areaHeight - minComplement - top - bottom,
                            l.maxHeight(alt)));
                } else {
                    // Otherwise, we must use the area without complement and insets as a maximum for the child
                    b = Math.max(b, top + boundedSize(l.minHeight(alt), l.prefHeight(alt),
                            Math.min(l.maxHeight(alt), areaHeight - minComplement - top - bottom)));
                }
            } else {
                b = Math.max(b, top + bo);
            }
        }
        return b;
    }

    /**
     * Return the minimum complement of baseline
     * @param children
     * @return
     */
    static double getMinBaselineComplement(List<? extends Layoutable> children) {
        return getBaselineComplement(children, true, false);
    }

    /**
     * Return the preferred complement of baseline
     * @param children
     * @return
     */
    static double getPrefBaselineComplement(List<? extends Layoutable> children) {
        return getBaselineComplement(children, false, false);
    }

    /**
     * Return the maximal complement of baseline
     * @param children
     * @return
     */
    static double getMaxBaselineComplement(List<? extends Layoutable> children) {
        return getBaselineComplement(children, false, true);
    }

    private static double getBaselineComplement(List<? extends Layoutable> children, boolean min, boolean max) {
        double bc = 0;
        for (Layoutable l : children) {
            final double bo = l.getBaselineOffset();
            if (bo == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT) {
                continue;
            }
            if (l.isResizable()) {
                bc = Math.max(bc, (min ? l.minHeight(-1) : max ? l.maxHeight(-1) : l.prefHeight(-1)) - bo);
            } else {
                bc = Math.max(bc, l.getLayoutBounds().getHeight() - bo);
            }
        }
        return bc;
    }

    /**
     * The bounded width and height of a child.
     *
     * @param width the bounded width
     * @param height the bounded height
     */
    record Size(double width, double height) {}

    /**
     * Returns the size of a Node that should be placed in an area of the specified size,
     * bounded in it's min/max size, respecting bias.
     *
     * @param child the child, cannot be {@code null}
     * @param areaWidth the width of the bounding area where the child is going to be placed
     * @param areaHeight the height of the bounding area where the child is going to be placed
     * @param fillWidth whether the child should try to fill the area width
     * @param fillHeight whether the child should try to fill the area height
     * @return the bounded size the child should be resized to, never {@code null}
     */
    static Size boundedSizeWithBias(Layoutable child, double areaWidth, double areaHeight, boolean fillWidth, boolean fillHeight) {
        Orientation bias = child.getContentBias();

        double childWidth = 0;
        double childHeight = 0;

        if (bias == null) {
            childWidth = boundedSize(
                    child.minWidth(-1), fillWidth ? areaWidth
                    : Math.min(areaWidth, child.prefWidth(-1)),
                    child.maxWidth(-1));
            childHeight = boundedSize(
                    child.minHeight(-1), fillHeight ? areaHeight
                    : Math.min(areaHeight, child.prefHeight(-1)),
                    child.maxHeight(-1));
        } else if (bias == Orientation.HORIZONTAL) {
            childWidth = boundedSize(
                    child.minWidth(-1), fillWidth ? areaWidth
                    : Math.min(areaWidth, child.prefWidth(-1)),
                    child.maxWidth(-1));
            childHeight = boundedSize(
                    child.minHeight(childWidth), fillHeight ? areaHeight
                    : Math.min(areaHeight, child.prefHeight(childWidth)),
                    child.maxHeight(childWidth));
        } else { // bias == VERTICAL
            childHeight = boundedSize(
                    child.minHeight(-1), fillHeight ? areaHeight
                    : Math.min(areaHeight, child.prefHeight(-1)),
                    child.maxHeight(-1));
            childWidth = boundedSize(
                    child.minWidth(childHeight), fillWidth ? areaWidth
                    : Math.min(areaWidth, child.prefWidth(childHeight)),
                    child.maxWidth(childHeight));
        }

        return new Size(childWidth, childHeight);
    }

    /**
     * Utility method which lays out the child within an area of it's
     * parent defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * If the child is resizable, this method will use {@code fillWidth} and {@code fillHeight}
     * to determine whether to resize it to fill the area or keep the child at its
     * preferred dimension.  If fillWidth/fillHeight are true, then this method
     * will only resize the child up to its max size limits.  If the child's maximum
     * size preference is less than the area size, the maximum size will be used.
     * If child's maximum is greater than the area size, then the child will be
     * resized to fit within the area, unless its minimum size prevents it.
     * <p>
     * If the child has a non-null contentBias, then this method will use it when
     * resizing the child.  If the contentBias is horizontal, it will set its width
     * first and then pass that value to compute the child's height.  If child's
     * contentBias is vertical, then it will set its height first
     * and pass that value to compute the child's width.  If the child's contentBias
     * is null, then it's width and height have no dependencies on each other.
     * <p>
     * If the child is not resizable (Shape, Group, etc) then it will only be
     * positioned and not resized.
     * <p>
     * If the child's resulting size differs from the area's size (either
     * because it was not resizable or it's sizing preferences prevented it), then
     * this function will align the node relative to the area using horizontal and
     * vertical alignment values.
     * If valignment is {@code VPos.BASELINE} then the node's baseline will be aligned
     * with the area baseline offset parameter, otherwise the baseline parameter
     * is ignored.
     * <p>
     * If {@code margin} is non-null, then that space will be allocated around the
     * child within the layout area.  margin may be null.
     * <p>
     * The resulting x,y values will be rounded to their nearest pixel
     * boundaries and the width/height values will be ceiled to the next
     * pixel boundary, as determined by the given {@code snapper}.
     *
     * @param snapper the {@link Snapper} to use, cannot be {@code null}
     * @param child the child being positioned within the area, cannot be {@code null}
     * @param areaX the horizontal offset of the layout area
     * @param areaY the vertical offset of the layout area
     * @param areaWidth the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param margin the margin of space to be allocated around the child
     * @param fillWidth whether or not the child should be resized to fill the area width or kept to its preferred width
     * @param fillHeight whether or not the child should e resized to fill the area height or kept to its preferred height
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     */
    static void layoutInArea(
        Snapper snapper, Layoutable child, double areaX, double areaY,
        double areaWidth, double areaHeight,
        double areaBaselineOffset,
        Insets margin, boolean fillWidth, boolean fillHeight,
        HPos halignment, VPos valignment
    ) {

        Insets childMargin = margin != null ? margin : Insets.EMPTY;
        double top = snapper.snapSpaceY(childMargin.getTop());
        double bottom = snapper.snapSpaceY(childMargin.getBottom());
        double left = snapper.snapSpaceX(childMargin.getLeft());
        double right = snapper.snapSpaceX(childMargin.getRight());

        if (valignment == VPos.BASELINE) {
            double bo = child.getBaselineOffset();
            if (bo == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT) {
                if (child.isResizable()) {
                    // Everything below the baseline is like an "inset". The Node with BASELINE_OFFSET_SAME_AS_HEIGHT cannot
                    // be resized to this area
                    bottom += snapper.snapSpaceY(areaHeight - areaBaselineOffset);
                } else {
                    top = snapper.snapSpaceY(areaBaselineOffset - child.getLayoutBounds().getHeight());
                }
            } else {
                top = snapper.snapSpaceY(areaBaselineOffset - bo);
            }
        }

        if (child.isResizable()) {
            Size size = boundedSizeWithBias(child, areaWidth - left - right, areaHeight - top - bottom, fillWidth, fillHeight);

            child.resize(snapper.snapSizeX(size.width()), snapper.snapSizeY(size.height()));
        }
        position(snapper, child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset,
                top, right, bottom, left, halignment, valignment);
    }

    private static void position(
        Snapper snapper, Layoutable child, double areaX, double areaY, double areaWidth, double areaHeight,
        double areaBaselineOffset,
        double topMargin, double rightMargin, double bottomMargin, double leftMargin,
        HPos hpos, VPos vpos
    ) {
        final double xoffset = leftMargin + computeXOffset(areaWidth - leftMargin - rightMargin,
                                                     child.getLayoutBounds().getWidth(), hpos);
        final double yoffset;
        if (vpos == VPos.BASELINE) {
            double bo = child.getBaselineOffset();

            if (bo == Measurable.BASELINE_OFFSET_SAME_AS_HEIGHT) {
                // We already know the layout bounds at this stage, so we can use them
                yoffset = areaBaselineOffset - child.getLayoutBounds().getHeight();
            } else {
                yoffset = areaBaselineOffset - bo;
            }
        } else {
            yoffset = topMargin + computeYOffset(areaHeight - topMargin - bottomMargin,
                                         child.getLayoutBounds().getHeight(), vpos);
        }

        double x = snapper.snapPositionX(areaX + xoffset);
        double y = snapper.snapPositionY(areaY + yoffset);

        child.relocate(x, y);
    }

    private static double computeXOffset(double width, double contentWidth, HPos hpos) {
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

    private static double computeYOffset(double height, double contentHeight, VPos vpos) {
        switch(vpos) {
            case BASELINE:
            case TOP:
                return 0;
            case CENTER:
                return (height - contentHeight) / 2;
            case BOTTOM:
                return height - contentHeight;
            default:
                throw new AssertionError("Unhandled vPos");
        }
    }

    /**
     * Computes the value based on the given min and max values. We encode in this
     * method the logic surrounding various edge cases, such as when the min is
     * specified as greater than the max, or the max less than the min, or a pref
     * value that exceeds either the max or min in their extremes.
     * <p/>
     * If the min is greater than the max, then we want to make sure the returned
     * value is the min. In other words, in such a case, the min becomes the only
     * acceptable return value.
     * <p/>
     * If the min and max values are well ordered, and the pref is less than the min
     * then the min is returned. Likewise, if the values are well ordered and the
     * pref is greater than the max, then the max is returned. If the pref lies
     * between the min and the max, then the pref is returned.
     *
     *
     * @param min The minimum bound
     * @param pref The value to be clamped between the min and max
     * @param max the maximum bound
     * @return the size bounded by min, pref, and max.
     */
    private static double boundedSize(double min, double pref, double max) {
        double a = pref >= min ? pref : min;
        double b = min >= max ? min : max;
        return a <= b ? a : b;
    }
}
