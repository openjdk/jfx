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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import javafx.css.Styleable;

import static javafx.geometry.Orientation.*;
import javafx.util.Callback;

/**
 * FlowPane lays out its children in a flow that wraps at the flowpane's boundary.
 * <p>
 * A horizontal flowpane (the default) will layout nodes in rows, wrapping at the
 * flowpane's width.  A vertical flowpane lays out nodes in columns,
 * wrapping at the flowpane's height.  If the flowpane has a border and/or padding set,
 * the content will be flowed within those insets.
 * <p>
 * FlowPane's prefWrapLength property establishes its preferred width
 * (for horizontal) or preferred height (for vertical). Applications should set
 * prefWrapLength if the default value (400) doesn't suffice.  Note that prefWrapLength
 * is used only for calculating the preferred size and may not reflect the actual
 * wrapping dimension, which tracks the actual size of the flowpane.
 * <p>
 * The alignment property controls how the rows and columns are aligned
 * within the bounds of the flowpane and defaults to Pos.TOP_LEFT.  It is also possible
 * to control the alignment of nodes within the rows and columns by setting
 * rowValignment for horizontal or columnHalignment for vertical.
 * <p>
 * Example of a horizontal flowpane:
 * <pre>{@code
 *     Image images[] = { ... };
 *     FlowPane flow = new FlowPane();
 *     flow.setVgap(8);
 *     flow.setHgap(4);
 *     flow.setPrefWrapLength(300); // preferred width = 300
 *     for (int i = 0; i < images.length; i++) {
 *         flow.getChildren().add(new ImageView(image[i]);
 *     }
 * }</pre>
 *
 *<p>
 * Example of a vertical flowpane:
 * <pre>{@code
 *     FlowPane flow = new FlowPane(Orientation.VERTICAL);
 *     flow.setColumnHalignment(HPos.LEFT); // align labels on left
 *     flow.setPrefWrapLength(200); // preferred height = 200
 *     for (int i = 0; i < titles.size(); i++) {
 *         flow.getChildren().add(new Label(titles[i]);
 *     }
 * }</pre>
 *
 * <p>
 * FlowPane lays out each managed child regardless of the child's visible property value;
 * unmanaged children are ignored for all layout calculations.</p>
 *
 * <p>
 * FlowPane may be styled with backgrounds and borders using CSS.  See
 * {@link javafx.scene.layout.Region Region} superclass for details.</p>
 *
 * <h2>Resizable Range</h2>
 *
 * <p>
 * A flowpane's parent will resize the flowpane within the flowpane's resizable range
 * during layout.   By default the flowpane computes this range based on its content
 * as outlined in the tables below.
 * </p>
 * <table border="1">
 * <caption>Horizontal</caption>
 * <tr><td></td><th scope="col">width</th><th scope="col">height</th></tr>
 * <tr><th scope="row">minimum</th>
 * <td>left/right insets plus largest of children's pref widths</td>
 * <td>top/bottom insets plus height required to display all children at their preferred heights when wrapped at a specified width</td></tr>
 * <tr><th scope="row">preferred</th>
 * <td>left/right insets plus prefWrapLength</td>
 * <td>top/bottom insets plus height required to display all children at their pref heights when wrapped at a specified width</td></tr>
 * <tr><th scope="row">maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <br>
 * <table border="1">
 * <caption>Vertical</caption>
 * <tr><td></td><th scope="col">width</th><th scope="col">height</th></tr>
 * <tr><th scope="row">minimum</th>
 * <td>left/right insets plus width required to display all children at their preferred widths when wrapped at a specified height</td>
 * <td>top/bottom insets plus largest of children's pref heights</td></tr>
 * <tr><th scope="row">preferred</th>
 * <td>left/right insets plus width required to display all children at their pref widths when wrapped at the specified height</td>
 * <td>top/bottom insets plus prefWrapLength</td></tr>
 * <tr><th scope="row">maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * A flowpane's unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned to it.
 * <p>
 * FlowPane provides properties for setting the size range directly.  These
 * properties default to the sentinel value Region.USE_COMPUTED_SIZE, however the
 * application may set them to other values as needed:
 * <pre><code>
 *     <b>flowPane.setMaxWidth(500);</b>
 * </code></pre>
 * Applications may restore the computed values by setting these properties back
 * to Region.USE_COMPUTED_SIZE.
 * <p>
 * FlowPane does not clip its content by default, so it is possible that children's
 * bounds may extend outside its own bounds if a child's pref size is larger than
 * the space flowpane has to allocate for it.</p>
 *
 * @since JavaFX 2.0
 */
