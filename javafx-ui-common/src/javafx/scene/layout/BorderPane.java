/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * <p> <img src="doc-files/borderpane.png"/> </p>
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
 * <li>top: Pos.TOP_RIGHT</li>
 * <li>center: Pos.CENTER</li>
 * </ul>
 * See "Optional Layout Constraints" on how to customize these alignments.
 *
 * <p>
 * BorderPane lays out each child set in the five positions regardless of the child's
 * visible property value; unmanaged children are ignored.</p>
 *
 * <h4>Resizable Range</h4>
 * BorderPane is commonly used as the root of a {@link javafx.scene.Scene Scene},
 * in which case its size will track the size of the scene.  If the scene or stage
 * size has not been directly set by the application, the scene size will be
 * initialized to the border pane's preferred size.   However, if a border pane
 * has a parent other than the scene, that parent will resize the border pane within
 * the border pane's resizable range during layout.   By default the border pane
 * computes this range based on its content as outlined in the table below.
 * <p>
 * <table border="1">
 * <tr><td></td><th>width</th><th>height</th></tr>
 * <tr><th>minimum</th>
 * <td>left/right insets plus width required to display right/left children at their pref widths and top/bottom/center with at least their min widths</td>
 * <td>top/bottom insets plus height required to display top/bottom children at their pref heights and left/right/center with at least their min heights</td></tr>
 * <tr><th>preferred</th>
 * <td>left/right insets plus width required to display display top/right/bottom/left/center children with at least their pref widths</td>
 * <td>top/bottom insets plus height required to display display top/right/bottom/left/center children with at least their pref heights</td></tr>
 * <tr><th>maximum</th>
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
 * BorderPane does not clip its content by default, so it is possible that childrens'
 * bounds may extend outside its own bounds if a child's min size prevents it from
 * being fit within it space.</p>
 *
 * <h4>Optional Layout Constraints</h4>
 *
 * An application may set constraints on individual children to customize BorderPane's layout.
 * For each constraint, BorderPane provides a static method for setting it on the child.
 * <p>
 * <table border="1">
 * <tr><th>Constraint</th><th>Type</th><th>Description</th></tr>
 * <tr><td>alignment</td><td>javafx.geometry.Pos</td><td>The alignment of the child within its area of the border pane.</td></tr>
 * <tr><td>margin</td><td>javafx.geometry.Insets</td><td>Margin space around the outside of the child.</td></tr>
 * </table>
 * <p>
 * Example:
 * <pre><code>     ListView list = new ListView();
 *     <b>BorderPane.setAlignment(list, Pos.TOP_LEFT);
 *     BorderPane.setMargin(list, new Insets(12,12,12,12));</b>
 *     borderPane.setCenter(list);
 * </code></pre>
 *
 */
