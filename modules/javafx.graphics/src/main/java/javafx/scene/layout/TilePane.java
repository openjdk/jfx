/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.binding.ExpressionHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableIntegerProperty;
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
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.css.Styleable;

import static javafx.geometry.Orientation.*;
import javafx.util.Callback;


/**
 * TilePane lays out its children in a grid of uniformly sized "tiles".
 * <p>
 * A horizontal tilepane (the default) will tile nodes in rows, wrapping at the
 * tilepane's width.  A vertical tilepane will tile nodes in columns,
 * wrapping at the tilepane's height.
 * <p>
 * The size of each "tile" defaults to the size needed to encompass the largest
 * preferred width and height of the tilepane's children and the tilepane
 * will recompute the size of the tiles as needed to accommodate the largest preferred
 * size of its children as it changes.   The application may also control the size
 * of the tiles directly by setting prefTileWidth/prefTileHeight
 * properties to a value other than USE_COMPUTED_SIZE (the default).
 * <p>
 * Applications should initialize either <code>prefColumns</code> (for horizontal)
 * or <code>prefRows</code> (for vertical) to establish the tilepane's preferred
 * size (the arbitrary default is 5).  Note that prefColumns/prefRows
 * is used only for calculating the preferred size and may not reflect the actual
 * number of rows or columns, which may change as the tilepane is resized and
 * the tiles are wrapped at its actual boundaries.
 * <p>
 * The alignment property controls how the rows and columns are aligned
 * within the bounds of the tilepane and defaults to Pos.TOP_LEFT.  It is also possible
 * to control the alignment of nodes within the individual tiles by setting
 * {@link #tileAlignmentProperty() tileAlignment}, which defaults to Pos.CENTER.
 * <p>
 * A horizontal tilepane example:
 * <pre>{@code
 *    TilePane tile = new TilePane();
 *    tile.setHgap(8);
 *    tile.setPrefColumns(4);
 *    for (int i = 0; i < 20; i++) {
 *        tile.getChildren().add(new ImageView(...));
 *    }
 * }</pre>
 * <p>
 * A vertical TilePane example:
 * <pre>{@code
 *    TilePane tile = new TilePane(Orientation.VERTICAL);
 *    tile.setTileAlignment(Pos.CENTER_LEFT);
 *    tile.setPrefRows(10);
 *    for (int i = 0; i < 50; i++) {
 *        tile.getChildren().add(new ImageView(...));
 *    }
 * }</pre>
 *
 * The TilePane will attempt to resize each child to fill its tile.
 * If the child could not be sized to fill the tile (either because it was not
 * resizable or its size limits prevented it) then it will be aligned within the
 * tile using tileAlignment.
 *
 * <h2>Resizable Range</h2>
 *
 * <p>
 * A tilepane's parent will resize the tilepane within the tilepane's resizable range
 * during layout. By default the tilepane computes this range based on its content
 * as outlined in the tables below.
 * </p>
 * <table border="1">
 * <caption>Horizontal</caption>
 * <tr><td></td><th scope="col">width</th><th scope="col">height</th></tr>
 * <tr><th scope="row">minimum</th>
 * <td>left/right insets plus the tile width.</td>
 * <td>top/bottom insets plus height required to display all tiles when wrapped at a specified width with a vgap between each row.</td></tr>
 * <tr><th scope="row">preferred</th>
 * <td>left/right insets plus prefColumns multiplied by the tile width.</td>
 * <td>top/bottom insets plus height required to display all tiles when wrapped at a specified width with a vgap between each row.</td></tr>
 * <tr><th scope="row">maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <br>
 * <table border="1">
 * <caption>Vertical</caption>
 * <tr><td></td><th scope="col">width</th><th scope="col">height</th></tr>
 * <tr><th scope="row">minimum</th>
 * <td>left/right insets plus width required to display all tiles when wrapped at a specified height with an hgap between each column.</td>
 * <td>top/bottom insets plus the tile height.</td></tr>
 * <tr><th scope="row">preferred</th>
 * <td>left/right insets plus width required to display all tiles when wrapped at the specified height with an hgap between each column.</td>
 * <td>top/bottom insets plus prefRows multiplied by the tile height.</td></tr>
 * <tr><th scope="row">maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * A tilepane's unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned to it.
 * <p>
 * TilePane provides properties for setting the size range directly.  These
 * properties default to the sentinel value Region.USE_COMPUTED_SIZE, however the
 * application may set them to other values as needed:
 * <pre><code>
 *     <b>tilePane.setMaxWidth(500);</b>
 * </code></pre>
 * Applications may restore the computed values by setting these properties back
 * to Region.USE_COMPUTED_SIZE.
 * <p>
 * TilePane does not clip its content by default, so it is possible that children's'
 * bounds may extend outside the tiles (and possibly the tilepane bounds) if a
 * child's pref size prevents it from being fit within its tile. Also, if the tilepane
 * is resized smaller than its preferred size, it may not be able to fit all the
 * tiles within its bounds and the content will extend outside.
 *
 * <h2>Optional Layout Constraints</h2>
 *
 * <p>
 * An application may set constraints on individual children to customize TilePane's layout.
 * For each constraint, TilePane provides a static method for setting it on the child.
 * </p>
 *
 * <table border="1">
 * <caption>TilePane Constraint Table</caption>
 * <tr><th scope="col">Constraint</th><th scope="col">Type</th><th scope="col">Description</th></tr>
 * <tr><th scope="row">alignment</th><td>javafx.geometry.Pos</td><td>The alignment of the child within its tile.</td></tr>
 * <tr><th scope="row">margin</th><td>javafx.geometry.Insets</td><td>Margin space around the outside of the child.</td></tr>
 * </table>
 * <p>
 * Example:
 * <pre>{@code
 *     TilePane tilepane = new TilePane();
 *     for (int i = 0; i < 20; i++) {
 *        Label title = new Label(imageTitle[i]):
 *        Imageview imageview = new ImageView(new Image(imageName[i]));
 *        TilePane.setAlignment(label, Pos.BOTTOM_RIGHT);
 *        tilepane.getChildren().addAll(title, imageview);
 *     }
 * }</pre>
 * @since JavaFX 2.0
 */