public class FlowPane extends Pane {

    /* ******************************************************************
     *  BEGIN static methods
     ********************************************************************/
    private static final String MARGIN_CONSTRAINT = "flowpane-margin";

    /**
     * Sets the margin for the child when contained by a flowpane.
     * If set, the flowpane will layout it out with the margin space around it.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a flowpane
     * @param value the margin of space around the child
     */
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN_CONSTRAINT, value);
    }

    /**
     * Returns the child's margin constraint if set.
     * @param child the child node of a flowpane
     * @return the margin for the child or null if no margin was set
     */
    public static Insets getMargin(Node child) {
        return (Insets)getConstraint(child, MARGIN_CONSTRAINT);
    }

    private static final Callback<Node, Insets> marginAccessor = n -> getMargin(n);

    /**
     * Removes all flowpane constraints from the child node.
     * @param child the child node
     */
    public static void clearConstraints(Node child) {
        setMargin(child, null);
    }

    /* ******************************************************************
     *  END static methods
     ********************************************************************/

    /**
     * Creates a horizontal FlowPane layout with hgap/vgap = 0.
     */
    public FlowPane() {
        super();
    }

    /**
     * Creates a FlowPane layout with the specified orientation and hgap/vgap = 0.
     * @param orientation the direction the tiles should flow &amp; wrap
     */
    public FlowPane(Orientation orientation) {
        this();
        setOrientation(orientation);
    }

    /**
     * Creates a horizontal FlowPane layout with the specified hgap/vgap.
     * @param hgap the amount of horizontal space between each tile
     * @param vgap the amount of vertical space between each tile
     */
    public FlowPane(double hgap, double vgap) {
        this();
        setHgap(hgap);
        setVgap(vgap);
    }

    /**
     * Creates a FlowPane layout with the specified orientation and hgap/vgap.
     * @param orientation the direction the tiles should flow &amp; wrap
     * @param hgap the amount of horizontal space between each tile
     * @param vgap the amount of vertical space between each tile
     */
    public FlowPane(Orientation orientation, double hgap, double vgap) {
        this();
        setOrientation(orientation);
        setHgap(hgap);
        setVgap(vgap);
    }

    /**
     * Creates a horizontal FlowPane layout with hgap/vgap = 0.
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public FlowPane(Node... children) {
        super();
        getChildren().addAll(children);
    }

    /**
     * Creates a FlowPane layout with the specified orientation and hgap/vgap = 0.
     * @param orientation the direction the tiles should flow &amp; wrap
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public FlowPane(Orientation orientation, Node... children) {
        this();
        setOrientation(orientation);
        getChildren().addAll(children);
    }

    /**
     * Creates a horizontal FlowPane layout with the specified hgap/vgap.
     * @param hgap the amount of horizontal space between each tile
     * @param vgap the amount of vertical space between each tile
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public FlowPane(double hgap, double vgap, Node... children) {
        this();
        setHgap(hgap);
        setVgap(vgap);
        getChildren().addAll(children);
    }

    /**
     * Creates a FlowPane layout with the specified orientation and hgap/vgap.
     * @param orientation the direction the tiles should flow &amp; wrap
     * @param hgap the amount of horizontal space between each tile
     * @param vgap the amount of vertical space between each tile
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public FlowPane(Orientation orientation, double hgap, double vgap, Node... children) {
        this();
        setOrientation(orientation);
        setHgap(hgap);
        setVgap(vgap);
        getChildren().addAll(children);
    }

    /**
     * The orientation of this flowpane.
     * A horizontal flowpane lays out children left to right, wrapping at the
     * flowpane's width boundary.   A vertical flowpane lays out children top to
     * bottom, wrapping at the flowpane's height.
     * The default is horizontal.
     * @return the orientation of this flowpane
     */
    public final ObjectProperty<Orientation> orientationProperty() {
        if (orientation == null) {
            orientation = new StyleableObjectProperty(HORIZONTAL) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<FlowPane, Orientation> getCssMetaData() {
                    return StyleableProperties.ORIENTATION;
                }

                @Override
                public Object getBean() {
                    return FlowPane.this;
                }

                @Override
                public String getName() {
                    return "orientation";
                }
            };
        }
        return orientation;
    }

    private ObjectProperty<Orientation> orientation;
    public final void setOrientation(Orientation value) { orientationProperty().set(value); }
    public final Orientation getOrientation() { return orientation == null ? HORIZONTAL : orientation.get();  }

    /**
     * The amount of horizontal space between each node in a horizontal flowpane
     * or the space between columns in a vertical flowpane.
     * @return the amount of horizontal space between each node in a horizontal
     * flowpane or the space between columns in a vertical flowpane
     */
    public final DoubleProperty hgapProperty() {
        if (hgap == null) {
            hgap = new StyleableDoubleProperty() {

                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<FlowPane, Number> getCssMetaData() {
                    return StyleableProperties.HGAP;
                }

                @Override
                public Object getBean() {
                    return FlowPane.this;
                }

                @Override
                public String getName() {
                    return "hgap";
                }
            };
        }
        return hgap;
    }

    private DoubleProperty hgap;
    public final void setHgap(double value) { hgapProperty().set(value); }
    public final double getHgap() { return hgap == null ? 0 : hgap.get(); }

    /**
     * The amount of vertical space between each node in a vertical flowpane
     * or the space between rows in a horizontal flowpane.
     * @return the amount of vertical space between each node in a vertical
     * flowpane or the space between rows in a horizontal flowpane
     */
    public final DoubleProperty vgapProperty() {
        if (vgap == null) {
            vgap = new StyleableDoubleProperty() {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<FlowPane, Number> getCssMetaData() {
                    return StyleableProperties.VGAP;
                }

                @Override
                public Object getBean() {
                    return FlowPane.this;
                }

                @Override
                public String getName() {
                    return "vgap";
                }
            };
        }
        return vgap;
    }

    private DoubleProperty vgap;
    public final void setVgap(double value) { vgapProperty().set(value); }
    public final double getVgap() { return vgap == null ? 0 : vgap.get(); }


    /**
     * The preferred width where content should wrap in a horizontal flowpane or
     * the preferred height where content should wrap in a vertical flowpane.
     * <p>
     * This value is used only to compute the preferred size of the flowpane and may
     * not reflect the actual width or height, which may change if the flowpane is
     * resized to something other than its preferred size.
     * <p>
     * Applications should initialize this value to define a reasonable span
     * for wrapping the content.
     *
     * @return the preferred width where content should wrap in a horizontal
     * flowpane or the preferred height where content should wrap in a vertical
     * flowpane
     */
    public final DoubleProperty prefWrapLengthProperty() {
        if (prefWrapLength == null) {
            prefWrapLength = new DoublePropertyBase(400) {
                @Override
                protected void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return FlowPane.this;
                }

                @Override
                public String getName() {
                    return "prefWrapLength";
                }
            };
        }
        return prefWrapLength;
    }
    private DoubleProperty prefWrapLength;
    public final void setPrefWrapLength(double value) { prefWrapLengthProperty().set(value); }
    public final double getPrefWrapLength() { return prefWrapLength == null ? 400 : prefWrapLength.get(); }


    /**
     * The overall alignment of the flowpane's content within its width and height.
     * <p>For a horizontal flowpane, each row will be aligned within the flowpane's width
     * using the alignment's hpos value, and the rows will be aligned within the
     * flowpane's height using the alignment's vpos value.
     * <p>For a vertical flowpane, each column will be aligned within the flowpane's height
     * using the alignment's vpos value, and the columns will be aligned within the
     * flowpane's width using the alignment's hpos value.
     * @return the overall alignment of the flowpane's content within its width
     * and height
     */
    public final ObjectProperty<Pos> alignmentProperty() {
        if (alignment == null) {
            alignment = new StyleableObjectProperty<Pos>(Pos.TOP_LEFT) {

                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<FlowPane, Pos> getCssMetaData() {
                    return StyleableProperties.ALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return FlowPane.this;
                }

                @Override
                public String getName() {
                    return "alignment";
                }
            };
        }
        return alignment;
    }

    private ObjectProperty<Pos> alignment;
    public final void setAlignment(Pos value) { alignmentProperty().set(value); }
    public final Pos getAlignment() { return alignment == null ? Pos.TOP_LEFT : alignment.get(); }
    private Pos getAlignmentInternal() {
        Pos localPos = getAlignment();
        return localPos == null ? Pos.TOP_LEFT : localPos;
    }

    /**
     * The horizontal alignment of nodes within each column of a vertical flowpane.
     * The property is ignored for horizontal flowpanes.
     * @return the horizontal alignment of nodes within each column of a
     * vertical flowpane
     */
    public final ObjectProperty<HPos> columnHalignmentProperty() {
        if (columnHalignment == null) {
            columnHalignment = new StyleableObjectProperty<HPos>(HPos.LEFT) {

                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<FlowPane, HPos> getCssMetaData() {
                    return StyleableProperties.COLUMN_HALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return FlowPane.this;
                }

                @Override
                public String getName() {
                    return "columnHalignment";
                }
            };
        }
        return columnHalignment;
    }

    private ObjectProperty<HPos> columnHalignment;
    public final void setColumnHalignment(HPos value) { columnHalignmentProperty().set(value); }
    public final HPos getColumnHalignment() { return columnHalignment == null ? HPos.LEFT : columnHalignment.get(); }
    private HPos getColumnHalignmentInternal() {
        HPos localPos = getColumnHalignment();
        return localPos == null ? HPos.LEFT : localPos;
    }

    /**
     * The vertical alignment of nodes within each row of a horizontal flowpane.
     * If this property is set to VPos.BASELINE, then the flowpane will always
     * resize children to their preferred heights, rather than expanding heights
     * to fill the row height.
     * The property is ignored for vertical flowpanes.
     * @return the vertical alignment of nodes within each row of a horizontal
     * flowpane
     */
    public final ObjectProperty<VPos> rowValignmentProperty() {
        if (rowValignment == null) {
            rowValignment = new StyleableObjectProperty<VPos>(VPos.CENTER) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<FlowPane, VPos> getCssMetaData() {
                    return StyleableProperties.ROW_VALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return FlowPane.this;
                }

                @Override
                public String getName() {
                    return "rowValignment";
                }
            };
        }
        return rowValignment;
    }

    private ObjectProperty<VPos> rowValignment;
    public final void setRowValignment(VPos value) { rowValignmentProperty().set(value); }
    public final VPos getRowValignment() { return rowValignment == null ? VPos.CENTER : rowValignment.get(); }
    private VPos getRowValignmentInternal() {
        VPos localPos =  getRowValignment();
        return localPos == null ? VPos.CENTER : localPos;
    }

    @Override public Orientation getContentBias() {
        return getOrientation();
    }

    @Override protected double computeMinWidth(double height) {
        if (getContentBias() == HORIZONTAL) {
            double maxPref = 0;
            final List<Node> children = getChildren();
            for (int i=0, size=children.size(); i<size; i++) {
                Node child = children.get(i);
                if (child.isManaged()) {
                    maxPref = Math.max(maxPref, child.prefWidth(-1));
                }
            }
            final Insets insets = getInsets();
            return insets.getLeft() + snapSizeX(maxPref) + insets.getRight();
        }
        return computePrefWidth(height);
    }

    @Override protected double computeMinHeight(double width) {
        if (getContentBias() == VERTICAL) {
            double maxPref = 0;
            final List<Node> children = getChildren();
            for (int i=0, size=children.size(); i<size; i++) {
                Node child = children.get(i);
                if (child.isManaged()) {
                    maxPref = Math.max(maxPref, child.prefHeight(-1));
                }
            }
            final Insets insets = getInsets();
            return insets.getTop() + snapSizeY(maxPref) + insets.getBottom();
        }
        return computePrefHeight(width);
    }

    @Override protected double computePrefWidth(double forHeight) {
        final Insets insets = getInsets();
        if (getOrientation() == HORIZONTAL) {
            // horizontal
            double maxRunWidth = getPrefWrapLength();
            List<Run> hruns = getRuns(maxRunWidth);
            double w = computeContentWidth(hruns);
            w = getPrefWrapLength() > w ? getPrefWrapLength() : w;
            return insets.getLeft() + snapSizeX(w) + insets.getRight();
        } else {
            // vertical
            double maxRunHeight = forHeight != -1?
                forHeight - insets.getTop() - insets.getBottom() : getPrefWrapLength();
            List<Run> vruns = getRuns(maxRunHeight);
            return insets.getLeft() + computeContentWidth(vruns) + insets.getRight();
        }
    }

    @Override protected double computePrefHeight(double forWidth) {
        final Insets insets = getInsets();
        if (getOrientation() == HORIZONTAL) {
            // horizontal
            double maxRunWidth = forWidth != -1?
                forWidth - insets.getLeft() - insets.getRight() : getPrefWrapLength();
            List<Run> hruns = getRuns(maxRunWidth);
            return insets.getTop() + computeContentHeight(hruns) + insets.getBottom();
        } else {
            // vertical
            double maxRunHeight = getPrefWrapLength();
            List<Run> vruns = getRuns(maxRunHeight);
            double h = computeContentHeight(vruns);
            h = getPrefWrapLength() > h ? getPrefWrapLength() : h;
            return insets.getTop() + snapSizeY(h) + insets.getBottom();
        }
    }

    @Override public void requestLayout() {
        if (!computingRuns) {
            runs = null;
        }
        super.requestLayout();
    }

    private List<Run> runs = null;
    private double lastMaxRunLength = -1;
    boolean computingRuns = false;

    private List<Run> getRuns(double maxRunLength) {
        if (runs == null || maxRunLength != lastMaxRunLength) {
            computingRuns = true;
            lastMaxRunLength = maxRunLength;
            runs = new ArrayList();
            double runLength = 0;
            double runOffset = 0;
            Run run = new Run();
            double vgap = snapSpaceY(this.getVgap());
            double hgap = snapSpaceX(this.getHgap());

            final List<Node> children = getChildren();
            for (int i=0, size=children.size(); i<size; i++) {
                Node child = children.get(i);
                if (child.isManaged()) {
                    LayoutRect nodeRect = new LayoutRect();
                    nodeRect.node = child;
                    Insets margin = getMargin(child);
                    nodeRect.width = computeChildPrefAreaWidth(child, margin);
                    nodeRect.height = computeChildPrefAreaHeight(child, margin);
                    double nodeLength = getOrientation() == HORIZONTAL ? nodeRect.width : nodeRect.height;
                    if (runLength + nodeLength > maxRunLength && runLength > 0) {
                        // wrap to next run *unless* its the only node in the run
                        normalizeRun(run, runOffset);
                        if (getOrientation() == HORIZONTAL) {
                            // horizontal
                            runOffset += run.height + vgap;
                        } else {
                            // vertical
                            runOffset += run.width + hgap;
                        }
                        runs.add(run);
                        runLength = 0;
                        run = new Run();
                    }
                    if (getOrientation() == HORIZONTAL) {
                        // horizontal
                        nodeRect.x = runLength;
                        runLength += nodeRect.width + hgap;
                    } else {
                        // vertical
                        nodeRect.y = runLength;
                        runLength += nodeRect.height + vgap;
                    }
                    run.rects.add(nodeRect);
                }

            }
            // insert last run
            normalizeRun(run, runOffset);
            runs.add(run);
            computingRuns = false;
        }
        return runs;
    }

    private void normalizeRun(final Run run, double runOffset) {
        if (getOrientation() == HORIZONTAL) {
            // horizontal
            ArrayList<Node> rownodes = new ArrayList();
            run.width = (run.rects.size()-1)*snapSpaceX(getHgap());
            for (int i=0, max=run.rects.size(); i<max; i++) {
                LayoutRect lrect = run.rects.get(i);
                rownodes.add(lrect.node);
                run.width += lrect.width;
                lrect.y = runOffset;
            }
            run.height = computeMaxPrefAreaHeight(rownodes, marginAccessor, getRowValignment());
            run.baselineOffset = getRowValignment() == VPos.BASELINE?
                    getAreaBaselineOffset(rownodes, marginAccessor, i -> run.rects.get(i).width, run.height, true) : 0;

        } else {
            // vertical
            run.height = (run.rects.size()-1)*snapSpaceY(getVgap());
            double maxw = 0;
            for (int i=0, max=run.rects.size(); i<max; i++) {
                LayoutRect lrect = run.rects.get(i);
                run.height += lrect.height;
                lrect.x = runOffset;
                maxw = Math.max(maxw, lrect.width);
            }

            run.width = maxw;
            run.baselineOffset = run.height;
        }
    }

    private double computeContentWidth(List<Run> runs) {
        double cwidth = getOrientation() == HORIZONTAL ? 0 : (runs.size()-1)*snapSpaceX(getHgap());
        for (int i=0, max=runs.size(); i<max; i++) {
            Run run = runs.get(i);
            if (getOrientation() == HORIZONTAL) {
                cwidth = Math.max(cwidth, run.width);
            } else {
                // vertical
                cwidth += run.width;
            }
        }
        return cwidth;
    }

    private double computeContentHeight(List<Run> runs) {
        double cheight = getOrientation() == VERTICAL ? 0 : (runs.size()-1)*snapSpaceY(getVgap());
        for (int i=0, max=runs.size(); i<max; i++) {
            Run run = runs.get(i);
            if (getOrientation() == VERTICAL) {
                cheight = Math.max(cheight, run.height);
            } else {
                // horizontal
                cheight += run.height;
            }
        }
        return cheight;
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        final double width = getWidth();
        final double height = getHeight();
        final double top = insets.getTop();
        final double left = insets.getLeft();
        final double bottom = insets.getBottom();
        final double right = insets.getRight();
        final double insideWidth = width - left - right;
        final double insideHeight = height - top - bottom;

        //REMIND(aim): need to figure out how to cache the runs to avoid over-calculation
        final List<Run> runs = getRuns(getOrientation() == HORIZONTAL ? insideWidth : insideHeight);

        // Now that the nodes are broken into runs, figure out alignments
        for (int i=0, max=runs.size(); i<max; i++) {
            final Run run = runs.get(i);
            final double xoffset = left + computeXOffset(insideWidth,
                                     getOrientation() == HORIZONTAL ? run.width : computeContentWidth(runs),
                                     getAlignmentInternal().getHpos());
            final double yoffset = top + computeYOffset(insideHeight,
                                    getOrientation() == VERTICAL ? run.height : computeContentHeight(runs),
                                    getAlignmentInternal().getVpos());
            for (int j = 0; j < run.rects.size(); j++) {
                final LayoutRect lrect = run.rects.get(j);
//              System.out.println("flowpane.layout: run="+i+" "+run.width+"x"+run.height+" xoffset="+xoffset+" yoffset="+yoffset+" lrect="+lrect);
                final double x = xoffset + lrect.x;
                final double y = yoffset + lrect.y;
                layoutInArea(lrect.node, x, y,
                           getOrientation() == HORIZONTAL? lrect.width : run.width,
                           getOrientation() == VERTICAL? lrect.height : run.height,
                           run.baselineOffset, getMargin(lrect.node),
                           getColumnHalignmentInternal(), getRowValignmentInternal());
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/


     /*
      * Super-lazy instantiation pattern from Bill Pugh.
      */
     private static class StyleableProperties {

         private static final CssMetaData<FlowPane,Pos> ALIGNMENT =
             new CssMetaData<>("-fx-alignment",
                 new EnumConverter<>(Pos.class), Pos.TOP_LEFT) {

            @Override
            public boolean isSettable(FlowPane node) {
                return node.alignment == null || !node.alignment.isBound();
            }

            @Override
            public StyleableProperty<Pos> getStyleableProperty(FlowPane node) {
                return (StyleableProperty<Pos>)node.alignmentProperty();
            }

         };

         private static final CssMetaData<FlowPane,HPos> COLUMN_HALIGNMENT =
             new CssMetaData<>("-fx-column-halignment",
                 new EnumConverter<>(HPos.class), HPos.LEFT) {

            @Override
            public boolean isSettable(FlowPane node) {
                return node.columnHalignment == null || !node.columnHalignment.isBound();
            }

            @Override
            public StyleableProperty<HPos> getStyleableProperty(FlowPane node) {
                return (StyleableProperty<HPos>)node.columnHalignmentProperty();
            }

         };

         private static final CssMetaData<FlowPane,Number> HGAP =
             new CssMetaData<>("-fx-hgap",
                 SizeConverter.getInstance(), 0.0){

            @Override
            public boolean isSettable(FlowPane node) {
                return node.hgap == null || !node.hgap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(FlowPane node) {
                return (StyleableProperty<Number>)node.hgapProperty();
            }

         };

         private static final CssMetaData<FlowPane,VPos> ROW_VALIGNMENT =
             new CssMetaData<>("-fx-row-valignment",
                 new EnumConverter<>(VPos.class), VPos.CENTER) {

            @Override
            public boolean isSettable(FlowPane node) {
                return node.rowValignment == null || !node.rowValignment.isBound();
            }

            @Override
            public StyleableProperty<VPos> getStyleableProperty(FlowPane node) {
                return (StyleableProperty<VPos>)node.rowValignmentProperty();
            }

         };

         private static final CssMetaData<FlowPane,Orientation> ORIENTATION =
             new CssMetaData<>("-fx-orientation",
                 new EnumConverter<>(Orientation.class),
                 Orientation.HORIZONTAL) {

            @Override
            public Orientation getInitialValue(FlowPane node) {
                // A vertical flow pane should remain vertical
                return node.getOrientation();
            }

            @Override
            public boolean isSettable(FlowPane node) {
                return node.orientation == null || !node.orientation.isBound();
            }

            @Override
            public StyleableProperty<Orientation> getStyleableProperty(FlowPane node) {
                return (StyleableProperty<Orientation>)node.orientationProperty();
            }

         };

         private static final CssMetaData<FlowPane,Number> VGAP =
             new CssMetaData<>("-fx-vgap",
                 SizeConverter.getInstance(), 0.0){

            @Override
            public boolean isSettable(FlowPane node) {
                return node.vgap == null || !node.vgap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(FlowPane node) {
                return (StyleableProperty<Number>)node.vgapProperty();
            }

         };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Region.getClassCssMetaData());
            styleables.add(ALIGNMENT);
            styleables.add(COLUMN_HALIGNMENT);
            styleables.add(HGAP);
            styleables.add(ROW_VALIGNMENT);
            styleables.add(ORIENTATION);
            styleables.add(VGAP);

            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }


    /**
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     *
     * @since JavaFX 8.0
     */


    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    //REMIND(aim); replace when we get mutable rects
    private static class LayoutRect {
        public Node node;
        double x;
        double y;
        double width;
        double height;

        @Override public String toString() {
            return "LayoutRect node id="+node.getId()+" "+x+","+y+" "+width+"x"+height;
        }
    }

    private static class Run {
        ArrayList<LayoutRect> rects = new ArrayList();
        double width;
        double height;
        double baselineOffset;
    }
}