public class BorderPane extends Pane {
    /********************************************************************
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

    /********************************************************************
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
        if (r != null && r.isManaged() && r.getContentBias() != null) {
            return r.getContentBias();
        }

        final Node b = getBottom();
        if (b != null && b.isManaged() && b.getContentBias() != null) {
            return b.getContentBias();
        }

        final Node l = getLeft();
        if (l != null && l.isManaged() && l.getContentBias() != null) {
            return l.getContentBias();
        }

        final Node t = getTop();
        if (t != null && t.isManaged() && t.getContentBias() != null) {
            return t.getContentBias();
        }

        return null;
    }

    @Override protected double computeMinWidth(double height) {
        double topMinWidth;
        double leftMinWidth;
        double centerMinWidth;
        double rightMinWidth;
        double bottomMinWidth;

        final Node c = getCenter();
        final Node r = getRight();
        final Node b = getBottom();
        final Node l = getLeft();
        final Node t = getTop();

        if (getContentBias() == Orientation.VERTICAL) {
            final double h[] = adjustAreaHeight(height, -1);
            topMinWidth = getAreaWidth(t, h[0], true);
            leftMinWidth = getAreaWidth(l, h[1], true);
            centerMinWidth = getAreaWidth(c, h[2], true);
            rightMinWidth = getAreaWidth(r, h[3], true);
            bottomMinWidth = getAreaWidth(b, h[4], true);
        } else {
            topMinWidth = t != null? computeChildMinAreaWidth(t, getMargin(t)) : 0;
            leftMinWidth = l != null? computeChildMinAreaWidth(l, getMargin(l)) : 0;
            centerMinWidth = c != null? computeChildMinAreaWidth(c, getMargin(c)) : 0;
            rightMinWidth = r != null? computeChildMinAreaWidth(r, getMargin(r)) : 0;
            bottomMinWidth = b != null? computeChildMinAreaWidth(b, getMargin(b)) : 0;
        }

        final Insets insets = getInsets();
        return insets.getLeft() +
                Math.max(leftMinWidth + centerMinWidth + rightMinWidth, Math.max(topMinWidth,bottomMinWidth)) +
                insets.getRight();
    }

    @Override protected double computeMinHeight(double width) {
        double topMinHeight;
        double bottomMinHeight;
        double leftMinHeight;
        double centerMinHeight;
        double rightMinHeight;

        final Node c = getCenter();
        final Node r = getRight();
        final Node b = getBottom();
        final Node l = getLeft();
        final Node t = getTop();

        if (getContentBias() == Orientation.HORIZONTAL) {
            final double w[] = adjustAreaWidth(width, -1);
            topMinHeight = getAreaHeight(t, width, true);
            leftMinHeight = getAreaHeight(l, w[0], true);
            centerMinHeight = getAreaHeight(c, w[1], true);
            rightMinHeight = getAreaHeight(r, w[2], true);
            bottomMinHeight = getAreaHeight(b, width, true);
        } else {
            topMinHeight = t != null? computeChildMinAreaHeight(t, getMargin(t)) : 0;
            leftMinHeight = l != null? computeChildMinAreaHeight(l, getMargin(l)) : 0;
            centerMinHeight = c != null? computeChildMinAreaHeight(c, getMargin(c)) : 0;
            rightMinHeight = r != null? computeChildMinAreaHeight(r, getMargin(r)) : 0;
            bottomMinHeight = b != null? computeChildMinAreaHeight(b, getMargin(b)) : 0;
        }

        final Insets insets = getInsets();
        return insets.getTop() + topMinHeight +
                Math.max(centerMinHeight, Math.max(rightMinHeight,leftMinHeight)) +
                bottomMinHeight + insets.getBottom();
    }

    @Override protected double computePrefWidth(double height) {
        double topPrefWidth;
        double leftPrefWidth;
        double centerPrefWidth;
        double rightPrefWidth;
        double bottomPrefWidth;

        final Node c = getCenter();
        final Node r = getRight();
        final Node b = getBottom();
        final Node l = getLeft();
        final Node t = getTop();

        if (getContentBias() == Orientation.VERTICAL) {
            final double h[] = adjustAreaHeight(height, -1);
            topPrefWidth = getAreaWidth(t, h[0], false);
            leftPrefWidth = getAreaWidth(l, h[1], false);
            centerPrefWidth = getAreaWidth(c, h[2], false);
            rightPrefWidth = getAreaWidth(r, h[3], false);
            bottomPrefWidth = getAreaWidth(b, h[4], false);
        } else {
            double centerPrefHeight = c != null? computeChildPrefAreaHeight(c, getMargin(c)) : 0;
            double leftPrefHeight = l != null? computeChildPrefAreaHeight(l, getMargin(l)) : 0;
            double rightPrefHeight = r != null? computeChildPrefAreaHeight(r, getMargin(r)) : 0;
            double maxHeight = Math.max(centerPrefHeight, Math.max(rightPrefHeight, leftPrefHeight));
            leftPrefWidth = l != null? computeChildPrefAreaWidth(l, getMargin(l), maxHeight) : 0;
            rightPrefWidth = r != null? computeChildPrefAreaWidth(r, getMargin(r), maxHeight) : 0;
            centerPrefWidth = c != null? computeChildPrefAreaWidth(c, getMargin(c), maxHeight) : 0;
            topPrefWidth = t != null? computeChildPrefAreaWidth(t, getMargin(t)) : 0;
            bottomPrefWidth = b != null? computeChildPrefAreaWidth(b, getMargin(b)) : 0;
        }

        final Insets insets = getInsets();
        return insets.getLeft() +
                Math.max(leftPrefWidth + centerPrefWidth + rightPrefWidth, Math.max(topPrefWidth,bottomPrefWidth)) +
                insets.getRight();
    }

    @Override protected double computePrefHeight(double width) {
        double topPrefHeight;
        double bottomPrefHeight;
        double leftPrefHeight;
        double centerPrefHeight;
        double rightPrefHeight;
        double maxHeight;

        final Node c = getCenter();
        final Node r = getRight();
        final Node b = getBottom();
        final Node l = getLeft();
        final Node t = getTop();
        final Insets insets = getInsets();

        if (getContentBias() == Orientation.HORIZONTAL) {
            final double w[] = adjustAreaWidth(width, -1);
            topPrefHeight = getAreaHeight(t, width, false);
            leftPrefHeight = getAreaHeight(l, w[0], false);
            centerPrefHeight = getAreaHeight(c, w[1], false);
            rightPrefHeight = getAreaHeight(r, w[2], false);
            bottomPrefHeight = getAreaHeight(b, width, false);
            maxHeight = Math.max(centerPrefHeight, Math.max(rightPrefHeight, leftPrefHeight));
        } else {
            final Insets centerMargin = c == null ? null : getMargin(c);
            final Insets rightMargin = r == null ? null : getMargin(r);
            final Insets bottomMargin = b == null ? null : getMargin(b);
            final Insets leftMargin = l == null ? null : getMargin(l);
            final Insets topMargin = t == null ? null : getMargin(t);
            centerPrefHeight = c != null? computeChildPrefAreaHeight(c, centerMargin) : 0;
            leftPrefHeight = l != null? computeChildPrefAreaHeight(l, leftMargin) : 0;
            rightPrefHeight = r != null? computeChildPrefAreaHeight(r, rightMargin) : 0;
            maxHeight = Math.max(centerPrefHeight, Math.max(rightPrefHeight, leftPrefHeight));
            double leftPrefWidth = l != null? computeChildPrefAreaWidth(l, leftMargin, maxHeight) : 0;
            double rightPrefWidth = r != null? computeChildPrefAreaWidth(r, rightMargin, maxHeight) : 0;
            double centerPrefWidth = c != null? computeChildPrefAreaWidth(c, centerMargin, maxHeight) : 0;
            double topPrefWidth = t != null? computeChildPrefAreaWidth(t, topMargin) : 0;
            double bottomPrefWidth = b != null? computeChildPrefAreaWidth(b, bottomMargin) : 0;
            double prefWidth = insets.getLeft() +
                    Math.max(leftPrefWidth + centerPrefWidth + rightPrefWidth, Math.max(topPrefWidth,bottomPrefWidth)) +
                    insets.getRight();

            topPrefHeight = t != null? computeChildPrefAreaHeight(t, topMargin, prefWidth) : 0;
            bottomPrefHeight = b != null? computeChildPrefAreaHeight(b, bottomMargin, prefWidth) : 0;
        }

        return insets.getTop() + topPrefHeight + maxHeight + bottomPrefHeight + insets.getBottom();
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        final double width = getWidth();
        final double height = getHeight();
        final double insideX = insets.getLeft();
        final double insideY = insets.getTop();
        final double insideWidth = width - insideX - insets.getRight();
        final double insideHeight = height - insideY - insets.getBottom();
        final double widths[] = adjustAreaWidth(width, height);
        final double heights[] = adjustAreaHeight(height, width);
        final Node c = getCenter();
        final Node r = getRight();
        final Node b = getBottom();
        final Node l = getLeft();
        final Node t = getTop();

        double topHeight = 0;
        Insets topMargin = null;
        if (t != null && t.isManaged()) {
            topMargin = getNodeMargin(t);
            if (getContentBias() == Orientation.VERTICAL) {
                topHeight = heights[0] == -1 ? t.prefHeight(-1) : heights[0];
            } else {
                topHeight = snapSize(topMargin.getTop() +
                        t.prefHeight(insideWidth - topMargin.getLeft() - topMargin.getRight()) +
                        topMargin.getBottom());
            }
        }

        double bottomHeight = 0;
        Insets bottomMargin = null;
        if (b != null && b.isManaged()) {
            bottomMargin = getNodeMargin(b);
            if (getContentBias() == Orientation.VERTICAL) {
                bottomHeight = heights[4] == -1 ? b.prefHeight(-1) : heights[4];
            } else {
                bottomHeight = snapSize(bottomMargin.getTop() +
                        b.prefHeight(insideWidth - bottomMargin.getLeft() - bottomMargin.getRight()) +
                        bottomMargin.getBottom());
            }
        }

        double leftWidth = 0;
        Insets leftMargin = null;
        if (l != null && l.isManaged()) {
            leftMargin = getNodeMargin(l);
            if (getContentBias() == Orientation.HORIZONTAL) {
                leftWidth =  widths[0] == -1 ? l.prefWidth(-1) : widths[0];
            } else {
                leftWidth = snapSize(leftMargin.getLeft() +
                    l.prefWidth(insideHeight - topHeight - bottomHeight - leftMargin.getTop() - leftMargin.getBottom()) +
                    leftMargin.getRight());
            }
        }

        double rightWidth = 0;
        Insets rightMargin = null;
        if (r != null && r.isManaged()) {
            rightMargin = getNodeMargin(r);
            if (getContentBias() == Orientation.HORIZONTAL) {
                rightWidth = widths[2] == -1 ? r.prefWidth(-1) : widths[2];
            } else {
                rightWidth = snapSize(rightMargin.getLeft() +
                        r.prefWidth(insideHeight - topHeight - bottomHeight - rightMargin.getTop() - rightMargin.getBottom()) +
                        rightMargin.getRight());
            }
        }

        if (t != null && t.isManaged()) {
            Pos alignment = getAlignment(t);
            topHeight = Math.min(topHeight, insideHeight);
            layoutInArea(t, insideX, insideY, insideWidth, topHeight, 0/*ignore baseline*/,
                    topMargin,
                    alignment != null? alignment.getHpos() : HPos.LEFT,
                    alignment != null? alignment.getVpos() : VPos.TOP);
        }

        if (b != null && b.isManaged() ) {
            Pos alignment = getAlignment(b);
            bottomHeight = Math.min(bottomHeight, insideHeight - topHeight);
            layoutInArea(b, insideX, insideY + insideHeight - bottomHeight,
                    insideWidth, bottomHeight, 0/*ignore baseline*/,
                    bottomMargin,
                    alignment != null? alignment.getHpos() : HPos.LEFT,
                    alignment != null? alignment.getVpos() : VPos.BOTTOM);
        }

        if (l != null && l.isManaged()) {
            Pos alignment = getAlignment(l);
            leftWidth = Math.min(leftWidth, insideWidth);
            layoutInArea(l, insideX, insideY + topHeight,
                    leftWidth, insideHeight - topHeight - bottomHeight, 0/*ignore baseline*/,
                    leftMargin,
                    alignment != null? alignment.getHpos() : HPos.LEFT,
                    alignment != null? alignment.getVpos() : VPos.TOP);
        }

        if (r != null && r.isManaged()) {
            Pos alignment = getAlignment(r);
            rightWidth = Math.min(rightWidth, insideWidth - leftWidth);
            layoutInArea(r, insideX + insideWidth - rightWidth, insideY + topHeight,
                    rightWidth, insideHeight - topHeight - bottomHeight, 0/*ignore baseline*/,
                    rightMargin,
                    alignment != null? alignment.getHpos() : HPos.RIGHT,
                    alignment != null? alignment.getVpos() : VPos.TOP);
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
            return minimum ? computeChildMinAreaWidth(child, margin, height):
                                   computeChildPrefAreaWidth(child, margin, height);
        }
        return 0;
    }

    private double getAreaHeight(Node child, double width, boolean minimum) {
        if (child != null && child.isManaged()) {
            Insets margin = getNodeMargin(child);
            return minimum ? computeChildMinAreaHeight(child, margin, width):
                                   computeChildPrefAreaHeight(child, margin, width);
        }
        return 0;
    }

    private boolean childHasContentBias(Node child, Orientation orientation) {
        if (child != null && child.isManaged()) {
            return child.getContentBias() == orientation;
        }
        return false;
    }

    private double getAreaLimitWidth(Node child, boolean shrinking, double height) {
        if (child != null && child.isManaged()) {
            Insets margin = getNodeMargin(child);
            return shrinking ? computeChildMinAreaWidth(child, margin, height) :
                computeChildMaxAreaWidth(child, margin, height);
        }
        return 0;
    }

    private double[] adjustAreaWidth(double width, double height) {
        double actualWidth[] = new double[3];
        actualWidth[0] = getAreaWidth(getLeft(), -1, false);
        actualWidth[1] = getAreaWidth(getCenter(), -1, false);
        actualWidth[2] = getAreaWidth(getRight(), -1, false);

        double contentWidths = actualWidth[0] + actualWidth[1] + actualWidth[2];
        double extraWidth = width - contentWidths;
        boolean shrinking = extraWidth < 0;

        boolean contentBias[] = new boolean[3];
        contentBias[0] = childHasContentBias(getLeft(), Orientation.HORIZONTAL);
        contentBias[1] = childHasContentBias(getCenter(), Orientation.HORIZONTAL);
        contentBias[2] = childHasContentBias(getRight(), Orientation.HORIZONTAL);

        double[] areaLimitWidth = new double[3];
        areaLimitWidth[0] = getAreaLimitWidth(getLeft(), shrinking, height);
        areaLimitWidth[1] = getAreaLimitWidth(getCenter(), shrinking, height);
        areaLimitWidth[2] = getAreaLimitWidth(getRight(), shrinking, height);

        double availableWidth = width;
        double w[] = {-1, -1, -1};
        int numBiases = w.length;
        if (width != -1 && getContentBias() == Orientation.HORIZONTAL) {
            for (int i = 0; i < w.length; i++) {
                if (!contentBias[i]) {
                    w[i] = -1;
                    numBiases--;
                    if (shrinking) {
                        availableWidth -= actualWidth[i];
                    }
                }
            }

            extraWidth = extraWidth/numBiases;
            for (int i = 0; i < w.length; i++) {
                if (!shrinking) {
                    if (contentBias[i]) {
                        double grow = actualWidth[i] + extraWidth;
                        if (grow < areaLimitWidth[i]) {
                            w[i] = grow;
                        } else {
                            w[i] = areaLimitWidth[i];
                        }
                    }
                } else {
                    if (availableWidth > 0) {
                        if (contentBias[i]) {
                            if (availableWidth > areaLimitWidth[i]) {
                                w[i] = availableWidth/numBiases;
                            } else {
                                w[i] = areaLimitWidth[i];
                            }
                        }
                    }
                }
            }
        }
        return w;
    }

    private double getAreaLimitHeight(Node child, boolean shrinking, double width) {
        if (child != null && child.isManaged()) {
            Insets margin = getNodeMargin(child);
            return shrinking ? computeChildMinAreaHeight(child, margin, width) :
                computeChildMaxAreaHeight(child, margin, width);
        }
        return 0;
    }

    private double[] adjustAreaHeight(double height, double width) {
        final Node c = getCenter();
        final Node r = getRight();
        final Node b = getBottom();
        final Node l = getLeft();
        final Node t = getTop();

        double actualHeight[] = new double[5];
        actualHeight[0] = getAreaHeight(t, -1, false);
        actualHeight[1] = getAreaHeight(l, -1, false);
        actualHeight[2] = getAreaHeight(c, -1, false);
        actualHeight[3] = getAreaHeight(r, -1, false);
        actualHeight[4] = getAreaHeight(b, -1, false);

        double contentHeight = Math.max(actualHeight[1], Math.max(actualHeight[2], actualHeight[3]));
        contentHeight += (actualHeight[0] + actualHeight[4]);

        double extraHeight = height - contentHeight;
        boolean shrinking = extraHeight < 0;

        boolean contentBias[] = new boolean[5];
        contentBias[0] = childHasContentBias(t, Orientation.VERTICAL);
        contentBias[1] = childHasContentBias(l, Orientation.VERTICAL);
        contentBias[2] = childHasContentBias(c, Orientation.VERTICAL);
        contentBias[3] = childHasContentBias(r, Orientation.VERTICAL);
        contentBias[4] = childHasContentBias(b, Orientation.VERTICAL);

        double areaLimitHeight[] = new double[5];
        areaLimitHeight[0] = getAreaLimitHeight(t, shrinking, width);
        areaLimitHeight[1] = getAreaLimitHeight(l, shrinking, width);
        areaLimitHeight[2] = getAreaLimitHeight(c, shrinking, width);
        areaLimitHeight[3] = getAreaLimitHeight(r, shrinking, width);
        areaLimitHeight[4] = getAreaLimitHeight(b, shrinking, width);

        double availableHeight = height;
        double h[] = {-1, -1, -1, -1, -1};
        int numBiases = h.length;

        if (height != -1 && getContentBias() == Orientation.VERTICAL) {

            double middleRowContentHeight = 0;
            for (int i = 1; i < 4; i++) {
                if (!contentBias[i]) {
                    middleRowContentHeight = Math.max(middleRowContentHeight, actualHeight[i]);
                }
            }
            for (int i = 0; i < h.length; i++) {
                if (!contentBias[i]) {
                    h[i] = -1;
                    numBiases--;
                    if (shrinking) {
                        if (i < 1 || i > 3) {
                            availableHeight -= actualHeight[i];
                        } else if (i == 1) {
                            // We only need to subtract the middle row once.
                            availableHeight -= middleRowContentHeight;
                        }
                    }
                }
            }

            extraHeight = extraHeight/numBiases;
            for (int i = 0; i < h.length; i++) {
                if (!shrinking) {
                    if (contentBias[i]) {
                        double grow = actualHeight[i] + extraHeight;
                        if (grow < areaLimitHeight[i]) {
                            h[i] = grow;
                        } else {
                            h[i] = areaLimitHeight[i];
                        }
                    }
                } else {
                    if (availableHeight > 0) {
                        if (contentBias[i]) {
                            if (availableHeight > areaLimitHeight[i]) {
                                h[i] = availableHeight/numBiases;
                            } else {
                                h[i] = areaLimitHeight[i];
                            }
                        }
                    }
                }
            }
        }
        return h;
    }

    /***************************************************************************
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
