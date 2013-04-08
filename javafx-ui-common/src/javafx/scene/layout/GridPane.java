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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import com.sun.javafx.collections.TrackableObservableList;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.CssMetaData;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;



/**
 * GridPane lays out its children within a flexible grid of rows and columns.
 * If a border and/or padding is set, then its content will be layed out within
 * those insets.
 * <p>
 * A child may be placed anywhere within the grid and may span multiple
 * rows/columns.  Children may freely overlap within rows/columns and their
 * stacking order will be defined by the order of the gridpane's children list
 * (0th node in back, last node in front).
 * <p>
 * GridPane may be styled with backgrounds and borders using CSS.  See
 * {@link javafx.scene.layout.Region Region} superclass for details.</p>
 *
 * <h4>Grid Constraints</h4>
 * <p>
 * A child's placement within the grid is defined by it's layout constraints:
 * <p>
 * <table border="1">
 * <tr><th>Constraint</th><th>Type</th><th>Description</th></tr>
 * <tr><td>columnIndex</td><td>integer</td><td>column where child's layout area starts.</td></tr>
 * <tr><td>rowIndex</td><td>integer</td><td>row where child's layout area starts.</td></tr>
 * <tr><td>columnSpan</td><td>integer</td><td>the number of columns the child's layout area spans horizontally.</td></tr>
 * <tr><td>rowSpan</td><td>integer</td><td>the number of rows the child's layout area spans vertically.</td></tr>
 * </table>
 * <p>
 * If the row/column indices are not explicitly set, then the child will be placed
 * in the first row/column.  If row/column spans are not set, they will default to 1.
 * A child's placement constraints can be changed dynamically and the gridpane
 * will update accordingly.
 * <p>
 * The total number of rows/columns does not need to be specified up front as the
 * gridpane will automatically expand/contract the grid to accommodate the content.
 * <p>
 * To use the GridPane, an application needs to set the layout constraints on
 * the children and add those children to the gridpane instance.
 * Constraints are set on the children using static setter methods on the GridPane
 * class:
 * <pre><code>     GridPane gridpane = new GridPane();
 *
 *     // Set one constraint at a time...
 *     Button button = new Button();
 *     <b>GridPane.setRowIndex(button, 1);
 *     GridPane.setColumnIndex(button, 2);</b>
 *
 *     // or convenience methods set more than one constraint at once...
 *     Label label = new Label();
 *     <b>GridPane.setConstraints(label, 3, 1);</b> // column=3 row=1
 *
 *     // don't forget to add children to gridpane
 *     <b>gridpane.getChildren().addAll(button, label);</b>
 * </code></pre>
 *
 * Applications may also use convenience methods which combine the steps of
 * setting the constraints and adding the children:
 * <pre><code>
 *     GridPane gridpane = new GridPane();
 *     <b>gridpane.add(new Button(), 2, 1);</b> // column=2 row=1
 *     <b>gridpane.add(new Label(), 3, 1);</b>  // column=3 row=1
 * </code></pre>
 *
 *
 * <h4>Row/Column Sizing</h4>
 *
 * By default, rows and columns will be sized to fit their content;
 * a column will be wide enough to accommodate the widest child, a
 * row tall enough to fit the tallest child.   However, if an application needs
 * to explicitly control the size of rows or columns, it may do so by adding
 * RowConstraints and ColumnConstraints objects to specify those metrics.
 * For example, to create a grid with two fixed-width columns:
 * <pre><code>
 *     GridPane gridpane = new GridPane();
 *     <b>gridpane.getColumnConstraints().add(new ColumnConstraints(100));</b> // column 1 is 100 wide
 *     <b>gridpane.getColumnConstraints().add(new ColumnConstraints(200));</b> // column 2 is 200 wide
 * </code></pre>
 * By default the gridpane will resize rows/columns to their preferred sizes (either
 * computed from content or fixed), even if the gridpane is resized larger than
 * its preferred size.   If an application needs a particular row or column to
 * grow if there is extra space, it may set its grow priority on the RowConstraints
 * or ColumnConstraints object.  For example:
 * <pre><code>
 *     GridPane gridpane = new GridPane();
 *     ColumnConstraints column1 = new ColumnConstraints(100,100,Double.MAX_VALUE);
 *     <b>column1.setHgrow(Priority.ALWAYS);</b>
 *     ColumnConstraints column2 = new ColumnConstraints(100);
 *     gridpane.getColumnConstraints().addAll(column1, column2); // first column gets any extra width
 * </code></pre>
 *
 *
 * <h4>Percentage Sizing</h4>
 *
 * Alternatively, RowConstraints and ColumnConstraints allow the size to be specified
 * as a percentage of gridpane's available space:
 * <pre><code>
 *     GridPane gridpane = new GridPane();
 *     ColumnConstraints column1 = new ColumnConstraints();
 *     <b>column1.setPercentWidth(50);</b>
 *     ColumnConstraints column2 = new ColumnConstraints();
 *     <b>column2.setPercentWidth(50);</b>
 *     gridpane.getColumnConstraints().addAll(column1, column2); // each get 50% of width
 * </code></pre>
 * If a percentage value is set on a row/column, then that value takes precedent and the
 * row/column's min, pref, max, and grow constraints will be ignored.
 * <p>
 * Note that if the sum of the widthPercent (or heightPercent) values total greater than 100, the values will
 * be treated as weights.  e.g.  if 3 columns are each given a widthPercent of 50,
 * then each will be allocated 1/3 of the gridpane's available width (50/(50+50+50)).
 *
 * <h4>Mixing Size Types</h4>
 *
 * An application may freely mix the size-types of rows/columns (computed from content, fixed,
 * or percentage).  The percentage rows/columns will always be allocated space first
 * based on their percentage of the gridpane's available space (size minus insets and gaps).
 * The remaining space will be allocated to rows/columns given their minimum, preferred,
 * and maximum sizes and grow priorities.
 *
 * <h4>Resizable Range</h4>
 * A gridpane's parent will resize the gridpane within the gridpane's resizable range
 * during layout.   By default the gridpane computes this range based on its content
 * and row/column constraints as outlined in the table below.
 * <p>
 * <table border="1">
 * <tr><td></td><th>width</th><th>height</th></tr>
 * <tr><th>minimum</th>
 * <td>left/right insets plus the sum of each column's min width.</td>
 * <td>top/bottom insets plus the sum of each row's min height.</td></tr>
 * <tr><th>preferred</th>
 * <td>left/right insets plus the sum of each column's pref width.</td>
 * <td>top/bottom insets plus the sum of each row's pref height.</td></tr>
 * <tr><th>maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * A gridpane's unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned
 * to it.
 * <p>
 * GridPane provides properties for setting the size range directly.  These
 * properties default to the sentinel value USE_COMPUTED_SIZE, however the
 * application may set them to other values as needed:
 * <pre><code>     <b>gridpane.setPrefSize(300, 300);</b>
 *     // never size the gridpane larger than its preferred size:
 *     <b>gridpane.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);</b>
 * </code></pre>
 * Applications may restore the computed values by setting these properties back
 * to USE_COMPUTED_SIZE.
 * <p>
 * GridPane does not clip its content by default, so it is possible that childrens'
 * bounds may extend outside its own bounds if a child's min size prevents it from
 * being fit within it space.</p>
 *
 * <h4>Optional Layout Constraints</h4>
 *
 * An application may set additional constraints on children to customize how the
 * child is sized and positioned within the layout area established by it's row/column
 * indices/spans:
 * <p>
 * <table border="1">
 * <tr><th>Constraint</th><th>Type</th><th>Description</th></tr>
 * <tr><td>halignment</td><td>javafx.geometry.HPos</td><td>The horizontal alignment of the child within its layout area.</td></tr>
 * <tr><td>valignment</td><td>javafx.geometry.VPos</td><td>The vertical alignment of the child within its layout area.</td></tr>
 * <tr><td>hgrow</td><td>javafx.scene.layout.Priority</td><td>The horizontal grow priority of the child.</td></tr>
 * <tr><td>vgrow</td><td>javafx.scene.layout.Priority</td><td>The vertical grow priority of the child.</td></tr>
 * <tr><td>margin</td><td>javafx.geometry.Insets</td><td>Margin space around the outside of the child.</td></tr>
 * </table>
 * <p>
 * By default the alignment of a child within its layout area is defined by the
 * alignment set for the row and column.  If an individual alignment constraint is
 * set on a child, that alignment will override the row/column alignment only
 * for that child.  Alignment of other children in the same row or column will
 * not be affected.
 * <p>
 * Grow priorities, on the other hand, can only be applied to entire rows or columns.
 * Therefore, if a grow priority constraint is set on a single child, it will be
 * used to compute the default grow priority of the encompassing row/column.  If
 * a grow priority is set directly on a RowConstraint or ColumnConstraint object,
 * it will override the value computed from content.
 *
 *
 */
