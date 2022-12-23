/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.geom.Vec2d;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;


/**
 * BorderPane lays out children in top, left, right, bottom, and center positions.
 *
 * <p> <img src="doc-files/borderpane.png" alt="A diagram that shows the position
 * of each child"> </p>
 *
 * The top and bottom children will be resized to their preferred heights and
 * extend the width of the border pane.  The left and right children will be resized
 * to their preferred widths and extend the length between the top and bottom nodes.
 * And the center node will be resized to fill the available space in the middle.
 * Any of the positions may be null.
 *
 * Example:
 * <pre><code>     <b>BorderPane borderPane = new BorderPane();</b>
 *     ToolBar toolbar = new ToolBar();
 *     HBox statusbar = new HBox();
 *     Node appContent = new AppContentNode();
 *     <b>borderPane.setTop(toolbar);
 *     borderPane.setCenter(appContent);
 *     borderPane.setBottom(statusbar);</b>
 * </code></pre>
 * <p>
 * Borderpanes may be styled with backgrounds and borders using CSS.  See
 * {@link javafx.scene.layout.Region Region} superclass for details.</p>
 *
 * <p>
 * BorderPane honors the minimum, preferred, and maximum sizes of its children.
 * If the child's resizable range prevents it from be resized to fit within its
 * position, it will be aligned relative to the space using a default alignment
 * as follows:
 * <ul>
 * <li>top: Pos.TOP_LEFT</li>
 * <li>bottom: Pos.BOTTOM_LEFT</li>
 * <li>left: Pos.TOP_LEFT</li>
 * <li>right: Pos.TOP_RIGHT</li>
 * <li>center: Pos.CENTER</li>
 * </ul>
 * See "Optional Layout Constraints" on how to customize these alignments.
 *
 * <p>
 * BorderPane lays out each child set in the five positions regardless of the child's
 * visible property value; unmanaged children are ignored.</p>
 *
 * <h2>Resizable Range</h2>
 * <p>
 * BorderPane is commonly used as the root of a {@link javafx.scene.Scene Scene},
 * in which case its size will track the size of the scene.  If the scene or stage
 * size has not been directly set by the application, the scene size will be
 * initialized to the border pane's preferred size.   However, if a border pane
 * has a parent other than the scene, that parent will resize the border pane within
 * the border pane's resizable range during layout.   By default the border pane
 * computes this range based on its content as outlined in the table below.
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
 * A border pane's unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned to it.
 * <p>
 * BorderPane provides properties for setting the size range directly.  These
 * properties default to the sentinel value Region.USE_COMPUTED_SIZE, however the
 * application may set them to other values as needed:
 * <pre><code>
 *     <b>borderPane.setPrefSize(500,400);</b>
 * </code></pre>
 * Applications may restore the computed values by setting these properties back
 * to Region.USE_COMPUTED_SIZE.
 * <p>
 * BorderPane does not clip its content by default, so it is possible that children's
 * bounds may extend outside its own bounds if a child's min size prevents it from
 * being fit within it space.</p>
 *
 * <h2>Optional Layout Constraints</h2>
 *
 * <p>
 * An application may set constraints on individual children to customize BorderPane's layout.
 * For each constraint, BorderPane provides a static method for setting it on the child.
 * </p>
 *
 * <table border="1">
 * <caption>BorderPane Constraint Table</caption>
 * <tr><th scope="col">Constraint</th><th scope="col">Type</th><th scope="col">Description</th></tr>
 * <tr><th scope="row">alignment</th><td>javafx.geometry.Pos</td><td>The alignment of the child within its area of the border pane.</td></tr>
 * <tr><th scope="row">margin</th><td>javafx.geometry.Insets</td><td>Margin space around the outside of the child.</td></tr>
 * </table>
 * <p>
 * Example:
 * <pre><code>     ListView list = new ListView();
 *     <b>BorderPane.setAlignment(list, Pos.TOP_LEFT);
 *     BorderPane.setMargin(list, new Insets(12,12,12,12));</b>
 *     borderPane.setCenter(list);
 * </code></pre>
 *
 * @since JavaFX 2.0
 */
public class BorderPane extends Pane {
    /* ******************************************************************
     *  BEGIN static methods
     ********************************************************************/