public class TilePane extends Pane {

    /* ******************************************************************
     *  BEGIN static methods
     ********************************************************************/

    private static final String MARGIN_CONSTRAINT = "tilepane-margin";
    private static final String ALIGNMENT_CONSTRAINT = "tilepane-alignment";

    /**
     * Sets the alignment for the child when contained by a tilepane.
     * If set, will override the tilepane's default alignment for children
     * within their 'tiles'.
     * Setting the value to null will remove the constraint.
     * @param node the child node of a tilepane
     * @param value the alignment position for the child
     */
    public static void setAlignment(Node node, Pos value) {
        setConstraint(node, ALIGNMENT_CONSTRAINT, value);
    }

    /**
     * Returns the child's alignment constraint if set.
     * @param node the child node of a tilepane
     * @return the alignment position for the child or null if no alignment was set
     */
    public static Pos getAlignment(Node node) {
        return (Pos)getConstraint(node, ALIGNMENT_CONSTRAINT);
    }

    /**
     * Sets the margin for the child when contained by a tilepane.
     * If set, the tilepane will layout the child with the margin space around it.
     * Setting the value to null will remove the constraint.
     * @param node the child node of a tilepane
     * @param value the margin of space around the child
     */
    public static void setMargin(Node node, Insets value) {
        setConstraint(node, MARGIN_CONSTRAINT, value);
    }

    /**
     * Returns the child's margin constraint if set.
     * @param node the child node of a tilepane
     * @return the margin for the child or null if no margin was set
     */
    public static Insets getMargin(Node node) {
        return (Insets)getConstraint(node, MARGIN_CONSTRAINT);
    }

    private static final Callback<Node, Insets> marginAccessor = n -> getMargin(n);

    /**
     * Removes all tilepane constraints from the child node.
     * @param child the child node
     */
    public static void clearConstraints(Node child) {
        setAlignment(child, null);
        setMargin(child, null);
    }

    /* ******************************************************************
     *  END static methods
     ********************************************************************/

    private double _tileWidth = -1;
    private double _tileHeight = -1;

    /**
     * Creates a horizontal TilePane layout with prefColumn = 5 and hgap/vgap = 0.
     */
    public TilePane() {
        super();
    }

    /**
     * Creates a TilePane layout with the specified orientation,
     * prefColumn/prefRows = 5 and hgap/vgap = 0.
     * @param orientation the direction the tiles should flow &amp; wrap
     */
    public TilePane(Orientation orientation) {
        super();
        setOrientation(orientation);
    }