public class GridPane extends Pane {

    /**
     * Sentinel value which may be set on a child's row/column span constraint to
     * indicate that it should span the remaining rows/columns.
     */
    public static final int REMAINING = Integer.MAX_VALUE;

    /********************************************************************
     *  BEGIN static methods
     ********************************************************************/
    private static final String MARGIN_CONSTRAINT = "gridpane-margin";
    private static final String HALIGNMENT_CONSTRAINT = "gridpane-halignment";
    private static final String VALIGNMENT_CONSTRAINT = "gridpane-valignment";
    private static final String HGROW_CONSTRAINT = "gridpane-hgrow";
    private static final String VGROW_CONSTRAINT = "gridpane-vgrow";
    private static final String ROW_INDEX_CONSTRAINT = "gridpane-row";
    private static final String COLUMN_INDEX_CONSTRAINT = "gridpane-column";
    private static final String ROW_SPAN_CONSTRAINT = "gridpane-row-span";
    private static final String COLUMN_SPAN_CONSTRAINT = "gridpane-column-span";

    /**
     * Sets the row index for the child when contained by a gridpane
     * so that it will be positioned starting in that row of the gridpane.
     * If a gridpane child has no row index set, it will be positioned in the
     * first row.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a gridpane
     * @param value the row index of the child
     */
    public static void setRowIndex(Node child, Integer value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("rowIndex must be greater or equal to 0, but was "+value);
        }
        setConstraint(child, ROW_INDEX_CONSTRAINT, value);
    }

    /**
     * Returns the child's row index constraint if set.
     * @param child the child node of a gridpane
     * @return the row index for the child or null if no row index was set
     */
    public static Integer getRowIndex(Node child) {
        return (Integer)getConstraint(child, ROW_INDEX_CONSTRAINT);
    }

    /**
     * Sets the column index for the child when contained by a gridpane
     * so that it will be positioned starting in that column of the gridpane.
     * If a gridpane child has no column index set, it will be positioned in
     * the first column.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a gridpane
     * @param value the column index of the child
     */
    public static void setColumnIndex(Node child, Integer value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("columnIndex must be greater or equal to 0, but was "+value);
        }
        setConstraint(child, COLUMN_INDEX_CONSTRAINT, value);
    }

    /**
     * Returns the child's column index constraint if set.
     * @param child the child node of a gridpane
     * @return the column index for the child or null if no column index was set
     */
    public static Integer getColumnIndex(Node child) {
        return (Integer)getConstraint(child, COLUMN_INDEX_CONSTRAINT);
    }

    /**
     * Sets the row span for the child when contained by a gridpane
     * so that it will span that number of rows vertically.  This may be
     * set to REMAINING, which will cause the span to extend across all the remaining
     * rows.
     * <p>
     * If a gridpane child has no row span set, it will default to spanning one row.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a gridpane
     * @param value the row span of the child
     */
    public static void setRowSpan(Node child, Integer value) {
        if (value != null && value < 1) {
            throw new IllegalArgumentException("rowSpan must be greater or equal to 1, but was "+value);
        }
        setConstraint(child, ROW_SPAN_CONSTRAINT, value);
    }

    /**
     * Returns the child's row-span constraint if set.
     * @param child the child node of a gridpane
     * @return the row span for the child or null if no row span was set
     */
    public static Integer getRowSpan(Node child) {
        return (Integer)getConstraint(child, ROW_SPAN_CONSTRAINT);
    }

    /**
     * Sets the column span for the child when contained by a gridpane
     * so that it will span that number of columns horizontally.   This may be
     * set to REMAINING, which will cause the span to extend across all the remaining
     * columns.
     * <p>
     * If a gridpane child has no column span set, it will default to spanning one column.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a gridpane
     * @param value the column span of the child
     */
    public static void setColumnSpan(Node child, Integer value) {
        if (value != null && value < 1) {
            throw new IllegalArgumentException("columnSpan must be greater or equal to 1, but was "+value);
        }
        setConstraint(child, COLUMN_SPAN_CONSTRAINT, value);
    }

    /**
     * Returns the child's column-span constraint if set.
     * @param child the child node of a gridpane
     * @return the column span for the child or null if no column span was set
     */
    public static Integer getColumnSpan(Node child) {
        return (Integer)getConstraint(child, COLUMN_SPAN_CONSTRAINT);
    }

    /**
     * Sets the margin for the child when contained by a gridpane.
     * If set, the gridpane will lay it out with the margin space around it.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a gridpane
     * @param value the margin of space around the child
     */
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN_CONSTRAINT, value);
    }

    /**
     * Returns the child's margin constraint if set.
     * @param child the child node of a gridpane
     * @return the margin for the child or null if no margin was set
     */
    public static Insets getMargin(Node child) {
        return (Insets)getConstraint(child, MARGIN_CONSTRAINT);
    }

    /**
     * Sets the horizontal alignment for the child when contained by a gridpane.
     * If set, will override the gridpane's default horizontal alignment.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a gridpane
     * @param value the hozizontal alignment for the child
     */
    public static void setHalignment(Node child, HPos value) {
        setConstraint(child, HALIGNMENT_CONSTRAINT, value);
    }

    /**
     * Returns the child's halignment constraint if set.
     * @param child the child node of a gridpane
     * @return the horizontal alignment for the child or null if no alignment was set
     */
    public static HPos getHalignment(Node child) {
        return (HPos)getConstraint(child, HALIGNMENT_CONSTRAINT);
    }

    /**
     * Sets the vertical alignment for the child when contained by a gridpane.
     * If set, will override the gridpane's default vertical alignment.
     * Setting the value to null will remove the constraint.
     * @param child the child node of a gridpane
     * @param value the vertical alignment for the child
     */
    public static void setValignment(Node child, VPos value) {
        setConstraint(child, VALIGNMENT_CONSTRAINT, value);
    }

    /**
     * Returns the child's valignment constraint if set.
     * @param child the child node of a gridpane
     * @return the vertical alignment for the child or null if no alignment was set
     */
    public static VPos getValignment(Node child) {
        return (VPos)getConstraint(child, VALIGNMENT_CONSTRAINT);
    }

    /**
     * Sets the horizontal grow priority for the child when contained by a gridpane.
     * If set, the gridpane will use the priority to allocate the child additional
     * horizontal space if the gridpane is resized larger than it's preferred width.
     * Setting the value to null will remove the constraint.
     * @param child the child of a gridpane
     * @param value the horizontal grow priority for the child
     */
    public static void setHgrow(Node child, Priority value) {
        setConstraint(child, HGROW_CONSTRAINT, value);
    }

    /**
     * Returns the child's hgrow constraint if set.
     * @param child the child node of a gridpane
     * @return the horizontal grow priority for the child or null if no priority was set
     */
    public static Priority getHgrow(Node child) {
        return (Priority)getConstraint(child, HGROW_CONSTRAINT);
    }

    /**
     * Sets the vertical grow priority for the child when contained by a gridpane.
     * If set, the gridpane will use the priority to allocate the child additional
     * vertical space if the gridpane is resized larger than it's preferred height.
     * Setting the value to null will remove the constraint.
     * @param child the child of a gridpane
     * @param value the vertical grow priority for the child
     */
    public static void setVgrow(Node child, Priority value) {
        setConstraint(child, VGROW_CONSTRAINT, value);
    }

    /**
     * Returns the child's vgrow constraint if set.
     * @param child the child node of a gridpane
     * @return the vertical grow priority for the child or null if no priority was set
     */
    public static Priority getVgrow(Node child) {
        return (Priority)getConstraint(child, VGROW_CONSTRAINT);
    }

    /**
     * Sets the column,row indeces for the child when contained in a gridpane.
     * @param child the child node of a gridpane
     * @param columnIndex the column index position for the child
     * @param rowIndex the row index position for the child
     */
    public static void setConstraints(Node child, int columnIndex, int rowIndex) {
        setRowIndex(child, rowIndex);
        setColumnIndex(child, columnIndex);
    }

    /**
     * Sets the column, row, column-span, and row-span value for the child when
     * contained in a gridpane.
     * @param child the child node of a gridpane
     * @param columnIndex the column index position for the child
     * @param rowIndex the row index position for the child
     * @param columnspan the number of columns the child should span
     * @param rowspan the number of rows the child should span
     */
    public static void setConstraints(Node child, int columnIndex, int rowIndex, int columnspan, int rowspan) {
        setRowIndex(child, rowIndex);
        setColumnIndex(child, columnIndex);
        setRowSpan(child, rowspan);
        setColumnSpan(child, columnspan);
    }

    /**
     * Sets the grid position, spans, and alignment for the child when contained in a gridpane.
     * @param child the child node of a gridpane
     * @param columnIndex the column index position for the child
     * @param rowIndex the row index position for the child
     * @param columnspan the number of columns the child should span
     * @param rowspan the number of rows the child should span
     * @param halignment the horizontal alignment of the child
     * @param valignment the vertical alignment of the child
     */
    public static void setConstraints(Node child, int columnIndex, int rowIndex, int columnspan, int rowspan,
            HPos halignment, VPos valignment) {
        setRowIndex(child, rowIndex);
        setColumnIndex(child, columnIndex);
        setRowSpan(child, rowspan);
        setColumnSpan(child, columnspan);
        setHalignment(child, halignment);
        setValignment(child, valignment);
    }

    /**
     * Sets the grid position, spans, and alignment for the child when contained in a gridpane.
     * @param child the child node of a gridpane
     * @param columnIndex the column index position for the child
     * @param rowIndex the row index position for the child
     * @param columnspan the number of columns the child should span
     * @param rowspan the number of rows the child should span
     * @param halignment the horizontal alignment of the child
     * @param valignment the vertical alignment of the child
     * @param hgrow the horizontal grow priority of the child
     * @param vgrow the vertical grow priority of the child
     */
    public static void setConstraints(Node child, int columnIndex, int rowIndex, int columnspan, int rowspan,
            HPos halignment, VPos valignment, Priority hgrow, Priority vgrow) {
        setRowIndex(child, rowIndex);
        setColumnIndex(child, columnIndex);
        setRowSpan(child, rowspan);
        setColumnSpan(child, columnspan);
        setHalignment(child, halignment);
        setValignment(child, valignment);
        setHgrow(child, hgrow);
        setVgrow(child, vgrow);
    }

    /**
     * Sets the grid position, spans, alignment, grow priorities, and margin for
     * the child when contained in a gridpane.
     * @param child the child node of a gridpane
     * @param columnIndex the column index position for the child
     * @param rowIndex the row index position for the child
     * @param columnspan the number of columns the child should span
     * @param rowspan the number of rows the child should span
     * @param halignment the horizontal alignment of the child
     * @param valignment the vertical alignment of the child
     * @param hgrow the horizontal grow priority of the child
     * @param vgrow the vertical grow priority of the child
     * @param margin the margin of space around the child
     */
    public static void setConstraints(Node child, int columnIndex, int rowIndex, int columnspan, int rowspan,
            HPos halignment, VPos valignment, Priority hgrow, Priority vgrow, Insets margin) {
        setRowIndex(child, rowIndex);
        setColumnIndex(child, columnIndex);
        setRowSpan(child, rowspan);
        setColumnSpan(child, columnspan);
        setHalignment(child, halignment);
        setValignment(child, valignment);
        setHgrow(child, hgrow);
        setVgrow(child, vgrow);
        setMargin(child, margin);
    }

    /**
     * Removes all gridpane constraints from the child node.
     * @param child the child node
     */
    public static void clearConstraints(Node child) {
        setRowIndex(child, null);
        setColumnIndex(child, null);
        setRowSpan(child, null);
        setColumnSpan(child, null);
        setHalignment(child, null);
        setValignment(child, null);
        setHgrow(child, null);
        setVgrow(child, null);
        setMargin(child, null);
    }


    private static final Color GRID_LINE_COLOR = Color.rgb(30, 30, 30);
    private static final double GRID_LINE_DASH = 3;

    static void createRow(int rowIndex, int columnIndex, Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            setConstraints(nodes[i], columnIndex + i, rowIndex);
        }
    }

    static void createColumn(int columnIndex, int rowIndex, Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            setConstraints(nodes[i], columnIndex, rowIndex + i);
        }
    }
    
    static int getNodeRowIndex(Node node) {
        Integer rowIndex = getRowIndex(node);
        return rowIndex != null? rowIndex : 0;
    }

    private static int getNodeRowSpan(Node node) {
        Integer rowspan = getRowSpan(node);
        return rowspan != null? rowspan : 1;
    }

    static int getNodeRowEnd(Node node) {
        int rowSpan = getNodeRowSpan(node);
        return rowSpan != REMAINING? getNodeRowIndex(node) + rowSpan - 1 : REMAINING;
    }

    static int getNodeColumnIndex(Node node) {
        Integer columnIndex = getColumnIndex(node);
        return columnIndex != null? columnIndex : 0;
    }

    private static int getNodeColumnSpan(Node node) {
        Integer colspan = getColumnSpan(node);
        return colspan != null? colspan : 1;
    }

    static int getNodeColumnEnd(Node node) {
        int columnSpan = getNodeColumnSpan(node);
        return columnSpan != REMAINING? getNodeColumnIndex(node) + columnSpan - 1 : REMAINING;
    }

    private static Priority getNodeHgrow(Node node) {
        Priority hgrow = getHgrow(node);
        return hgrow != null? hgrow : Priority.NEVER;
    }

    private static Priority getNodeVgrow(Node node) {
        Priority vgrow = getVgrow(node);
        return vgrow != null? vgrow : Priority.NEVER;
    }

    private static double sum(double[] numbers) {
        double total = 0;
        for (double n : numbers) {
            total += n;
        }
        return total;
    }

    private static Priority[] createPriorityArray(int length, Priority value) {
        Priority[] array = new Priority[length];
        for (int i = 0; i < length; i++) {
            array[i] = value;
        }
        return array;
    }

    /********************************************************************
     *  END static methods
     ********************************************************************/

    /**
     * Creates a GridPane layout with hgap/vgap = 0 and TOP_LEFT alignment.
     */
    public GridPane() {
        super();
        getChildren().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> c) {
                requestLayout();
            }
        });
    }

    /**
     * The width of the horizontal gaps between columns.
     */
    public final DoubleProperty hgapProperty() {
        if (hgap == null) {
            hgap = new StyleableDoubleProperty(0) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<GridPane, Number> getCssMetaData() {
                    return StyleableProperties.HGAP;
                }

                @Override
                public Object getBean() {
                    return GridPane.this;
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
     * The height of the vertical gaps between rows.
     */
    public final DoubleProperty vgapProperty() {
        if (vgap == null) {
            vgap = new StyleableDoubleProperty(0) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<GridPane, Number> getCssMetaData() {
                    return StyleableProperties.VGAP;
                }

                @Override
                public Object getBean() {
                    return GridPane.this;
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
     * The alignment of of the grid within the gridpane's width and height.
     */
    public final ObjectProperty<Pos> alignmentProperty() {
        if (alignment == null) {
            alignment = new StyleableObjectProperty<Pos>(Pos.TOP_LEFT) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<GridPane, Pos> getCssMetaData() {
                    return StyleableProperties.ALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return GridPane.this;
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
    public final void setAlignment(Pos value) {
        alignmentProperty().set(value);
    }
    public final Pos getAlignment() {
        return alignment == null ? Pos.TOP_LEFT : alignment.get();
    }
    private Pos getAlignmentInternal() {
        Pos localPos = getAlignment();
        return localPos == null ? Pos.TOP_LEFT : localPos;
    }

    /**
     * For debug purposes only: controls whether lines are displayed to show the gridpane's rows and columns.
     * Default is <code>false</code>.
     */
    public final BooleanProperty gridLinesVisibleProperty() {
        if (gridLinesVisible == null) {
            gridLinesVisible = new StyleableBooleanProperty() {
                @Override
                protected void invalidated() {
                    if (get()) {
                        gridLines = new Group();
                        gridLines.setManaged(false);
                        getChildren().add(gridLines);
                    } else {
                        getChildren().remove(gridLines);
                        gridLines = null;
                    }
                    requestLayout();
                }

                @Override
                public CssMetaData<GridPane, Boolean> getCssMetaData() {
                    return StyleableProperties.GRID_LINES_VISIBLE;
                }

                @Override
                public Object getBean() {
                    return GridPane.this;
                }

                @Override
                public String getName() {
                    return "gridLinesVisible";
                }
            };
        }
        return gridLinesVisible;
    }

    private BooleanProperty gridLinesVisible;
    public final void setGridLinesVisible(boolean value) { gridLinesVisibleProperty().set(value); }
    public final boolean isGridLinesVisible() { return gridLinesVisible == null ? false : gridLinesVisible.get(); }

    /**
     * RowConstraints instances can be added to explicitly control individual row
     * sizing and layout behavior.
     * If not set, row sizing and layout behavior will be computed based on content.
     *
     */
    private final ObservableList<RowConstraints> rowConstraints = new TrackableObservableList<RowConstraints>() {
        @Override
        protected void onChanged(Change<RowConstraints> c) {
            while (c.next()) {
                for (RowConstraints constraints : c.getRemoved()) {
                    if (constraints != null && !rowConstraints.contains(constraints)) {
                        constraints.remove(GridPane.this);
                    }
                }
                for (RowConstraints constraints : c.getAddedSubList()) {
                    if (constraints != null) {
                        constraints.add(GridPane.this);
                    }
                }
            }
            requestLayout();
        }
    };

    /**
     * Returns list of row constraints. Row constraints can be added to
     * explicitly control individual row sizing and layout behavior.
     * If not set, row sizing and layout behavior is computed based on content.
     */
    public final ObservableList<RowConstraints> getRowConstraints() { return rowConstraints; }
    /**
     * ColumnConstraints instances can be added to explicitly control individual column
     * sizing and layout behavior.
     * If not set, column sizing and layout behavior will be computed based on content.
     */
    private final ObservableList<ColumnConstraints> columnConstraints = new TrackableObservableList<ColumnConstraints>() {
        @Override
        protected void onChanged(Change<ColumnConstraints> c) {
            while(c.next()) {
                for (ColumnConstraints constraints : c.getRemoved()) {
                    if (constraints != null && !columnConstraints.contains(constraints)) {
                        constraints.remove(GridPane.this);
                    }
                }
                for (ColumnConstraints constraints : c.getAddedSubList()) {
                    if (constraints != null) {
                        constraints.add(GridPane.this);
                    }
                }
            }
            requestLayout();
        }
    };

    /**
     * Returns list of column constraints. Column constraints can be added to
     * explicitly control individual column sizing and layout behavior.
     * If not set, column sizing and layout behavior is computed based on content.
     */
    public final ObservableList<ColumnConstraints> getColumnConstraints() { return columnConstraints; }

    /**
     * Adds a child to the gridpane at the specified column,row position.
     * This convenience method will set the gridpane column and row constraints
     * on the child.
     * @param child the node being added to the gridpane
     * @param columnIndex the column index position for the child within the gridpane
     * @param rowIndex the row index position for the child within the gridpane
     */
    public void add(Node child, int columnIndex, int rowIndex) {
        setConstraints(child, columnIndex, rowIndex);
        getChildren().add(child);
    }

    /**
     * Adds a child to the gridpane at the specified column,row position and spans.
     * This convenience method will set the gridpane column, row, and span constraints
     * on the child.
     * @param child the node being added to the gridpane
     * @param columnIndex the column index position for the child within the gridpane
     * @param rowIndex the row index position for the child within the gridpane
     * @param colspan the number of columns the child's layout area should span
     * @param rowspan the number of rows the child's layout area should span
     */
    public void add(Node child, int columnIndex, int rowIndex, int colspan, int rowspan) {
        setConstraints(child, columnIndex, rowIndex, colspan, rowspan);
        getChildren().add(child);
    }

    /**
     * Convenience method for placing the specified nodes sequentially in a given
     * row of the gridpane.    If the row already contains nodes the specified nodes
     * will be appended to the row.  For example, the first node will be positioned at [column,row],
     * the second at [column+1,row], etc.   This method will set the appropriate gridpane
     * row/column constraints on the nodes as well as add the nodes to the gridpane's
     * children sequence.
     *
     * @param rowIndex the row index position for the children within the gridpane
     * @param children the nodes to be added as a row in the gridpane
     */
    public void addRow(int rowIndex, Node... children) {
        int columnIndex = 0;       
        final List<Node> list = getChildren();
        for (int i = 0, size = list.size(); i < size; i++) {
            Node child = list.get(i);
            if (child.isManaged() && rowIndex == getNodeRowIndex(child)) {
                int index = getNodeColumnIndex(child);
                int end = getNodeColumnEnd(child);
                columnIndex = Math.max(columnIndex, (end != REMAINING? end : index) + 1);
            }
        }        
        createRow(rowIndex, columnIndex, children);
        getChildren().addAll(children);
    }

    /**
     * Convenience method for placing the specified nodes sequentially in a given
     * column of the gridpane.    If the column already contains nodes the specified nodes
     * will be appended to the column.  For example, the first node will be positioned at [column, row],
     * the second at [column, row+1], etc.   This method will set the appropriate gridpane
     * row/column constraints on the nodes as well as add the nodes to the gridpane's
     * children sequence.
     *
     * @param columnIndex the column index position for the children within the gridpane
     * @param children the nodes to be added as a column in the gridpane
     */
    public void addColumn(int columnIndex, Node... children)  {
        int rowIndex = 0;
        final List<Node> list = getChildren();
        for (int i = 0, size = list.size(); i < size; i++) {
            Node child = list.get(i);
            if (child.isManaged() && columnIndex == getNodeColumnIndex(child)) {
                int index = getNodeRowIndex(child);
                int end = getNodeRowEnd(child);
                rowIndex = Math.max(rowIndex, (end != REMAINING? end : index) + 1);
            }
        }        
        createColumn(columnIndex, rowIndex, children);
        getChildren().addAll(children);
    }

    private Group gridLines;

    private double[] rowPercentHeight;
    private double rowPercentTotal = 0;

    private double[] rowMinHeight;
    private double[] rowPrefHeight;
    private double[]  rowMaxHeight;
    private double[] rowBaseline;
    private Priority[] rowGrow;

    private double[] rowHeights;

    private double[] columnPercentWidth;
    private double columnPercentTotal = 0;

    private double[] columnMinWidth;
    private double[] columnPrefWidth;
    private double[] columnMaxWidth;
    private Priority[] columnGrow;

    private double[] columnWidths;

    private boolean metricsDirty = true;
    // This is set to true while in layoutChildren and set false on the conclusion.
    // It is used to decide whether to update metricsDirty in requestLayout().
    private boolean performingLayout = false;
    
    @Override protected double computeMinWidth(double height) {
        computeGridMetrics();
        if (getContentBias() == Orientation.VERTICAL) {
            adjustRowHeights(rowMinHeight, height);
            computeColumnMetrics(columnWidths.length, rowHeights);
        }
        return snapSpace(getInsets().getLeft()) +
               (computeTotalWidth(columnMinWidth) + (columnMinWidth.length - 1) * snapSpace(getHgap())) +
               snapSpace(getInsets().getRight());

    }

    @Override protected double computeMinHeight(double width) {
        computeGridMetrics();
        if (getContentBias() == Orientation.HORIZONTAL) {
            adjustColumnWidths(columnMinWidth, width);
            computeRowMetrics(rowHeights.length, columnWidths);
        }
        return snapSpace(getInsets().getTop()) +
               (computeTotalHeight(rowMinHeight) + (rowMinHeight.length - 1) * snapSpace(getVgap())) +
               snapSpace(getInsets().getBottom());
    }

    @Override protected double computePrefWidth(double height) {
        computeGridMetrics();
        if (getContentBias() == Orientation.VERTICAL) {
            adjustRowHeights(rowPrefHeight, height);
            computeColumnMetrics(columnWidths.length, rowHeights);
        }
        return snapSpace(getInsets().getLeft()) +
               (computeTotalWidth(columnPrefWidth) + (columnPrefWidth.length - 1) * snapSpace(getHgap())) +
               snapSpace(getInsets().getRight());
    }

    @Override protected double computePrefHeight(double width) {
        computeGridMetrics();
        if (getContentBias() == Orientation.HORIZONTAL) {
            adjustColumnWidths(columnPrefWidth, width);
            computeRowMetrics(rowHeights.length, columnWidths);
        }
        return snapSpace(getInsets().getTop()) +
               (computeTotalHeight(rowPrefHeight) + (rowPrefHeight.length - 1) * snapSpace(getVgap())) +
               snapSpace(getInsets().getBottom());
    }

    private double computeTotalWidth(double widths[]) {
        double totalWidth = 0;
        double nonPercentWidthTotal = 0;
        for (int i = 0; i < widths.length; i++) {
            if (columnPercentWidth[i] < 0) {
                nonPercentWidthTotal += widths[i];
            } else {
                totalWidth = Math.max(totalWidth, (widths[i] * 100)/columnPercentWidth[i]);
            }
        }
        if (columnPercentTotal <= 0) {
            totalWidth = nonPercentWidthTotal;
        } else if (columnPercentTotal < 100) {
            totalWidth = Math.max(totalWidth, (nonPercentWidthTotal * 100)/(100-columnPercentTotal));
        }
        return totalWidth;
    }

    private double computeTotalHeight(double heights[]) {
        double totalHeight = 0;
        double nonPercentHeightTotal = 0;
        for (int i = 0; i < heights.length; i++) {
            if (rowPercentHeight[i] < 0) {
                nonPercentHeightTotal += heights[i];
            } else {
                totalHeight = Math.max(totalHeight, (heights[i] * 100)/rowPercentHeight[i]);
            }
        }
        if (rowPercentTotal <= 0) {
            totalHeight = nonPercentHeightTotal;
        } else if (rowPercentTotal < 100) {
            totalHeight = Math.max(totalHeight, (nonPercentHeightTotal * 100)/(100-rowPercentTotal));
        }
        return totalHeight;
    }

    private VPos getRowValignment(int rowIndex) {
        if (rowIndex < getRowConstraints().size()) {
            RowConstraints constraints = getRowConstraints().get(rowIndex);
            if (constraints.getValignment() != null) {
                return constraints.getValignment();
            }
        }
        return VPos.CENTER;
    }

    private HPos getColumnHalignment(int columnIndex) {
        if (columnIndex < getColumnConstraints().size()) {
            ColumnConstraints constraints = getColumnConstraints().get(columnIndex);
            if (constraints.getHalignment() != null) {
                return constraints.getHalignment();
            }
        }
        return HPos.LEFT;
    }

    private boolean shouldRowFillHeight(int rowIndex) {
        if (rowIndex < getRowConstraints().size()) {
            return getRowConstraints().get(rowIndex).isFillHeight() && getRowValignment(rowIndex) != VPos.BASELINE;
        }
        return true;
    }

    private boolean shouldColumnFillWidth(int columnIndex) {
        if (columnIndex < getColumnConstraints().size()) {
            return getColumnConstraints().get(columnIndex).isFillWidth();
        }
        return true;
    }

    private void computeGridMetrics() {
        if (metricsDirty) {
            int numRows = getRowConstraints().size();
            int numColumns = getColumnConstraints().size();
            final List<Node> children = getChildren();
            for (int i = 0, size = children.size(); i < size; i++) {
                Node child = children.get(i);
                if (child.isManaged()) {
                    int rowIndex = getNodeRowIndex(child);
                    int columnIndex = getNodeColumnIndex(child);
                    int rowEnd = getNodeRowEnd(child);
                    int columnEnd = getNodeColumnEnd(child);
                    numRows = Math.max(numRows, (rowEnd != REMAINING? rowEnd : rowIndex) + 1);
                    numColumns = Math.max(numColumns, (columnEnd != REMAINING? columnEnd : columnIndex) + 1);
                }
            }
            //println("computeGridMetrics: rows={numRows} columns={numColumns}");
            computeRowMetrics(numRows, createDoubleArray(numColumns, -1));
            computeColumnMetrics(numColumns, createDoubleArray(numRows, -1));
            metricsDirty = false;
        }
    }

    private void computeRowMetrics(int numRows, double widths[]) {
        rowPercentHeight = createDoubleArray(numRows, -1);
        rowMinHeight = createDoubleArray(numRows, 0);
        rowPrefHeight = createDoubleArray(numRows, 0);
        rowMaxHeight = createDoubleArray(numRows, java.lang.Integer.MAX_VALUE);
        rowHeights = createDoubleArray(numRows, 0);
        rowBaseline = createDoubleArray(numRows, 0);
        rowGrow = createPriorityArray(numRows, Priority.NEVER);

        final double snapvgap = snapSpace(getVgap());
        final double snaphgap = snapSpace(getHgap());
        for (int i = 0; i < numRows; i++) {
            boolean computeMin = true;
            boolean computeMax = true;
            boolean computePref = true;
            boolean computeGrow = true;
            List<Node> startNodes = new ArrayList<Node>();
            List<Node> endNodes = new ArrayList<Node>();
            final List<Node> children = getChildren();
            for (int j = 0, size = children.size(); j < size; j++) {
                Node child = children.get(j);
                if (child.isManaged()) {
                    if (getNodeRowIndex(child) == i) {
                        startNodes.add(child);
                    }
                    int rowEnd = getNodeRowEnd(child);
                    if ((rowEnd == REMAINING && i == (numRows - 1)) || rowEnd == i) {
                        endNodes.add(child);
                    }
                }
            }

            if (i < getRowConstraints().size()) {
                RowConstraints constraints = getRowConstraints().get(i);
                if (constraints.getPercentHeight() > 0) {
                    rowPercentHeight[i] = constraints.getPercentHeight();
                    computeGrow = false;
                } else {
                    double h = constraints.getPrefHeight();
                    if (h != USE_COMPUTED_SIZE) {
                        rowPrefHeight[i] = h;
                        computePref = false;
                    }
                    h = constraints.getMinHeight();
                    if (h != USE_COMPUTED_SIZE) {
                        rowMinHeight[i] = h;
                        computeMin = false;
                    }
                    h = constraints.getMaxHeight();
                    if (h != USE_COMPUTED_SIZE) {
                        rowMaxHeight[i] = h;
                        computeMax = false;
                    }
                    if (constraints.getVgrow() != null) {
                        rowGrow[i] = constraints.getVgrow();
                        computeGrow = false;
                    }
                }
            }
            VPos rowVPos = getRowValignment(i);
            Insets margins[] = new Insets[startNodes.size()];
            List<Node> baselineNodes = new ArrayList<Node>();
            for(int j = 0, k = 0, size = startNodes.size(); j < size; j++) {
                Node n = startNodes.get(j);
                if (rowVPos == VPos.BASELINE || getValignment(n) == VPos.BASELINE) {
                    baselineNodes.add(n);
                    margins[k++] = getMargin(n);
                }
            }
            rowBaseline[i] = getMaxAreaBaselineOffset(baselineNodes, margins);
            baselineNodes.clear();

            if (computeMin || computeMax || computePref || computeGrow || rowVPos == VPos.BASELINE) {
                // compute from content
                for (int j = 0, size = endNodes.size(); j < size; j++) {
                    Node child = endNodes.get(j);                    
                    Insets margin = getMargin(child);
                    double top = margin != null? margin.getTop() : 0;
                    int rowIndex = getNodeRowIndex(child);
                    int rowspan = getNodeRowSpan(child);                    
                    if (rowspan == REMAINING) {
                        rowspan = numRows - rowIndex;
                    }
                    int colIndex = getNodeColumnIndex(child);
                    int colspan = getNodeColumnSpan(child);
                    double width = widths[colIndex];
                    if (colspan != REMAINING && colspan > 1) {
                        for (int k = colIndex; k < colIndex + colspan; k++) {
                            if (widths[k] != USE_COMPUTED_SIZE) {
                                width += widths[k];
                            }
                        }
                        width += ((colspan - 1) * snaphgap);
                    }
                    
                    if (computePref) {
                        double preferredHeight = computeChildPrefAreaHeight(child, margin, width);
                        if (rowspan > 1) {
                            double h = 0.0f;
                            for (int k = rowIndex; k < rowIndex+rowspan-1 ; k++) {
                                h += rowPrefHeight[k];
                            }
                            preferredHeight -= h + ((rowspan-1) * snapvgap);
                        } else if (rowVPos == VPos.BASELINE) {
                            preferredHeight = rowBaseline[i] + (preferredHeight - child.getBaselineOffset() - top);
                        }
                        rowPrefHeight[i] = Math.max(rowPrefHeight[i], preferredHeight);
                    }
                    if (computeMin) {
                        double minimumHeight = computeChildMinAreaHeight(child, margin, width);
                        if (rowspan > 1) {
                            double h = 0.0f;
                            for (int k = rowIndex; k < rowIndex+rowspan-1 ; k++) {
                                h += rowMinHeight[k];
                            }
                            minimumHeight -= h + ((rowspan-1) * snapvgap);
                        } else if (rowVPos == VPos.BASELINE) {
                            minimumHeight = rowBaseline[i] + (minimumHeight - child.getBaselineOffset() - top);
                        }
                        rowMinHeight[i] = Math.max(rowMinHeight[i], minimumHeight);
                    }
                    if (computeMax) {
                        double maximumHeight = computeChildMaxAreaHeight(child, margin, width);
                        if (rowspan > 1) {
                            double h = 0.0f;
                            for (int k = rowIndex; k < rowIndex+rowspan-1 ; k++) {
                                h += rowMaxHeight[k];
                            }
                            maximumHeight -= h + ((rowspan-1) * snapvgap);
                        }
                        rowMaxHeight[i] = Math.max(rowMaxHeight[i], maximumHeight);
                    }
                    if (computeGrow && rowspan == 1) {
                        rowGrow[i] = Priority.max(rowGrow[i], getNodeVgrow(child));
                    }
                }

            }
            if (rowMinHeight[i] == USE_PREF_SIZE) {
                //RT-20573 Use the bounded size if the pref has not been set
                rowMinHeight[i] = rowPrefHeight[i] == 0 ?
                        boundedSize(rowMinHeight[i], rowPrefHeight[i], rowMaxHeight[i]) == USE_PREF_SIZE ?
                            0 : boundedSize(rowMinHeight[i], rowPrefHeight[i], rowMaxHeight[i]) :
                        rowPrefHeight[i];
            }
            if (rowMaxHeight[i] == USE_PREF_SIZE) {
                rowMaxHeight[i] = rowPrefHeight[i] == 0 ?
                        boundedSize(rowMinHeight[i], rowPrefHeight[i], rowMaxHeight[i]) == USE_PREF_SIZE ?
                            0 : boundedSize(rowMinHeight[i], rowPrefHeight[i], rowMaxHeight[i]) :
                        rowPrefHeight[i];
            }
            rowPrefHeight[i] = boundedSize(rowMinHeight[i], rowPrefHeight[i], rowMaxHeight[i]);
            //System.out.println("row "+i+": h="+rowHeights[i]+" percent="+rowPercentHeight[i]+" min="+rowMinHeight[i]+" pref="+rowPrefHeight[i]+" max="+rowMaxHeight[i]+" grow="+rowGrow[i]);
        }

        rowPercentTotal = 0;
        for (int i = 0; i < rowPercentHeight.length; i++) {
            if (rowPercentHeight[i] > 0) {
                rowPercentTotal += rowPercentHeight[i];
            }
        }
        if (rowPercentTotal > 100) {
            double weight = 100/rowPercentTotal;
            //System.out.println("  converting rowPercentTotal="+rowPercentTotal+" by weight="+weight);
            for (int i = 0; i < rowPercentHeight.length; i++) {
                if (rowPercentHeight[i] > 0) {
                    rowPercentHeight[i] *= weight;
                }
            }
            rowPercentTotal = 100;
        }
    }

    private void computeColumnMetrics(int numColumns, double heights[]) {
        columnPercentWidth = createDoubleArray(numColumns, -1);
        columnMinWidth = createDoubleArray(numColumns, 0);
        columnPrefWidth = createDoubleArray(numColumns, 0);
        columnMaxWidth = createDoubleArray(numColumns, java.lang.Integer.MAX_VALUE);
        columnWidths = createDoubleArray(numColumns, 0);
        columnGrow = createPriorityArray(numColumns, Priority.NEVER);
        
        final double snaphgap = snapSpace(getHgap());
        final double snapvgap = snapSpace(getVgap());
        for (int i = 0; i < numColumns; i++) {
            boolean computeMin = true;
            boolean computeMax = true;
            boolean computePref = true;
            boolean computeGrow = true;
            List<Node> startNodes = new ArrayList<Node>();
            List<Node> endNodes = new ArrayList<Node>();
            final List<Node> children = getChildren();
            for (int j = 0, size = children.size(); j < size; j++) {
                Node child = children.get(j);
                if (child.isManaged()) {
                    if (getNodeColumnIndex(child) == i) {
                        startNodes.add(child);
                    }
                    int columnEnd = getNodeColumnEnd(child);
                    if ((columnEnd == REMAINING && i == (numColumns - 1)) || columnEnd == i) {
                        endNodes.add(child);
                    }
                }
            }

            if (i < getColumnConstraints().size()) {
                ColumnConstraints constraints = getColumnConstraints().get(i);
                if (constraints.getPercentWidth() > 0) {
                    columnPercentWidth[i] = constraints.getPercentWidth();
                    computeGrow = false;
                } else {
                    double w = constraints.getPrefWidth();          
                    if (w != USE_COMPUTED_SIZE) {
                        columnPrefWidth[i] = w;
                        computePref = false;
                    }
                    w = constraints.getMinWidth();
                    if (w != USE_COMPUTED_SIZE) {
                        columnMinWidth[i] = w;
                        computeMin = false;
                    }
                    w = constraints.getMaxWidth();
                    if (w != USE_COMPUTED_SIZE) {
                        columnMaxWidth[i] = w;
                        computeMax = false;
                    }
                    if (constraints.getHgrow() != null) {
                        columnGrow[i] = constraints.getHgrow();
                        computeGrow = false;
                    }
                }
            }

            if (computeMin || computeMax || computePref || computeGrow) {
                // compute from content                
                for (int j = 0, size = endNodes.size(); j < size; j++) {
                    Node child = endNodes.get(j);
                    Insets margin = getMargin(child);
                    int columnIndex = getNodeColumnIndex(child);
                    int colspan = getNodeColumnSpan(child);
                    if (colspan == REMAINING) {
                        colspan = numColumns - columnIndex;
                    }
                    int rowIndex = getNodeRowIndex(child);
                    int rowspan = getNodeRowSpan(child);
                    double height = heights[rowIndex];
                    if (rowspan != REMAINING && rowspan > 1) {
                        for (int k = rowIndex; k < rowIndex + rowspan; k++) {
                            if (heights[k] != USE_COMPUTED_SIZE) {
                                height += heights[k];
                            }
                        }
                        height += ((rowspan - 1) * snapvgap);
                    }
                    
                    if (computePref) {
                        double preferredWidth = computeChildPrefAreaWidth(child, margin, height);
                        if (colspan > 1) {
                            double w = 0.0f;
                            for (int k = columnIndex; k < columnIndex + colspan - 1; k++) {
                                w += columnPrefWidth[k];
                            }
                            preferredWidth -= w + ((colspan-1)*snaphgap);
                        }
                        columnPrefWidth[i] = Math.max(columnPrefWidth[i], preferredWidth);                             
                    }
                    if (computeMin) {
                        double minimumWidth = computeChildMinAreaWidth(child, margin, height);
                        if (colspan > 1) {
                            double w = 0.0f;
                            for (int k = columnIndex; k < columnIndex + colspan - 1; k++) {
                                w += columnMinWidth[k];
                            }
                            minimumWidth -= w + ((colspan-1)*snaphgap);
                        }
                        columnMinWidth[i] = Math.max(columnMinWidth[i], minimumWidth);
                    }
                    if (computeMax) {
                        double maximumWidth = computeChildMaxAreaWidth(child, margin, height);
                        if (colspan > 1) {
                            double w = 0.0f;
                            for (int k = columnIndex; k < columnIndex + colspan - 1; k++) {
                                w += columnMaxWidth[k];
                            }
                            maximumWidth -= w + ((colspan-1)*snaphgap);
                        }
                        columnMaxWidth[i] = Math.max(columnMaxWidth[i], maximumWidth);
                    }

                    if (computeGrow && colspan == 1) {
                        columnGrow[i] = Priority.max(columnGrow[i], getNodeHgrow(child));
                    }

                }
            }

            if (columnMinWidth[i] == USE_PREF_SIZE) {
                //RT-20573 Use the bounded size if the pref has not been set
                columnMinWidth[i] = columnPrefWidth[i] == 0 ? 
                    boundedSize(columnMinWidth[i], columnPrefWidth[i], columnMaxWidth[i]) == USE_PREF_SIZE ?
                        0 : boundedSize(columnMinWidth[i], columnPrefWidth[i], columnMaxWidth[i]) :
                    columnPrefWidth[i];
            }
            if (columnMaxWidth[i] == USE_PREF_SIZE) {
                columnMaxWidth[i] = columnPrefWidth[i] == 0 ? 
                    boundedSize(columnMinWidth[i], columnPrefWidth[i], columnMaxWidth[i]) == USE_PREF_SIZE ?
                        0 : boundedSize(columnMinWidth[i], columnPrefWidth[i], columnMaxWidth[i]) :
                    columnPrefWidth[i];
            }                        
            columnPrefWidth[i] = boundedSize(columnMinWidth[i], columnPrefWidth[i], columnMaxWidth[i]);
            //System.out.println("column "+i+": w="+columnWidths[i]+" percent="+columnPercentWidth[i]+" min="+columnMinWidth[i]+" pref="+columnPrefWidth[i]+" max="+columnMaxWidth[i]+" grow="+columnGrow[i]);
        }
        // if percentages sum is bigger than 100, treat them as weights
        columnPercentTotal = 0;
        for (int i = 0; i < columnPercentWidth.length; i++) {
            if (columnPercentWidth[i] > 0) {
                columnPercentTotal += columnPercentWidth[i];
            }
        }
        if (columnPercentTotal > 100) {
            double weight = 100/columnPercentTotal;
            //System.out.println("  converting columnPercentTotal="+columnPercentTotal+" by weight="+weight);
            for (int i = 0; i < columnPercentWidth.length; i++) {
                if (columnPercentWidth[i] > 0) {
                    columnPercentWidth[i] *= weight;
                }
            }
            columnPercentTotal = 100;
        }
    }

    double[] getColumnWidths() {
        return columnWidths;
    }

    double[] getRowHeights() {
        return rowHeights;
    }

    /**
     *
     * @return null unless one of its children has a content bias.
     */
    @Override public Orientation getContentBias() {
        final List<Node> children = getChildren();
        for (int i = 0, size = children.size(); i < size; i++) {
            Node child = children.get(i);
            if (child.isManaged() && child.getContentBias() != null) {
                return child.getContentBias();
            }
        }
        return null;
    }

    @Override public void requestLayout() {
        // RT-18878: Do not update metrics dirty if we are performing layout.
        // If metricsDirty is set true during a layout pass the next call to computeGridMetrics()
        // will clear all the cell bounds resulting in out of date info until the
        // next layout pass.
        if (!metricsDirty && !performingLayout) {
            metricsDirty = true;
        }
        super.requestLayout();
    }

    @Override protected void layoutChildren() {
        performingLayout = true;
        final double snaphgap = snapSpace(getHgap());
        final double snapvgap = snapSpace(getVgap());
        final double top = snapSpace(getInsets().getTop());
        final double bottom = snapSpace(getInsets().getBottom());
        final double left = snapSpace(getInsets().getLeft());
        final double right = snapSpace(getInsets().getRight());

        final double width = getWidth();
        final double height = getHeight();
        final double contentHeight = height - top - bottom;
        final double contentWidth = width - left - right;
        double columnTotal = 0;
        double rowTotal = 0;
        computeGridMetrics();

        Orientation contentBias = getContentBias();        
        if (contentBias == null) {
            rowTotal = adjustRowHeights(rowPrefHeight, height);
            columnTotal = adjustColumnWidths(columnPrefWidth, width);
        } else if (contentBias == Orientation.HORIZONTAL) {         
            columnTotal = adjustColumnWidths(columnPrefWidth, width);
            computeRowMetrics(rowHeights.length, columnWidths);
            rowTotal = adjustRowHeights(rowPrefHeight, height);
        } else if (contentBias == Orientation.VERTICAL) {
            rowTotal = adjustRowHeights(rowPrefHeight, height);
            computeColumnMetrics(columnWidths.length, rowHeights);
            columnTotal = adjustColumnWidths(columnPrefWidth, width);
        }

        final double x = left + computeXOffset(contentWidth, columnTotal, getAlignmentInternal().getHpos());
        final double y = top + computeYOffset(contentHeight, rowTotal, getAlignmentInternal().getVpos());
        final List<Node> children = getChildren();
        for (int i = 0, size = children.size(); i < size; i++) {
            Node child = children.get(i);
            if (child.isManaged()) {
                int rowIndex = getNodeRowIndex(child);
                int columnIndex = getNodeColumnIndex(child);
                int colspan = getNodeColumnSpan(child);
                if (colspan == REMAINING) {
                    colspan = columnWidths.length - columnIndex;
                }
                int rowspan = getNodeRowSpan(child);
                if (rowspan == REMAINING) {
                    rowspan = rowHeights.length - rowIndex;
                }
                double areaX = x;
                for (int j = 0; j < columnIndex; j++) {
                    areaX += columnWidths[j] + snaphgap;
                }
                double areaY = y;
                for (int j = 0; j < rowIndex; j++) {
                    areaY += rowHeights[j] + snapvgap;
                }
                double areaW = columnWidths[columnIndex];
                for (int j = 2; j <= colspan; j++) {
                    areaW += columnWidths[columnIndex+j-1] + snaphgap;
                }
                double areaH = rowHeights[rowIndex];
                for (int j = 2; j <= rowspan; j++) {
                    areaH += rowHeights[rowIndex+j-1] + snapvgap;
                }

                HPos halign = getHalignment(child);
                VPos valign = getValignment(child);
                Insets margin = getMargin(child);
                if (margin != null && valign == VPos.BASELINE) {
                    // The top margin has already added to rowBaseline[] in computeRowMetric()
                    // we do not need to add it again in layoutInArea.
                    margin = new Insets(0, margin.getRight(), margin.getBottom(), margin.getLeft());
                }
                //System.out.println("layoutNode("+child.toString()+" row/span="+rowIndex+"/"+rowspan+" col/span="+columnIndex+"/"+colspan+" area="+areaX+","+areaY+" "+areaW+"x"+areaH+""+" rowBaseline="+rowBaseline[rowIndex]);
                layoutInArea(child, areaX, areaY, areaW, areaH, rowBaseline[rowIndex],
                        margin,
                        shouldColumnFillWidth(columnIndex), shouldRowFillHeight(rowIndex),
                        halign != null? halign : getColumnHalignment(columnIndex),
                        valign != null? valign : getRowValignment(rowIndex));
            }
        }
        layoutGridLines(x, y, rowTotal, columnTotal);
        performingLayout = false;
    }

    private double adjustRowHeights(double areaHeights[], double height) {
        final double snapvgap = snapSpace(getVgap());
        final double top = snapSpace(getInsets().getTop());
        final double bottom = snapSpace(getInsets().getBottom());
        final int numRows = rowHeights.length;
        final double vgaps = snapvgap * (numRows - 1);
        double rowTotal = vgaps;
        final double contentHeight = getHeight() - top - bottom;

        // if there are percentage rows, give them their percentages first
        if (rowPercentTotal > 0) {
            for (int i = 0; i < rowPercentHeight.length; i++) {
                if (rowPercentHeight[i] >= 0) {
                    rowHeights[i] = (contentHeight - vgaps) * (rowPercentHeight[i]/100);
                    rowTotal += rowHeights[i];
                }
            }
        }
        // compute non-percentage row heights
        for (int i = 0; i < numRows; i++) {
            if (rowPercentHeight[i] < 0) {
                rowHeights[i] = boundedSize(rowMinHeight[i], areaHeights[i], rowMaxHeight[i]);
                rowTotal += rowHeights[i];
            }
        }
        double heightAvailable = (height == -1 ? prefHeight(-1) : height) - top - bottom - rowTotal;
        // now that both fixed and percentage rows have been computed, divy up any surplus or deficit
        if (heightAvailable != 0) {
            // maybe grow or shrink row heights
            double remaining = growOrShrinkRowHeights(Priority.ALWAYS, heightAvailable);
            remaining = growOrShrinkRowHeights(Priority.SOMETIMES, remaining);
            rowTotal += (heightAvailable - remaining);
        }
        return rowTotal;
    }

    private double growOrShrinkRowHeights(Priority priority, double extraHeight) {
        final boolean shrinking = extraHeight < 0;
        List<Integer> adjusting = new ArrayList<Integer>();
        List<Integer> adjusted = new ArrayList<Integer>();

        for (int i = 0; i < rowGrow.length; i++) {
            if (rowPercentHeight[i] < 0 && (shrinking || rowGrow[i] == priority)) {
                adjusting.add(i);
            }
        }

        double available = extraHeight; // will be negative in shrinking case
        boolean handleRemainder = false;
        int portion = 0;
        while (available != 0 && adjusting.size() > 0) {
            if (!handleRemainder) {
                portion = (int)available / adjusting.size(); // negative in shrinking case
            }
            if (portion != 0) {
                for (int i = 0, size = adjusting.size(); i < size; i++) {
                    final int index = adjusting.get(i);
                    final double limit = (shrinking? rowMinHeight[index] : rowMaxHeight[index])
                            - rowHeights[index]; // negative in shrinking case
                    final double change = Math.abs(limit) <= Math.abs(portion)? limit : portion;
                    //System.out.println("row "+index+": height="+rowHeights[index]+" extra="+extraHeight+"portion="+portion+" row mpm="+rowMinHeight[index]+"/"+rowPrefHeight[index]+"/"+rowMaxHeight[index]+" limit="+limit+" change="+change);
                    rowHeights[index] += change;                
                    available -= change;
                    if (Math.abs(change) < Math.abs(portion)) {
                        adjusted.add(index);
                    }
                    if (available == 0) {
                        break;
                    }   
                }
                for (int i = 0, size = adjusted.size(); i < size; i++) {
                    adjusting.remove(adjusted.get(i));
                }
                adjusted.clear();
            } else {
                // Handle the remainder
                portion = (int)(available) % adjusting.size();
                if (portion == 0) {
                    break;
                } else {
                    // We have a remainder evenly distribute it.
                    portion = shrinking ? -1 : 1;
                    handleRemainder = true;
                }
            }
        }
                        
        for (int i = 0; i < rowHeights.length; i++) {
            rowHeights[i] = snapSpace(rowHeights[i]);       
        }
        return available; // might be negative in shrinking case
    }

    private double adjustColumnWidths(double areaWidths[], double width) {
        final double snaphgap = snapSpace(getHgap());
        final double left = snapSpace(getInsets().getLeft());
        final double right = snapSpace(getInsets().getRight());
        final int numColumns = columnWidths.length;
        final double hgaps = snaphgap * (numColumns - 1);
        double columnTotal = hgaps;
        final double contentWidth = getWidth() - left - right;
        
        // if there are percentage columns, give them their percentages first
        if (columnPercentTotal > 0) {
            for (int i = 0; i < columnPercentWidth.length; i++) {
                if (columnPercentWidth[i] >= 0) {
                    columnWidths[i] = (contentWidth - hgaps) * (columnPercentWidth[i]/100);
                    columnTotal += columnWidths[i];
                }
            }
        }
        // compute non-percentage column widths
        for (int i = 0; i < numColumns; i++) {
            if (columnPercentWidth[i] < 0) {
                columnWidths[i] = boundedSize(columnMinWidth[i], areaWidths[i], columnMaxWidth[i]);
                columnTotal += columnWidths[i];
            }
        }
        
        double widthAvailable = (width == -1 ? prefWidth(-1) : width) - left - right - columnTotal;
        // now that both fixed and percentage columns have been computed, divy up any surplus or deficit
        if (widthAvailable != 0) {
            // maybe grow or shrink column widths
            double remaining = growOrShrinkColumnWidths(Priority.ALWAYS, widthAvailable);
            remaining = growOrShrinkColumnWidths(Priority.SOMETIMES, remaining);
            columnTotal += (widthAvailable - remaining);
        }
        return columnTotal;
    }

    private double growOrShrinkColumnWidths(Priority priority, double extraWidth) {
        final boolean shrinking = extraWidth < 0;

        List<Integer> adjusting = new ArrayList<Integer>();
        List<Integer> adjusted = new ArrayList<Integer>();

        for (int i = 0; i < columnGrow.length; i++) {
            if (columnPercentWidth[i] < 0 && (shrinking || columnGrow[i] == priority)) {
                adjusting.add(i);
            }
        }
        
        double available = extraWidth; // will be negative in shrinking case
        boolean handleRemainder = false;
        int portion = 0;
        
        // RT-25684: We have to be careful that when subtracting change
        // that we don't jump right past 0 - this leads to an infinite
        // loop
        final boolean wasPositive = available >= 0.0;
        boolean isPositive = wasPositive;
        
        while (available != 0 && wasPositive == isPositive && adjusting.size() > 0) {            
            if (!handleRemainder) {
                portion = (int)available / adjusting.size(); // negative in shrinking case
            }
            if (portion != 0) {
                for (int i = 0, size = adjusting.size(); i < size; i++) {    
                    final int index = adjusting.get(i);
                    final double limit = (shrinking? columnMinWidth[index] : columnMaxWidth[index])
                            - columnWidths[index]; // negative in shrinking case
                    final double change = Math.abs(limit) <= Math.abs(portion)? limit : portion;
                    columnWidths[index] += change;                

                    // added for RT-25684, as outlined above
                    available -= change;
                    isPositive = available >= 0.0;
                    
                    if (Math.abs(change) < Math.abs(portion)) {
                        adjusted.add(index);
                    }
                    if (available == 0) {                        
                        break;
                    }                    
                }
                for (int i = 0, size = adjusted.size(); i < size; i++) {                
                    adjusting.remove(adjusted.get(i));
                }
                adjusted.clear();
            } else {
                // Handle the remainder
                portion = (int)(available) % adjusting.size();
                if (portion == 0) {
                    break;
                } else {
                    // We have a remainder evenly distribute it.
                    portion = shrinking ? -1 : 1;
                    handleRemainder = true;
                }
            }
        }
               
        for (int i = 0; i < columnWidths.length; i++) {
            columnWidths[i] = snapSpace(columnWidths[i]);
        }
        return available; // might be negative in shrinking case
    }

    private void layoutGridLines(double x, double y, double columnHeight, double rowWidth) {
        if (!isGridLinesVisible()) {
            return;
        }
        if (!gridLines.getChildren().isEmpty()) {
            gridLines.getChildren().clear();
        }
        double hgap = snapSpace(getHgap());
        double vgap = snapSpace(getVgap());

        // create vertical lines
        double linex = x;
        double liney = y;
        for (int i = 0; i <= columnWidths.length; i++) {
             gridLines.getChildren().add(createGridLine(linex, liney, linex, liney + columnHeight));
             if (i > 0 && i < columnWidths.length && getHgap() != 0) {
                 linex += getHgap();
                 gridLines.getChildren().add(createGridLine(linex, liney, linex, liney + columnHeight));
             }
             if (i < columnWidths.length) {
                 linex += columnWidths[i];
             }
        }
        // create horizontal lines
        linex = x;
        for (int i = 0; i <= rowHeights.length; i++) {
            gridLines.getChildren().add(createGridLine(linex, liney, linex + rowWidth, liney));
            if (i > 0 && i < rowHeights.length && getVgap() != 0) {
                liney += getVgap();
                gridLines.getChildren().add(createGridLine(linex, liney, linex + rowWidth, liney));
            }
            if (i < rowHeights.length) {
                liney += rowHeights[i];
            }
        }
    }

    private Line createGridLine(double startX, double startY, double endX, double endY) {
         Line line = new Line();
         line.setStartX(startX);
         line.setStartY(startY);
         line.setEndX(endX);
         line.setEndY(endY);
         line.setStroke(GRID_LINE_COLOR);
         line.setStrokeDashOffset(GRID_LINE_DASH);

         return line;
    }

    /**
     * Returns a string representation of this {@code GridPane} object.
     * @return a string representation of this {@code GridPane} object.
     */
    @Override public String toString() {
        return "Grid hgap="+getHgap()+", vgap="+getVgap()+", alignment="+getAlignment();
    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

      /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatAsPrivate implementation detail
      */
     private static class StyleableProperties {

         private static final CssMetaData<GridPane,Boolean> GRID_LINES_VISIBLE =
             new CssMetaData<GridPane,Boolean>("-fx-grid-lines-visible",
                 BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(GridPane node) {
                return node.gridLinesVisible == null ||
                        !node.gridLinesVisible.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(GridPane node) {
                return (StyleableProperty<Boolean>)node.gridLinesVisibleProperty();
            }
         };

         private static final CssMetaData<GridPane,Number> HGAP =
             new CssMetaData<GridPane,Number>("-fx-hgap",
                 SizeConverter.getInstance(), 0.0){

            @Override
            public boolean isSettable(GridPane node) {
                return node.hgap == null || !node.hgap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(GridPane node) {
                return (StyleableProperty<Number>)node.hgapProperty();
            }

         };

         private static final CssMetaData<GridPane,Pos> ALIGNMENT =
             new CssMetaData<GridPane,Pos>("-fx-alignment",
                 new EnumConverter<Pos>(Pos.class), Pos.TOP_LEFT) {

            @Override
            public boolean isSettable(GridPane node) {
                return node.alignment == null || !node.alignment.isBound();
            }

            @Override
            public StyleableProperty<Pos> getStyleableProperty(GridPane node) {
                return (StyleableProperty<Pos>)node.alignmentProperty();
            }

         };

         private static final CssMetaData<GridPane,Number> VGAP =
             new CssMetaData<GridPane,Number>("-fx-vgap",
                 SizeConverter.getInstance(), 0.0){

            @Override
            public boolean isSettable(GridPane node) {
                return node.vgap == null || !node.vgap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(GridPane node) {
                return (StyleableProperty<Number>)node.vgapProperty();
            }

         };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {

            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());
            styleables.add(GRID_LINES_VISIBLE);
            styleables.add(HGAP);
            styleables.add(ALIGNMENT);
            styleables.add(VGAP);
            
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     *
     */
    
    
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

}