    private static final String MARGIN = "borderpane-margin";
    private static final String ALIGNMENT = "borderpane-alignment";

    /**
     * Sets the alignment for the child when contained by a border pane.
     * If set, will override the border pane's default alignment for the child's position.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a border pane
     * @param value the alignment position for the child
     */
    public static void setAlignment(Node child, Pos value) {
        setConstraint(child, ALIGNMENT, value);
    }

    /**
     * Returns the child's alignment constraint if set.
     * @param child the child node of a border pane
     * @return the alignment position for the child or null if no alignment was set
     */
    public static Pos getAlignment(Node child) {
        return (Pos)getConstraint(child, ALIGNMENT);
    }

    /**
     * Sets the margin for the child when contained by a border pane.
     * If set, the border pane will lay it out with the margin space around it.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a border pane
     * @param value the margin of space around the child
     */
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN, value);
    }

    /**
     * Returns the child's margin constraint if set.
     * @param child the child node of a border pane
     * @return the margin for the child or null if no margin was set
     */
    public static Insets getMargin(Node child) {
        return (Insets)getConstraint(child, MARGIN);
    }

    // convenience for handling null margins
    private static Insets getNodeMargin(Node child) {
        Insets margin = getMargin(child);
        return margin != null ? margin : Insets.EMPTY;
    }

    /**
     * Removes all border pane constraints from the child node.
     * @param child the child node
     */
    public static void clearConstraints(Node child) {
        setAlignment(child, null);
        setMargin(child, null);
    }

    /* ******************************************************************
     *  END static methods
     ********************************************************************/

    /**
     * Creates a BorderPane layout.
     */
    public BorderPane() {
        super();
    }

    /**
     * Creates an BorderPane layout with the given Node as the center of the BorderPane.
     * @param center The node to set as the center of the BorderPane.
     * @since JavaFX 8.0
     */
    public BorderPane(Node center) {
        super();
        setCenter(center);
    }

    /**
     * Creates an BorderPane layout with the given Nodes to use for each of the main
     * layout areas of the Border Pane. The top, right, bottom, and left nodes are listed
     * in clockwise order.
     * @param center The node to set as the center of the BorderPane.
     * @param top The node to set as the top of the BorderPane.
     * @param right The node to set as the right of the BorderPane.
     * @param bottom The node to set as the bottom of the BorderPane.
     * @param left The node to set as the left of the BorderPane.
     * @since JavaFX 8.0
     */
    public BorderPane(Node center, Node top, Node right, Node bottom, Node left) {
        super();
        setCenter(center);
        setTop(top);
        setRight(right);
        setBottom(bottom);
        setLeft(left);
    }

    /**
     * The node placed in the center of this border pane.
     * If resizable, it will be resized fill the center of the border pane
     * between the top, bottom, left, and right nodes.   If the node cannot be
     * resized to fill the center space (it's not resizable or its max size prevents
     * it) then it will be center aligned unless the child's alignment constraint
     * has been set.
     * @return the node placed in the center of this border pane
     */
    public final ObjectProperty<Node> centerProperty() {
        if (center == null) {
            center = new BorderPositionProperty("center");
        }
        return center;
    }
    private ObjectProperty<Node> center;
    public final void setCenter(Node value) { centerProperty().set(value); }
    public final Node getCenter() { return center == null ? null : center.get(); }

    /**
     * The node placed on the top edge of this border pane.
     * If resizable, it will be resized to its preferred height and it's width
     * will span the width of the border pane.  If the node cannot be
     * resized to fill the top space (it's not resizable or its max size prevents
     * it) then it will be aligned top-left within the space unless the child's
     * alignment constraint has been set.
     * @return the node placed on the top edge of this border pane
     */
    public final ObjectProperty<Node> topProperty() {
        if (top == null) {
            top = new BorderPositionProperty("top");
        }
        return top;
    }
    private ObjectProperty<Node> top;
    public final void setTop(Node value) { topProperty().set(value); }
    public final Node getTop() { return top == null ? null : top.get();  }

    /**
     * The node placed on the bottom edge of this border pane.
     * If resizable, it will be resized to its preferred height and it's width
     * will span the width of the border pane.  If the node cannot be
     * resized to fill the bottom space (it's not resizable or its max size prevents
     * it) then it will be aligned bottom-left within the space unless the child's
     * alignment constraint has been set.
     * @return the node placed on the bottom edge of this border pane
     */
    public final ObjectProperty<Node> bottomProperty() {
        if (bottom == null) {
            bottom = new BorderPositionProperty("bottom");
        }
        return bottom;
    }
    private ObjectProperty<Node> bottom;
    public final void setBottom(Node value) { bottomProperty().set(value); }
    public final Node getBottom() { return bottom == null ? null : bottom.get();  }

    /**
     * The node placed on the left edge of this border pane.
     * If resizable, it will be resized to its preferred width and it's height
     * will span the height of the border pane between the top and bottom nodes.
     * If the node cannot be resized to fill the left space (it's not resizable
     * or its max size prevents it) then it will be aligned top-left within the space
     * unless the child's alignment constraint has been set.
     * @return the node placed on the left edge of this border pane
     */
    public final ObjectProperty<Node> leftProperty() {
        if (left == null) {
            left = new BorderPositionProperty("left");
        }
        return left;
    }
    private ObjectProperty<Node> left;
    public final void setLeft(Node value) { leftProperty().set(value); }
    public final Node getLeft() { return left == null ? null : left.get(); }

    /**
     * The node placed on the right edge of this border pane.
     * If resizable, it will be resized to its preferred width and it's height
     * will span the height of the border pane between the top and bottom nodes.
     * If the node cannot be resized to fill the right space (it's not resizable
     * or its max size prevents it) then it will be aligned top-right within the space
     * unless the child's alignment constraint has been set.
     * @return the node placed on the right edge of this border pane
     */
    public final ObjectProperty<Node> rightProperty() {
        if (right == null) {
            right = new BorderPositionProperty("right");
        }
        return right;
    }
    private ObjectProperty<Node> right;
    public final void setRight(Node value) { rightProperty().set(value); }
    public final Node getRight() { return right == null ? null : right.get(); }

    /**
     * @return null unless the center, right, bottom, left or top has a content bias.
     */
    @Override public Orientation getContentBias() {
        final Node c = getCenter();
        if (c != null && c.isManaged() && c.getContentBias() != null) {
            return c.getContentBias();
        }

        final Node r = getRight();
        if (r != null && r.isManaged() && r.getContentBias() == Orientation.VERTICAL) {
            return r.getContentBias();
        }

        final Node l = getLeft();
        if (l != null && l.isManaged() && l.getContentBias() == Orientation.VERTICAL) {
            return l.getContentBias();
        }
        final Node b = getBottom();
        if (b != null && b.isManaged() && b.getContentBias() == Orientation.HORIZONTAL) {
            return b.getContentBias();
        }

        final Node t = getTop();
        if (t != null && t.isManaged() && t.getContentBias() == Orientation.HORIZONTAL) {
            return t.getContentBias();
        }


        return null;
    }

    @Override protected double computeMinWidth(double height) {
        double topMinWidth = getAreaWidth(getTop(), -1, true);
        double bottomMinWidth = getAreaWidth(getBottom(), -1, true);

        double leftPrefWidth;
        double rightPrefWidth;
        double centerMinWidth;

        if (height != -1 && (childHasContentBias(getLeft(), Orientation.VERTICAL) ||
                childHasContentBias(getRight(), Orientation.VERTICAL) ||
            childHasContentBias(getCenter(), Orientation.VERTICAL))) {
            double topPrefHeight = getAreaHeight(getTop(), -1, false);
            double bottomPrefHeight = getAreaHeight(getBottom(), -1, false);

            double middleAreaHeight = Math.max(0, height - topPrefHeight - bottomPrefHeight);

            leftPrefWidth = getAreaWidth(getLeft(), middleAreaHeight, false);
            rightPrefWidth = getAreaWidth(getRight(), middleAreaHeight, false);
            centerMinWidth = getAreaWidth(getCenter(), middleAreaHeight, true);
        } else {
            leftPrefWidth = getAreaWidth(getLeft(), -1, false);
            rightPrefWidth = getAreaWidth(getRight(), -1, false);
            centerMinWidth = getAreaWidth(getCenter(), -1, true);
        }

        final Insets insets = getInsets();
        return insets.getLeft() +
                Math.max(leftPrefWidth + centerMinWidth + rightPrefWidth, Math.max(topMinWidth,bottomMinWidth)) +
                insets.getRight();
    }

    @Override protected double computeMinHeight(double width) {
        final Insets insets = getInsets();

        // Bottom and top are always at their pref height
        double topPrefHeight = getAreaHeight(getTop(), width, false);
        double bottomPrefHeight = getAreaHeight(getBottom(), width, false);

        double leftMinHeight = getAreaHeight(getLeft(), -1, true);
        double rightMinHeight = getAreaHeight(getRight(), -1, true);

        double centerMinHeight;
        if (width != -1 && childHasContentBias(getCenter(), Orientation.HORIZONTAL)) {
            double leftPrefWidth = getAreaWidth(getLeft(), -1, false);
            double rightPrefWidth = getAreaWidth(getRight(), -1, false);
            centerMinHeight = getAreaHeight(getCenter(),
                    Math.max(0, width - leftPrefWidth - rightPrefWidth) , true);
        } else {
            centerMinHeight = getAreaHeight(getCenter(), -1, true);
        }

        double middleAreaMinHeigh = Math.max(centerMinHeight, Math.max(rightMinHeight, leftMinHeight));

        return insets.getTop() + topPrefHeight + middleAreaMinHeigh + bottomPrefHeight + insets.getBottom();
    }

    @Override protected double computePrefWidth(double height) {
        double topPrefWidth = getAreaWidth(getTop(), -1, false);
        double bottomPrefWidth = getAreaWidth(getBottom(), -1, false);

        double leftPrefWidth;
        double rightPrefWidth;
        double centerPrefWidth;

        if ( height != -1 && (childHasContentBias(getLeft(), Orientation.VERTICAL) ||
                childHasContentBias(getRight(), Orientation.VERTICAL) ||
            childHasContentBias(getCenter(), Orientation.VERTICAL))) {
            double topPrefHeight = getAreaHeight(getTop(), -1, false);
            double bottomPrefHeight = getAreaHeight(getBottom(), -1, false);

            double middleAreaHeight = Math.max(0, height - topPrefHeight - bottomPrefHeight);

            leftPrefWidth = getAreaWidth(getLeft(), middleAreaHeight, false);
            rightPrefWidth = getAreaWidth(getRight(), middleAreaHeight, false);
            centerPrefWidth = getAreaWidth(getCenter(), middleAreaHeight, false);
        } else {
            leftPrefWidth = getAreaWidth(getLeft(), -1, false);
            rightPrefWidth = getAreaWidth(getRight(), -1, false);
            centerPrefWidth = getAreaWidth(getCenter(), -1, false);
        }

        final Insets insets = getInsets();
        return insets.getLeft() +
                Math.max(leftPrefWidth + centerPrefWidth + rightPrefWidth, Math.max(topPrefWidth,bottomPrefWidth)) +
                insets.getRight();
    }

    @Override protected double computePrefHeight(double width) {
        final Insets insets = getInsets();

        double topPrefHeight = getAreaHeight(getTop(), width, false);
        double bottomPrefHeight = getAreaHeight(getBottom(), width, false);
        double leftPrefHeight = getAreaHeight(getLeft(), -1, false);
        double rightPrefHeight = getAreaHeight(getRight(), -1, false);

        double centerPrefHeight;
        if (width != -1 && childHasContentBias(getCenter(), Orientation.HORIZONTAL)) {
            double leftPrefWidth = getAreaWidth(getLeft(), -1, false);
            double rightPrefWidth = getAreaWidth(getRight(), -1, false);
            centerPrefHeight = getAreaHeight(getCenter(),
                    Math.max(0, width - leftPrefWidth - rightPrefWidth) , false);
        } else {
            centerPrefHeight = getAreaHeight(getCenter(), -1, false);
        }

        double middleAreaPrefHeigh = Math.max(centerPrefHeight, Math.max(rightPrefHeight, leftPrefHeight));

        return insets.getTop() + topPrefHeight + middleAreaPrefHeigh + bottomPrefHeight + insets.getBottom();
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        double width = getWidth();
        double height = getHeight();
        final Orientation bias = getContentBias();

        if (bias == null) {
            final double minWidth = minWidth(-1);
            final double minHeight = minHeight(-1);
            width = width < minWidth ? minWidth : width;
            height = height < minHeight ? minHeight : height;
        } else if (bias == Orientation.HORIZONTAL) {
            final double minWidth = minWidth(-1);
            width = width < minWidth ? minWidth : width;
            final double minHeight = minHeight(width);
            height = height < minHeight ? minHeight : height;
        } else {
            final double minHeight = minHeight(-1);
            height = height < minHeight ? minHeight : height;
            final double minWidth = minWidth(height);
            width = width < minWidth ? minWidth : width;
        }

        final double insideX = insets.getLeft();
        final double insideY = insets.getTop();
        final double insideWidth = width - insideX - insets.getRight();
        final double insideHeight = height - insideY - insets.getBottom();
        final Node c = getCenter();
        final Node r = getRight();
        final Node b = getBottom();
        final Node l = getLeft();
        final Node t = getTop();

        double topHeight = 0;
        if (t != null && t.isManaged()) {
            Insets topMargin = getNodeMargin(t);
            double adjustedWidth = adjustWidthByMargin(insideWidth, topMargin);
            double adjustedHeight = adjustHeightByMargin(insideHeight, topMargin);
            topHeight = snapSizeY(t.prefHeight(adjustedWidth));
            topHeight = Math.min(topHeight, adjustedHeight);
            Vec2d result = boundedNodeSizeWithBias(t, adjustedWidth,
                   topHeight, true, true, TEMP_VEC2D);
            topHeight = snapSizeY(result.y);
            t.resize(snapSizeX(result.x), topHeight);

            topHeight = snapSpaceY(topMargin.getBottom()) + topHeight + snapSpaceY(topMargin.getTop());
            Pos alignment = getAlignment(t);
            positionInArea(t, insideX, insideY, insideWidth, topHeight, 0/*ignore baseline*/,
                    topMargin,
                    alignment != null? alignment.getHpos() : HPos.LEFT,
                    alignment != null? alignment.getVpos() : VPos.TOP, isSnapToPixel());
        }

        double bottomHeight = 0;
        if (b != null && b.isManaged()) {
            Insets bottomMargin = getNodeMargin(b);
            double adjustedWidth = adjustWidthByMargin(insideWidth, bottomMargin);
            double adjustedHeight = adjustHeightByMargin(insideHeight - topHeight, bottomMargin);
            bottomHeight = snapSizeY(b.prefHeight(adjustedWidth));
            bottomHeight = Math.min(bottomHeight, adjustedHeight);
            Vec2d result = boundedNodeSizeWithBias(b, adjustedWidth,
                    bottomHeight, true, true, TEMP_VEC2D);
            bottomHeight = snapSizeY(result.y);
            b.resize(snapSizeX(result.x), bottomHeight);

            bottomHeight = snapSpaceY(bottomMargin.getBottom()) + bottomHeight + snapSpaceY(bottomMargin.getTop());
            Pos alignment = getAlignment(b);
            positionInArea(b, insideX, insideY + insideHeight - bottomHeight,
                    insideWidth, bottomHeight, 0/*ignore baseline*/,
                    bottomMargin,
                    alignment != null? alignment.getHpos() : HPos.LEFT,
                    alignment != null? alignment.getVpos() : VPos.BOTTOM, isSnapToPixel());
        }

        double leftWidth = 0;
        if (l != null && l.isManaged()) {
            Insets leftMargin = getNodeMargin(l);
            double adjustedWidth = adjustWidthByMargin(insideWidth, leftMargin);
            double adjustedHeight = adjustHeightByMargin(insideHeight - topHeight - bottomHeight, leftMargin); // ????
            leftWidth = snapSizeX(l.prefWidth(adjustedHeight));
            leftWidth = Math.min(leftWidth, adjustedWidth);
            Vec2d result = boundedNodeSizeWithBias(l, leftWidth, adjustedHeight,
                    true, true, TEMP_VEC2D);
            leftWidth = snapSizeX(result.x);
            l.resize(leftWidth, snapSizeY(result.y));

            leftWidth = snapSpaceX(leftMargin.getLeft()) + leftWidth + snapSpaceX(leftMargin.getRight());
            Pos alignment = getAlignment(l);
            positionInArea(l, insideX, insideY + topHeight,
                    leftWidth, insideHeight - topHeight - bottomHeight, 0/*ignore baseline*/,
                    leftMargin,
                    alignment != null? alignment.getHpos() : HPos.LEFT,
                    alignment != null? alignment.getVpos() : VPos.TOP, isSnapToPixel());
        }

        double rightWidth = 0;
        if (r != null && r.isManaged()) {
            Insets rightMargin = getNodeMargin(r);
            double adjustedWidth = adjustWidthByMargin(insideWidth - leftWidth, rightMargin);
            double adjustedHeight = adjustHeightByMargin(insideHeight - topHeight - bottomHeight, rightMargin);

            rightWidth = snapSizeX(r.prefWidth(adjustedHeight));
            rightWidth = Math.min(rightWidth, adjustedWidth);
            Vec2d result = boundedNodeSizeWithBias(r, rightWidth, adjustedHeight,
                    true, true, TEMP_VEC2D);
            rightWidth = snapSizeX(result.x);
            r.resize(rightWidth, snapSizeY(result.y));

            rightWidth = snapSpaceX(rightMargin.getLeft()) + rightWidth + snapSpaceX(rightMargin.getRight());
            Pos alignment = getAlignment(r);
            positionInArea(r, insideX + insideWidth - rightWidth, insideY + topHeight,
                    rightWidth, insideHeight - topHeight - bottomHeight, 0/*ignore baseline*/,
                    rightMargin,
                    alignment != null? alignment.getHpos() : HPos.RIGHT,
                    alignment != null? alignment.getVpos() : VPos.TOP, isSnapToPixel());
        }

        if (c != null && c.isManaged()) {
            Pos alignment = getAlignment(c);

            layoutInArea(c, insideX + leftWidth, insideY + topHeight,
                    insideWidth - leftWidth - rightWidth,
                    insideHeight - topHeight - bottomHeight, 0/*ignore baseline*/,
                    getNodeMargin(c),
                    alignment != null? alignment.getHpos() : HPos.CENTER,
                    alignment != null? alignment.getVpos() : VPos.CENTER);
        }
    }

    private double getAreaWidth(Node child, double height, boolean minimum) {
        if (child != null && child.isManaged()) {
            Insets margin = getNodeMargin(child);
            return minimum ? computeChildMinAreaWidth(child, -1, margin, height, false):
                                   computeChildPrefAreaWidth(child, -1, margin, height, false);
        }
        return 0;
    }

    private double getAreaHeight(Node child, double width, boolean minimum) {
        if (child != null && child.isManaged()) {
            Insets margin = getNodeMargin(child);
            return minimum ? computeChildMinAreaHeight(child, -1, margin, width):
                                   computeChildPrefAreaHeight(child, -1, margin, width);
        }
        return 0;
    }

    private boolean childHasContentBias(Node child, Orientation orientation) {
        if (child != null && child.isManaged()) {
            return child.getContentBias() == orientation;
        }
        return false;
    }

    /* *************************************************************************
     *                                                                         *
     *                         Private Inner Class                             *
     *                                                                         *
     **************************************************************************/

    private final class BorderPositionProperty extends ObjectPropertyBase<Node> {
        private Node oldValue = null;
        private final String propertyName;
        private boolean isBeingInvalidated;

        BorderPositionProperty(String propertyName) {
            this.propertyName = propertyName;
            getChildren().addListener(new ListChangeListener<Node>() {

                @Override
                public void onChanged(ListChangeListener.Change<? extends Node> c) {
                    if (oldValue == null || isBeingInvalidated) {
                        return;
                    }
                    while (c.next()) {
                        if (c.wasRemoved()) {
                            List<? extends Node> removed = c.getRemoved();
                            for (int i = 0, sz = removed.size(); i < sz; ++i) {
                                if (removed.get(i) == oldValue) {
                                    oldValue = null; // Do not remove again in invalidated
                                    set(null);
                                }
                            }
                        }
                    }
                }
            });
        }

        @Override
        protected void invalidated() {
            final List<Node> children = getChildren();

            isBeingInvalidated = true;
            try {
                if (oldValue != null) {
                    children.remove(oldValue);
                }

                final Node _value = get();
                this.oldValue = _value;

                if (_value != null) {
                    children.add(_value);
                }
            } finally {
                isBeingInvalidated = false;
            }
        }

        @Override
        public Object getBean() {
            return BorderPane.this;
        }

        @Override
        public String getName() {
            return propertyName;
        }
    }
}
