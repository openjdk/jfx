/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
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
 * extend the width of the borderpane.  The left and right children will be resized
 * to their preferred widths and extend the length between the top and bottom nodes.
 * And the center node will be resized to fill the available space in the middle.
 * Any of the positions may be null.
 *  
 * Example:
 * <pre><code>     <b>BorderPane borderpane = new BorderPane();</b>
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
 * initialized to the borderpane's preferred size.   However, if a borderpane
 * has a parent other than the scene, that parent will resize the borderpane within
 * the borderpane's resizable range during layout.   By default the borderpane
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
 * A borderpane's unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned to it.
 * <p>
 * BorderPane provides properties for setting the size range directly.  These
 * properties default to the sentinel value Region.USE_COMPUTED_SIZE, however the
 * application may set them to other values as needed:
 * <pre><code>
 *     <b>borderpane.setPrefSize(500,400);</b>
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
 * <tr><td>alignment</td><td>javafx.geometry.Pos</td><td>The alignment of the child within its area of the borderpane.</td></tr>
 * <tr><td>margin</td><td>javafx.geometry.Insets</td><td>Margin space around the outside of the child.</td></tr>
 * </table>
 * <p>
 * Example:
 * <pre><code>     ListView list = new ListView();
 *     <b>BorderPane.setAlignment(list, Pos.TOP_LEFT);
 *     BorderPane.setMargin(list, new Insets(12,12,12,12));</b>
 *     borderpane.setCenter(list);
 * </code></pre>
 *
 */
public class BorderPane extends Pane {
    //TODO(aim): add topLeftCorner,topRightCorner,bottomLeftCorner,bottomRightCorner
    /********************************************************************
     *  BEGIN static methods
     ********************************************************************/

    private static final String MARGIN = "borderpane-margin";
    private static final String ALIGNMENT = "borderpane-alignment";

    /**
     * Sets the alignment for the child when contained by a borderpane.
     * If set, will override the borderpane's default alignment for the child's position.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a borderpane
     * @param value the alignment position for the child
     */
    public static void setAlignment(Node child, Pos value) {
        setConstraint(child, ALIGNMENT, value);
    }

    /**
     * Returns the child's alignment constraint if set.
     * @param child the child node of a borderpane
     * @return the alignment position for the child or null if no alignment was set
     */
    public static Pos getAlignment(Node child) {
        return (Pos)getConstraint(child, ALIGNMENT);
    }