    /**
     * Creates a horizontal TilePane layout with prefColumn = 5 and the specified
     * hgap/vgap.
     * @param hgap the amount of horizontal space between each tile
     * @param vgap the amount of vertical space between each tile
     */
    public TilePane(double hgap, double vgap) {
        super();
        setHgap(hgap);
        setVgap(vgap);
    }

    /**
     * Creates a TilePane layout with the specified orientation, hgap/vgap,
     * and prefRows/prefColumns = 5.
     * @param orientation the direction the tiles should flow &amp; wrap
     * @param hgap the amount of horizontal space between each tile
     * @param vgap the amount of vertical space between each tile
     */
    public TilePane(Orientation orientation, double hgap, double vgap) {
        this();
        setOrientation(orientation);
        setHgap(hgap);
        setVgap(vgap);
    }

    /**
     * Creates a horizontal TilePane layout with prefColumn = 5 and hgap/vgap = 0.
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public TilePane(Node... children) {
        super();
        getChildren().addAll(children);
    }

    /**
     * Creates a TilePane layout with the specified orientation,
     * prefColumn/prefRows = 5 and hgap/vgap = 0.
     * @param orientation the direction the tiles should flow &amp; wrap
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public TilePane(Orientation orientation, Node... children) {
        super();
        setOrientation(orientation);
        getChildren().addAll(children);
    }

    /**
     * Creates a horizontal TilePane layout with prefColumn = 5 and the specified
     * hgap/vgap.
     * @param hgap the amount of horizontal space between each tile
     * @param vgap the amount of vertical space between each tile
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public TilePane(double hgap, double vgap, Node... children) {
        super();
        setHgap(hgap);
        setVgap(vgap);
        getChildren().addAll(children);
    }

    /**
     * Creates a TilePane layout with the specified orientation, hgap/vgap,
     * and prefRows/prefColumns = 5.
     * @param orientation the direction the tiles should flow &amp; wrap
     * @param hgap the amount of horizontal space between each tile
     * @param vgap the amount of vertical space between each tile
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public TilePane(Orientation orientation, double hgap, double vgap, Node... children) {
        this();
        setOrientation(orientation);
        setHgap(hgap);
        setVgap(vgap);
        getChildren().addAll(children);
    }

    /**
     * The orientation of this tilepane.
     * A horizontal tilepane lays out children in tiles, left to right, wrapping
     * tiles at the tilepane's width boundary.   A vertical tilepane lays out
     * children in tiles, top to bottom, wrapping at the tilepane's height.
     * The default is horizontal.
     * @return the orientation of this tilepane
     */
    public final ObjectProperty<Orientation> orientationProperty() {
        if (orientation == null) {
            orientation = new StyleableObjectProperty(HORIZONTAL) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<TilePane, Orientation> getCssMetaData() {
                    return StyleableProperties.ORIENTATION;
                }

                @Override
                public Object getBean() {
                    return TilePane.this;
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
     * The preferred number of rows for a vertical tilepane.
     * This value is used only to compute the preferred size of the tilepane
     * and may not reflect the actual number of rows, which may change
     * if the tilepane is resized to something other than its preferred height.
     * This property is ignored for a horizontal tilepane.
     * <p>
     * It is recommended that the application initialize this value for a
     * vertical tilepane.
     * @return the preferred number of rows for a vertical tilepane
     */
    public final IntegerProperty prefRowsProperty() {
        if (prefRows == null) {
            prefRows = new StyleableIntegerProperty(5) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<TilePane, Number> getCssMetaData() {
                    return StyleableProperties.PREF_ROWS;
                }

                @Override
                public Object getBean() {
                    return TilePane.this;
                }

                @Override
                public String getName() {
                    return "prefRows";
                }
            };
        }
        return prefRows;
    }

    private IntegerProperty prefRows;
    public final void setPrefRows(int value) { prefRowsProperty().set(value); }
    public final int getPrefRows() { return prefRows == null ? 5 : prefRows.get(); }

    /**
     * The preferred number of columns for a horizontal tilepane.
     * This value is used only to compute the preferred size of the tilepane
     * and may not reflect the actual number of rows, which may change if the
     * tilepane is resized to something other than its preferred height.
     * This property is ignored for a vertical tilepane.
     * <p>
     * It is recommended that the application initialize this value for a
     * horizontal tilepane.
     * @return the preferred number of columns for a horizontal tilepane
     */
    public final IntegerProperty prefColumnsProperty() {
        if (prefColumns == null) {
            prefColumns = new StyleableIntegerProperty(5) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<TilePane, Number> getCssMetaData() {
                    return StyleableProperties.PREF_COLUMNS;
                }

                @Override
                public Object getBean() {
                    return TilePane.this;
                }

                @Override
                public String getName() {
                    return "prefColumns";
                }
            };
        }
        return prefColumns;
    }

    private IntegerProperty prefColumns;
    public final void setPrefColumns(int value) { prefColumnsProperty().set(value); }
    public final int getPrefColumns() { return prefColumns == null ? 5 : prefColumns.get(); }

    /**
     * The preferred width of each tile.
     * If equal to USE_COMPUTED_SIZE (the default) the tile width wlll be
     * automatically recomputed by the tilepane when the preferred size of children
     * changes to accommodate the widest child.  If the application sets this property
     * to value greater than 0, then tiles will be set to that width and the tilepane
     * will attempt to resize children to fit within that width (if they are resizable and
     * their min-max width range allows it).
     * @return the preferred width of each tile
     */
    public final DoubleProperty prefTileWidthProperty() {
        if (prefTileWidth == null) {
            prefTileWidth = new StyleableDoubleProperty(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<TilePane, Number> getCssMetaData() {
                    return StyleableProperties.PREF_TILE_WIDTH;
                }

                @Override
                public Object getBean() {
                    return TilePane.this;
                }

                @Override
                public String getName() {
                    return "prefTileWidth";
                }
            };
        }
        return prefTileWidth;
    }

    private DoubleProperty prefTileWidth;
    public final void setPrefTileWidth(double value) { prefTileWidthProperty().set(value); }
    public final double getPrefTileWidth() { return prefTileWidth == null ? USE_COMPUTED_SIZE : prefTileWidth.get(); }

    /**
     * The preferred height of each tile.
     * If equal to USE_COMPUTED_SIZE (the default) the tile height wlll be
     * automatically recomputed by the tilepane when the preferred size of children
     * changes to accommodate the tallest child.  If the application sets this property
     * to value greater than 0, then tiles will be set to that height and the tilepane
     * will attempt to resize children to fit within that height (if they are resizable and
     * their min-max height range allows it).
     * @return the preferred height of each tile
     */
    public final DoubleProperty prefTileHeightProperty() {
        if (prefTileHeight == null) {
            prefTileHeight = new StyleableDoubleProperty(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<TilePane, Number> getCssMetaData() {
                    return StyleableProperties.PREF_TILE_HEIGHT;
                }

                @Override
                public Object getBean() {
                    return TilePane.this;
                }

                @Override
                public String getName() {
                    return "prefTileHeight";
                }
            };
        }
        return prefTileHeight;
    }

    private DoubleProperty prefTileHeight;
    public final void setPrefTileHeight(double value) { prefTileHeightProperty().set(value); }
    public final double getPrefTileHeight() { return prefTileHeight == null ? USE_COMPUTED_SIZE : prefTileHeight.get(); }

    /**
     * The actual width of each tile.  This property is read-only.
     * @return the actual width of each tile
     */
    public final ReadOnlyDoubleProperty tileWidthProperty() {
        if (tileWidth == null) {
            tileWidth = new TileSizeProperty("tileWidth", _tileWidth) {

                @Override
                public double compute() {
                    return computeTileWidth();
                }

            };
        }
        return tileWidth;
    }
    private TileSizeProperty tileWidth;
    private void invalidateTileWidth() {
        if (tileWidth != null) {
            tileWidth.invalidate();
        } else {
            _tileWidth = -1;
        }
    }

    public final double getTileWidth() {
        if (tileWidth != null) {
            return tileWidth.get();
        }
        if (_tileWidth == -1) {
            _tileWidth = computeTileWidth();
        }
        return _tileWidth;
    }

    /**
     * The actual height of each tile.  This property is read-only.
     * @return the actual height of each tile
     */
    public final ReadOnlyDoubleProperty tileHeightProperty() {
        if (tileHeight == null) {
            tileHeight = new TileSizeProperty("tileHeight", _tileHeight) {

                @Override
                public double compute() {
                    return computeTileHeight();
                }

            };
        }
        return tileHeight;
    }
    private TileSizeProperty tileHeight;
    private void invalidateTileHeight() {
        if (tileHeight != null) {
            tileHeight.invalidate();
        } else {
            _tileHeight = -1;
        }
    }

    public final double getTileHeight() {
        if (tileHeight != null) {
            return tileHeight.get();
        }
        if (_tileHeight == -1) {
            _tileHeight = computeTileHeight();
        }
        return _tileHeight;
    }

    /**
     * The amount of horizontal space between each tile in a row.
     * @return the amount of horizontal space between each tile in a row
     */
    public final DoubleProperty hgapProperty() {
        if (hgap == null) {
            hgap = new StyleableDoubleProperty() {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<TilePane, Number> getCssMetaData() {
                    return StyleableProperties.HGAP;
                }

                @Override
                public Object getBean() {
                    return TilePane.this;
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
     * The amount of vertical space between each tile in a column.
     * @return the amount of vertical space between each tile in a column
     */
    public final DoubleProperty vgapProperty() {
        if (vgap == null) {
            vgap = new StyleableDoubleProperty() {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<TilePane, Number> getCssMetaData() {
                    return StyleableProperties.VGAP;
                }

                @Override
                public Object getBean() {
                    return TilePane.this;
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
     * The overall alignment of the tilepane's content within its width and height.
     * <p>For a horizontal tilepane, each row will be aligned within the tilepane's width
     * using the alignment's hpos value, and the rows will be aligned within the
     * tilepane's height using the alignment's vpos value.
     * <p>For a vertical tilepane, each column will be aligned within the tilepane's height
     * using the alignment's vpos value, and the columns will be aligned within the
     * tilepane's width using the alignment's hpos value.
     *
     * @return the overall alignment of the tilepane's content within its width
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
                public CssMetaData<TilePane, Pos> getCssMetaData() {
                    return StyleableProperties.ALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return TilePane.this;
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
     * The default alignment of each child within its tile.
     * This may be overridden on individual children by setting the child's
     * alignment constraint.
     * @return the default alignment of each child within its tile
     */
    public final ObjectProperty<Pos> tileAlignmentProperty() {
        if (tileAlignment == null) {
            tileAlignment = new StyleableObjectProperty<Pos>(Pos.CENTER) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public CssMetaData<TilePane, Pos> getCssMetaData() {
                    return StyleableProperties.TILE_ALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return TilePane.this;
                }

                @Override
                public String getName() {
                    return "tileAlignment";
                }
            };
        }
        return tileAlignment;
    }

    private ObjectProperty<Pos> tileAlignment;
    public final void setTileAlignment(Pos value) { tileAlignmentProperty().set(value); }
    public final Pos getTileAlignment() { return tileAlignment == null ? Pos.CENTER : tileAlignment.get(); }
    private Pos getTileAlignmentInternal() {
        Pos localPos = getTileAlignment();
        return localPos == null ? Pos.CENTER : localPos;
    }

    @Override public Orientation getContentBias() {
        return getOrientation();
    }

    @Override public void requestLayout() {
        invalidateTileWidth();
        invalidateTileHeight();
        super.requestLayout();
    }

    @Override protected double computeMinWidth(double height) {
        if (getContentBias() == Orientation.HORIZONTAL) {
            return getInsets().getLeft() + getTileWidth() + getInsets().getRight();
        }
        return computePrefWidth(height);
    }

    @Override protected double computeMinHeight(double width) {
        if (getContentBias() == Orientation.VERTICAL) {
            return getInsets().getTop() + getTileHeight() + getInsets().getBottom();
        }
        return computePrefHeight(width);
    }

    @Override protected double computePrefWidth(double forHeight) {
        List<Node> managed = getManagedChildren();
        final Insets insets = getInsets();
        int prefCols = 0;
        if (forHeight != -1) {
            // first compute number of rows that will fit in given height and
            // compute pref columns from that
            int prefRows = computeRows(forHeight - snapSpaceY(insets.getTop()) - snapSpaceY(insets.getBottom()), getTileHeight());
            prefCols = computeOther(managed.size(), prefRows);
        } else {
            prefCols = getOrientation() == HORIZONTAL? getPrefColumns() : computeOther(managed.size(), getPrefRows());
        }
        return snapSpaceX(insets.getLeft()) +
               computeContentWidth(prefCols, getTileWidth()) +
               snapSpaceX(insets.getRight());
    }

    @Override protected double computePrefHeight(double forWidth) {
        List<Node> managed = getManagedChildren();
        final Insets insets = getInsets();
        int prefRows = 0;
        if (forWidth != -1) {
            // first compute number of columns that will fit in given width and
            // compute pref rows from that
            int prefCols = computeColumns(forWidth - snapSpaceX(insets.getLeft()) - snapSpaceX(insets.getRight()), getTileWidth());
            prefRows = computeOther(managed.size(), prefCols);
        } else {
            prefRows = getOrientation() == HORIZONTAL? computeOther(managed.size(), getPrefColumns()) : getPrefRows();
        }
        return snapSpaceY(insets.getTop()) +
               computeContentHeight(prefRows, getTileHeight()) +
               snapSpaceY(insets.getBottom());
    }

    private double computeTileWidth() {
        List<Node> managed = getManagedChildren();
        double preftilewidth = getPrefTileWidth();
        if (preftilewidth == USE_COMPUTED_SIZE) {
            double h = -1;
            boolean vertBias = false;
            for (int i = 0, size = managed.size(); i < size; i++) {
                Node child = managed.get(i);
                if (child.getContentBias() == VERTICAL) {
                    vertBias = true;
                    break;
                }
            }
            if (vertBias) {
                // widest may depend on height of tile
                h = computeMaxPrefAreaHeight(managed, marginAccessor, -1, getTileAlignmentInternal().getVpos());
            }
            return snapSizeX(computeMaxPrefAreaWidth(managed, marginAccessor, h, true));
        }
        return snapSizeX(preftilewidth);
    }

    private double computeTileHeight() {
        List<Node> managed = getManagedChildren();
        double preftileheight = getPrefTileHeight();
        if (preftileheight == USE_COMPUTED_SIZE) {
            double w = -1;
            boolean horizBias = false;
            for (int i = 0, size = managed.size(); i < size; i++) {
                Node child = managed.get(i);
                if (child.getContentBias() == Orientation.HORIZONTAL) {
                    horizBias = true;
                    break;
                }
            }
            if (horizBias) {
                // tallest may depend on width of tile
                w = computeMaxPrefAreaWidth(managed, marginAccessor);
            }
            return snapSizeY(computeMaxPrefAreaHeight(managed, marginAccessor, w, getTileAlignmentInternal().getVpos()));
        }
        return snapSizeY(preftileheight);
    }

    private int computeOther(int numNodes, int numCells) {
        double other = (double)numNodes/(double)Math.max(1, numCells);
        return (int)Math.ceil(other);
    }

    private int computeColumns(double width, double tilewidth) {
        double snappedHgap = snapSpaceX(getHgap());
        return Math.max(1,(int)((width + snappedHgap) / (tilewidth + snappedHgap)));
    }

    private int computeRows(double height, double tileheight) {
        double snappedVgap = snapSpaceY(getVgap());
        return Math.max(1, (int)((height + snappedVgap) / (tileheight + snappedVgap)));
    }

    private double computeContentWidth(int columns, double tilewidth) {
        if (columns == 0) return 0;
        return columns * tilewidth + (columns - 1) * snapSpaceX(getHgap());
    }

    private double computeContentHeight(int rows, double tileheight) {
        if (rows == 0) return 0;
        return rows * tileheight + (rows - 1) * snapSpaceY(getVgap());
    }

    @Override protected void layoutChildren() {
        List<Node> managed = getManagedChildren();
        HPos hpos = getAlignmentInternal().getHpos();
        VPos vpos = getAlignmentInternal().getVpos();
        double width = getWidth();
        double height = getHeight();
        double top = snapSpaceY(getInsets().getTop());
        double left = snapSpaceX(getInsets().getLeft());
        double bottom = snapSpaceY(getInsets().getBottom());
        double right = snapSpaceX(getInsets().getRight());
        double vgap = snapSpaceY(getVgap());
        double hgap = snapSpaceX(getHgap());
        double insideWidth = width - left - right;
        double insideHeight = height - top - bottom;

        double tileWidth = getTileWidth() > insideWidth ? insideWidth : getTileWidth();
        double tileHeight = getTileHeight() > insideHeight ? insideHeight : getTileHeight();

        int lastRowRemainder = 0;
        int lastColumnRemainder = 0;
        if (getOrientation() == HORIZONTAL) {
            actualColumns = computeColumns(insideWidth, tileWidth);
            actualRows = computeOther(managed.size(), actualColumns);
            // remainder will be 0 if last row is filled
            lastRowRemainder = hpos != HPos.LEFT?
                 actualColumns - (actualColumns*actualRows - managed.size()) : 0;
        } else {
            // vertical
            actualRows = computeRows(insideHeight, tileHeight);
            actualColumns = computeOther(managed.size(), actualRows);
            // remainder will be 0 if last column is filled
            lastColumnRemainder = vpos != VPos.TOP?
                actualRows - (actualColumns*actualRows - managed.size()) : 0;
        }
        double rowX = left + computeXOffset(insideWidth,
                                            computeContentWidth(actualColumns, tileWidth),
                                            hpos);
        double columnY = top + computeYOffset(insideHeight,
                                            computeContentHeight(actualRows, tileHeight),
                                            vpos);

        double lastRowX = lastRowRemainder > 0?
                          left + computeXOffset(insideWidth,
                                            computeContentWidth(lastRowRemainder, tileWidth),
                                            hpos) :  rowX;
        double lastColumnY = lastColumnRemainder > 0?
                          top + computeYOffset(insideHeight,
                                            computeContentHeight(lastColumnRemainder, tileHeight),
                                            vpos) : columnY;
        double baselineOffset = getTileAlignmentInternal().getVpos() == VPos.BASELINE ?
                getAreaBaselineOffset(managed, marginAccessor, i -> tileWidth, tileHeight, false) : -1;

        int r = 0;
        int c = 0;
        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            double xoffset = r == (actualRows - 1)? lastRowX : rowX;
            double yoffset = c == (actualColumns - 1)? lastColumnY : columnY;

            double tileX = xoffset + (c * (tileWidth + hgap));
            double tileY = yoffset + (r * (tileHeight + vgap));

            Pos childAlignment = getAlignment(child);

            layoutInArea(child, tileX, tileY, tileWidth, tileHeight, baselineOffset,
                    getMargin(child),
                    childAlignment != null? childAlignment.getHpos() : getTileAlignmentInternal().getHpos(),
                    childAlignment != null? childAlignment.getVpos() : getTileAlignmentInternal().getVpos());

            if (getOrientation() == HORIZONTAL) {
                if (++c == actualColumns) {
                    c = 0;
                    r++;
                }
            } else {
                // vertical
                if (++r == actualRows) {
                    r = 0;
                    c++;
                }
            }
        }
    }

    private int actualRows = 0;
    private int actualColumns = 0;

    /* *************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/


     /*
      * Super-lazy instantiation pattern from Bill Pugh.
      */
     private static class StyleableProperties {

         private static final CssMetaData<TilePane,Pos> ALIGNMENT =
             new CssMetaData<TilePane,Pos>("-fx-alignment",
                 new EnumConverter<Pos>(Pos.class),
                 Pos.TOP_LEFT) {

            @Override
            public boolean isSettable(TilePane node) {
                return node.alignment == null || !node.alignment.isBound();
            }

            @Override
            public StyleableProperty<Pos> getStyleableProperty(TilePane node) {
                return (StyleableProperty<Pos>)node.alignmentProperty();
            }
        };

         private static final CssMetaData<TilePane,Number> PREF_COLUMNS =
             new CssMetaData<TilePane,Number>("-fx-pref-columns",
                 SizeConverter.getInstance(), 5.0) {

            @Override
            public boolean isSettable(TilePane node) {
                return node.prefColumns == null ||
                        !node.prefColumns.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TilePane node) {
                return (StyleableProperty<Number>)node.prefColumnsProperty();
            }
        };

         private static final CssMetaData<TilePane,Number> HGAP =
             new CssMetaData<TilePane,Number>("-fx-hgap",
                 SizeConverter.getInstance(), 0.0) {

            @Override
            public boolean isSettable(TilePane node) {
                return node.hgap == null ||
                        !node.hgap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TilePane node) {
                return (StyleableProperty<Number>)node.hgapProperty();
            }
        };

         private static final CssMetaData<TilePane,Number> PREF_ROWS =
             new CssMetaData<TilePane,Number>("-fx-pref-rows",
                 SizeConverter.getInstance(), 5.0) {

            @Override
            public boolean isSettable(TilePane node) {
                return node.prefRows == null ||
                        !node.prefRows.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TilePane node) {
                return (StyleableProperty<Number>)node.prefRowsProperty();
            }
        };

         private static final CssMetaData<TilePane,Pos> TILE_ALIGNMENT =
             new CssMetaData<TilePane,Pos>("-fx-tile-alignment",
                 new EnumConverter<Pos>(Pos.class),
                 Pos.CENTER) {

            @Override
            public boolean isSettable(TilePane node) {
                return node.tileAlignment == null ||
                        !node.tileAlignment.isBound();
            }

            @Override
            public StyleableProperty<Pos> getStyleableProperty(TilePane node) {
                return (StyleableProperty<Pos>)node.tileAlignmentProperty();
            }
         };

         private static final CssMetaData<TilePane,Number> PREF_TILE_WIDTH =
             new CssMetaData<TilePane,Number>("-fx-pref-tile-width",
                 SizeConverter.getInstance(), USE_COMPUTED_SIZE) {

            @Override
            public boolean isSettable(TilePane node) {
                return node.prefTileWidth == null ||
                        !node.prefTileWidth.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TilePane node) {
                return (StyleableProperty<Number>)node.prefTileWidthProperty();
            }
        };

         private static final CssMetaData<TilePane,Number> PREF_TILE_HEIGHT =
             new CssMetaData<TilePane,Number>("-fx-pref-tile-height",
                 SizeConverter.getInstance(), USE_COMPUTED_SIZE) {

            @Override
            public boolean isSettable(TilePane node) {
                return node.prefTileHeight == null ||
                        !node.prefTileHeight.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TilePane node) {
                return (StyleableProperty<Number>)node.prefTileHeightProperty();
            }
         };

         private static final CssMetaData<TilePane,Orientation> ORIENTATION =
             new CssMetaData<TilePane,Orientation>("-fx-orientation",
                 new EnumConverter<Orientation>(Orientation.class),
                 Orientation.HORIZONTAL) {

                @Override
                public Orientation getInitialValue(TilePane node) {
                    // A vertical TilePane should remain vertical
                    return node.getOrientation();
                }

                @Override
                public boolean isSettable(TilePane node) {
                    return node.orientation == null ||
                            !node.orientation.isBound();
                }

                @Override
                public StyleableProperty<Orientation> getStyleableProperty(TilePane node) {
                    return (StyleableProperty<Orientation>)node.orientationProperty();
                }
         };

         private static final CssMetaData<TilePane,Number> VGAP =
             new CssMetaData<TilePane,Number>("-fx-vgap",
                 SizeConverter.getInstance(), 0.0) {

            @Override
            public boolean isSettable(TilePane node) {
                return node.vgap == null ||
                        !node.vgap.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(TilePane node) {
                return (StyleableProperty<Number>)node.vgapProperty();
            }
        };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());
            styleables.add(ALIGNMENT);
            styleables.add(HGAP);
            styleables.add(ORIENTATION);
            styleables.add(PREF_COLUMNS);
            styleables.add(PREF_ROWS);
            styleables.add(PREF_TILE_WIDTH);
            styleables.add(PREF_TILE_HEIGHT);
            styleables.add(TILE_ALIGNMENT);
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

    private abstract class TileSizeProperty extends ReadOnlyDoubleProperty {
        private final String name;
        private ExpressionHelper<Number> helper;
        private double value;
        private boolean valid;

        TileSizeProperty(String name, double initSize) {
            this.name = name;
            this.value = initSize;
            this.valid = initSize != -1;
        }


        @Override
        public Object getBean() {
            return TilePane.this;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void addListener(InvalidationListener listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        @Override
        public void addListener(ChangeListener<? super Number> listener) {
            helper = ExpressionHelper.addListener(helper, this, listener);
        }

        @Override
        public void removeListener(ChangeListener<? super Number> listener) {
            helper = ExpressionHelper.removeListener(helper, listener);
        }

        @Override
        public double get() {
            if (!valid) {
                value = compute();
                valid = true;
            }

            return value;
        }

        public void invalidate() {
            if (valid) {
                valid = false;
                ExpressionHelper.fireValueChangedEvent(helper);
            }
        }

        public abstract double compute();
    }

}