    /**
     * Sets the margin for the child when contained by a borderpane.
     * If set, the borderpane will lay it out with the margin space around it.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a borderpane
     * @param value the margin of space around the child
     */
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN, value);
    }

    /**
     * Returns the child's margin constraint if set.
     * @param child the child node of a borderpane
     * @return the margin for the child or null if no margin was set
     */
    public static Insets getMargin(Node child) {
        return (Insets)getConstraint(child, MARGIN);
    }

    // convenience for handling null margins
    private static Insets getNodeMargin(Node child) {
        Insets margin = getMargin(child);
        return margin != null? margin : Insets.EMPTY;
    }

    /**
     * Removes all borderpane constraints from the child node.
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

    private ObjectProperty<Node> createObjectPropertyModelImpl(
            final String propertyName) {
        return new ObjectPropertyBase<Node>() {

                Node oldValue = null;

                @Override
                protected void invalidated() {
                    Node _value = get();
                    if (oldValue != null) {
                        getChildren().remove(oldValue);
                    }
                    this.oldValue = _value;
                    if (_value != null) {
                        getChildren().add(_value);
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
        };
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
            center = createObjectPropertyModelImpl("center");
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
            top = createObjectPropertyModelImpl("top");
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
            bottom = createObjectPropertyModelImpl("bottom");
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
            left = createObjectPropertyModelImpl("left");
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
            right = createObjectPropertyModelImpl("right");
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
        if (getCenter() != null && getCenter().isManaged() && getCenter().getContentBias() != null) {
            return getCenter().getContentBias();
        } else if (getRight() != null && getRight().isManaged() && getRight().getContentBias() != null) {
            return getRight().getContentBias();
        } else if (getBottom() != null && getBottom().isManaged() && getBottom().getContentBias() != null) {
            return getBottom().getContentBias();
        } else if (getLeft() != null && getLeft().isManaged() && getLeft().getContentBias() != null) {
            return getLeft().getContentBias();
        } else if (getTop() != null && getTop().isManaged() && getTop().getContentBias() != null) {
            return getTop().getContentBias();
        }
        return null;
    }

    @Override protected double computeMinWidth(double height) {
        double topMinWidth;
        double leftMinWidth;
        double centerMinWidth;
        double rightMinWidth;
        double bottomMinWidth;

        if (getContentBias() == Orientation.VERTICAL) {
            double h[] = adjustAreaHeight(height, -1);

            topMinWidth = getAreaWidth(getTop(), h[0], true);
            leftMinWidth = getAreaWidth(getLeft(), h[1], true);
            centerMinWidth = getAreaWidth(getCenter(), h[2], true);
            rightMinWidth = getAreaWidth(getRight(), h[3], true);
            bottomMinWidth = getAreaWidth(getBottom(), h[4], true);
        } else {
            topMinWidth = getTop() != null? computeChildMinAreaWidth(getTop(), getMargin(getTop())) : 0;
            leftMinWidth = getLeft() != null? computeChildMinAreaWidth(getLeft(), getMargin(getLeft())) : 0;
            centerMinWidth = getCenter() != null? computeChildMinAreaWidth(getCenter(), getMargin(getCenter())) : 0;
            rightMinWidth = getRight() != null? computeChildMinAreaWidth(getRight(), getMargin(getRight())) : 0;
            bottomMinWidth = getBottom() != null? computeChildMinAreaWidth(getBottom(), getMargin(getBottom())) : 0;
        }
        return getInsets().getLeft() +
                Math.max(leftMinWidth + centerMinWidth + rightMinWidth, Math.max(topMinWidth,bottomMinWidth)) +
                getInsets().getRight();
    }

    @Override protected double computeMinHeight(double width) {
        double topMinHeight = 0;
        double bottomMinHeight = 0;
        double leftMinHeight = 0;
        double centerMinHeight = 0;
        double rightMinHeight = 0;

        if (getContentBias() == Orientation.HORIZONTAL) {
            double w[] = adjustAreaWidth(width, -1);

            topMinHeight = getAreaHeight(getTop(), width, true);
            leftMinHeight = getAreaHeight(getLeft(), w[0], true);
            centerMinHeight = getAreaHeight(getCenter(), w[1], true);
            rightMinHeight = getAreaHeight(getRight(), w[2], true);
            bottomMinHeight = getAreaHeight(getBottom(), width, true);
        } else {
            topMinHeight = getTop() != null? computeChildMinAreaHeight(getTop(), getMargin(getTop())) : 0;
            leftMinHeight = getLeft() != null? computeChildMinAreaHeight(getLeft(), getMargin(getLeft())) : 0;
            centerMinHeight = getCenter() != null? computeChildMinAreaHeight(getCenter(), getMargin(getCenter())) : 0;
            rightMinHeight = getRight() != null? computeChildMinAreaHeight(getRight(), getMargin(getRight())) : 0;
            bottomMinHeight = getBottom() != null? computeChildMinAreaHeight(getBottom(), getMargin(getBottom())) : 0;
        }
        return getInsets().getTop() + topMinHeight +
                Math.max(centerMinHeight, Math.max(rightMinHeight,leftMinHeight)) +
                bottomMinHeight + getInsets().getBottom();
    }

    @Override protected double computePrefWidth(double height) {
        double topPrefWidth = 0;
        double leftPrefWidth = 0;
        double centerPrefWidth = 0;
        double rightPrefWidth = 0;
        double bottomPrefWidth = 0;

        if (getContentBias() == Orientation.VERTICAL) {
            double h[] = adjustAreaHeight(height, -1);

            topPrefWidth = getAreaWidth(getTop(), h[0], false);
            leftPrefWidth = getAreaWidth(getLeft(), h[1], false);
            centerPrefWidth = getAreaWidth(getCenter(), h[2], false);
            rightPrefWidth = getAreaWidth(getRight(), h[3], false);
            bottomPrefWidth = getAreaWidth(getBottom(), h[4], false);
        } else {
            double centerPrefHeight = getCenter() != null? computeChildPrefAreaHeight(getCenter(), getMargin(getCenter())) : 0;
            double leftPrefHeight = getLeft() != null? computeChildPrefAreaHeight(getLeft(), getMargin(getLeft())) : 0;
            double rightPrefHeight = getRight() != null? computeChildPrefAreaHeight(getRight(), getMargin(getRight())) : 0;

            double maxHeight = Math.max(centerPrefHeight, Math.max(rightPrefHeight, leftPrefHeight));

            leftPrefWidth = getLeft() != null? computeChildPrefAreaWidth(getLeft(), getMargin(getLeft()), maxHeight) : 0;
            rightPrefWidth = getRight() != null? computeChildPrefAreaWidth(getRight(), getMargin(getRight()), maxHeight) : 0;
            centerPrefWidth = getCenter() != null? computeChildPrefAreaWidth(getCenter(), getMargin(getCenter()), maxHeight) : 0;

            topPrefWidth = getTop() != null? computeChildPrefAreaWidth(getTop(), getMargin(getTop())) : 0;
            bottomPrefWidth = getBottom() != null? computeChildPrefAreaWidth(getBottom(), getMargin(getBottom())) : 0;
        }
        return getInsets().getLeft() +
                Math.max(leftPrefWidth + centerPrefWidth + rightPrefWidth, Math.max(topPrefWidth,bottomPrefWidth)) +
                getInsets().getRight();
    }

    @Override protected double computePrefHeight(double width) {
        double topPrefHeight = 0;
        double bottomPrefHeight = 0;
        double leftPrefHeight = 0;
        double centerPrefHeight = 0;
        double rightPrefHeight = 0;
        double maxHeight = 0;

        if (getContentBias() == Orientation.HORIZONTAL) {
            double w[] = adjustAreaWidth(width, -1);

            topPrefHeight = getAreaHeight(getTop(), width, false);
            leftPrefHeight = getAreaHeight(getLeft(), w[0], false);
            centerPrefHeight = getAreaHeight(getCenter(), w[1], false);
            rightPrefHeight = getAreaHeight(getRight(), w[2], false);
            bottomPrefHeight = getAreaHeight(getBottom(), width, false);

            maxHeight = Math.max(centerPrefHeight, Math.max(rightPrefHeight, leftPrefHeight));
        } else {
            centerPrefHeight = getCenter() != null? computeChildPrefAreaHeight(getCenter(), getMargin(getCenter())) : 0;
            leftPrefHeight = getLeft() != null? computeChildPrefAreaHeight(getLeft(), getMargin(getLeft())) : 0;
            rightPrefHeight = getRight() != null? computeChildPrefAreaHeight(getRight(), getMargin(getRight())) : 0;

            maxHeight = Math.max(centerPrefHeight, Math.max(rightPrefHeight, leftPrefHeight));

            double leftPrefWidth = getLeft() != null? computeChildPrefAreaWidth(getLeft(), getMargin(getLeft()), maxHeight) : 0;
            double rightPrefWidth = getRight() != null? computeChildPrefAreaWidth(getRight(), getMargin(getRight()), maxHeight) : 0;
            double centerPrefWidth = getCenter() != null? computeChildPrefAreaWidth(getCenter(), getMargin(getCenter()), maxHeight) : 0;

            double topPrefWidth = getTop() != null? computeChildPrefAreaWidth(getTop(), getMargin(getTop())) : 0;
            double bottomPrefWidth = getBottom() != null? computeChildPrefAreaWidth(getBottom(), getMargin(getBottom())) : 0;

            double prefWidth = getInsets().getLeft() +
                    Math.max(leftPrefWidth + centerPrefWidth + rightPrefWidth, Math.max(topPrefWidth,bottomPrefWidth)) +
                    getInsets().getRight();

            topPrefHeight = getTop() != null? computeChildPrefAreaHeight(getTop(), getMargin(getTop()), prefWidth) : 0;
            bottomPrefHeight = getBottom() != null? computeChildPrefAreaHeight(getBottom(), getMargin(getBottom()), prefWidth) : 0;
        }

        return getInsets().getTop() + topPrefHeight + maxHeight + bottomPrefHeight + getInsets().getBottom();
    }

    @Override protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        double insideX = getInsets().getLeft();
        double insideY = getInsets().getTop();
        double insideWidth = width - insideX - getInsets().getRight();
        double insideHeight = height - insideY - getInsets().getBottom();

        double widths[] = adjustAreaWidth(width, height);
        double heights[] = adjustAreaHeight(height, width);

        double topHeight = 0;
        Insets topMargin = null;
        if (getTop() != null) {
            topMargin = getNodeMargin(getTop());
            if (getContentBias() == Orientation.VERTICAL) {
                topHeight = heights[0] == -1 ? getTop().prefHeight(-1) : heights[0];
            } else {
                topHeight = snapSize(topMargin.getTop() +
                        getTop().prefHeight(insideWidth - topMargin.getLeft() - topMargin.getRight()) +
                        topMargin.getBottom());
            }
        }
        double bottomHeight = 0;
        Insets bottomMargin = null;
        if (getBottom() != null) {
            bottomMargin = getNodeMargin(getBottom());
            if (getContentBias() == Orientation.VERTICAL) {
                bottomHeight = heights[4] == -1 ? getBottom().prefHeight(-1) : heights[4];
            } else {
                bottomHeight = snapSize(bottomMargin.getTop() +
                        getBottom().prefHeight(insideWidth - bottomMargin.getLeft() - bottomMargin.getRight()) +
                        bottomMargin.getBottom());
            }
        }
        double leftWidth = 0;
        Insets leftMargin = null;
        if (getLeft() != null) {
            leftMargin = getNodeMargin(getLeft());
            if (getContentBias() == Orientation.HORIZONTAL) {
                leftWidth =  widths[0] == -1 ? getLeft().prefWidth(-1) : widths[0];
            } else {
                leftWidth = snapSize(leftMargin.getLeft() +
                    getLeft().prefWidth(insideHeight - topHeight - bottomHeight - leftMargin.getTop() - leftMargin.getBottom()) +
                    leftMargin.getRight());
            }
        }
        double rightWidth = 0;
        Insets rightMargin = null;
        if (getRight() != null) {
            rightMargin = getNodeMargin(getRight());
            if (getContentBias() == Orientation.HORIZONTAL) {
                rightWidth = widths[2] == -1 ? getRight().prefWidth(-1) : widths[2];
            } else {
                rightWidth = snapSize(rightMargin.getLeft() +
                        getRight().prefWidth(insideHeight - topHeight - bottomHeight - rightMargin.getTop() - rightMargin.getBottom()) +
                        rightMargin.getRight());
            }
        }

        if (getTop() != null) {
            Pos alignment = getAlignment(getTop());
            topHeight = Math.min(topHeight, insideHeight);
            layoutInArea(getTop(), insideX, insideY, insideWidth, topHeight, 0/*ignore baseline*/,
                    topMargin,
                    alignment != null? alignment.getHpos() : HPos.LEFT,
                    alignment != null? alignment.getVpos() : VPos.TOP);
        }
        if (getBottom() != null) {
            Pos alignment = getAlignment(getBottom());
            bottomHeight = Math.min(bottomHeight, insideHeight - topHeight);
            layoutInArea(getBottom(), insideX, insideY + insideHeight - bottomHeight,
                    insideWidth, bottomHeight, 0/*ignore baseline*/,
                    bottomMargin,
                    alignment != null? alignment.getHpos() : HPos.LEFT,
                    alignment != null? alignment.getVpos() : VPos.BOTTOM);
        }
        
        if (getLeft() != null) {
            Pos alignment = getAlignment(getLeft());
            leftWidth = Math.min(leftWidth, insideWidth);
            layoutInArea(getLeft(), insideX, insideY + topHeight,
                    leftWidth, insideHeight - topHeight - bottomHeight, 0/*ignore baseline*/,
                    leftMargin,
                    alignment != null? alignment.getHpos() : HPos.LEFT,
                    alignment != null? alignment.getVpos() : VPos.TOP);
        }
        if (getRight() != null) {
            Pos alignment = getAlignment(getRight());
            rightWidth = Math.min(rightWidth, insideWidth - leftWidth);
            layoutInArea(getRight(), insideX + insideWidth - rightWidth, insideY + topHeight,
                    rightWidth, insideHeight - topHeight - bottomHeight, 0/*ignore baseline*/,
                    rightMargin,
                    alignment != null? alignment.getHpos() : HPos.RIGHT,
                    alignment != null? alignment.getVpos() : VPos.TOP);
        }

        if (getCenter() != null) {
            Pos alignment = getAlignment(getCenter());
            layoutInArea(getCenter(), insideX + leftWidth, insideY + topHeight,
                    insideWidth - leftWidth - rightWidth,
                    insideHeight - topHeight - bottomHeight, 0/*ignore baseline*/,
                    getNodeMargin(getCenter()),
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
        double actualHeight[] = new double[5];
        actualHeight[0] = getAreaHeight(getTop(), -1, false);
        actualHeight[1] = getAreaHeight(getLeft(), -1, false);
        actualHeight[2] = getAreaHeight(getCenter(), -1, false);
        actualHeight[3] = getAreaHeight(getRight(), -1, false);
        actualHeight[4] = getAreaHeight(getBottom(), -1, false);

        double contentHeight = Math.max(actualHeight[1], Math.max(actualHeight[2], actualHeight[3]));
        contentHeight += (actualHeight[0] + actualHeight[4]);

        double extraHeight = height - contentHeight;
        boolean shrinking = extraHeight < 0;

        boolean contentBias[] = new boolean[5];
        contentBias[0] = childHasContentBias(getTop(), Orientation.VERTICAL);
        contentBias[1] = childHasContentBias(getLeft(), Orientation.VERTICAL);
        contentBias[2] = childHasContentBias(getCenter(), Orientation.VERTICAL);
        contentBias[3] = childHasContentBias(getRight(), Orientation.VERTICAL);
        contentBias[4] = childHasContentBias(getBottom(), Orientation.VERTICAL);

        double areaLimitHeight[] = new double[5];
        areaLimitHeight[0] = getAreaLimitHeight(getTop(), shrinking, width);
        areaLimitHeight[1] = getAreaLimitHeight(getLeft(), shrinking, width);
        areaLimitHeight[2] = getAreaLimitHeight(getCenter(), shrinking, width);
        areaLimitHeight[3] = getAreaLimitHeight(getRight(), shrinking, width);
        areaLimitHeight[4] = getAreaLimitHeight(getBottom(), shrinking, width);

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
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/


}
