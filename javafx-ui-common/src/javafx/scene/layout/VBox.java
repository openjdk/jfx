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
import javafx.css.CssMetaData;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;
import javafx.css.Styleable;

/**
 * VBox lays out its children in a single vertical column.
 * If the vbox has a border and/or padding set, then the contents will be layed
 * out within those insets.
 * <p>
 * VBox example:
 * <pre><code>
 *     VBox vbox = new VBox(8); // spacing = 8
 *     vbox.getChildren().addAll(new Button("Cut"), new Button("Copy"), new Button("Paste"));
 * </code></pre>
 *
 * VBox will resize children (if resizable) to their preferred heights and uses its
 * {@link #fillWidth} property to determine whether to resize their widths to
 * fill its own width or keep their widths to their preferred (fillWidth defaults to true).
 * The alignment of the content is controlled by the {@link #alignment} property,
 * which defaults to Pos.TOP_LEFT.
 * <p>
 * If a vbox is resized larger than its preferred height, by default it will keep
 * children to their preferred heights, leaving the extra space unused.  If an
 * application wishes to have one or more children be allocated that extra space
 * it may optionally set a vgrow constraint on the child.  See "Optional Layout
 * Constraints" for details.
 * <p>
 * VBox lays out each managed child regardless of the child's
 * visible property value; unmanaged children are ignored.</p>
 *
 * <h4>Resizable Range</h4>
 *
 * A vbox's parent will resize the vbox within the vbox's resizable range
 * during layout.   By default the vbox computes this range based on its content
 * as outlined in the table below.
 * <table border="1">
 * <tr><td></td><th>width</th><th>height</th></tr>
 * <tr><th>minimum</th>
 * <td>left/right insets plus the largest of the children's min widths.</td>
 * <td>top/bottom insets plus the sum of each child's min height plus spacing between each child.</td>
 * </tr>
 * <tr><th>preferred</th>
 * <td>left/right insets plus the largest of the children's pref widths.</td>
 * <td>top/bottom insets plus the sum of each child's pref height plus spacing between each child.</td>
 * </tr>
 * <tr><th>maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * A vbox's unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned
 * to it.
 * <p>
 * VBox provides properties for setting the size range directly.  These
 * properties default to the sentinel value USE_COMPUTED_SIZE, however the
 * application may set them to other values as needed:
 * <pre><code>
 *     <b>vbox.setPrefWidth(400);</b>
 * </code></pre>
 * Applications may restore the computed values by setting these properties back
 * to USE_COMPUTED_SIZE.
 * <p>
 * VBox does not clip its content by default, so it is possible that childrens'
 * bounds may extend outside its own bounds if a child's min size prevents it from
 * being fit within the vbox.</p>
 *
 * <h4>Optional Layout Constraints</h4>
 *
 * An application may set constraints on individual children to customize VBox's layout.
 * For each constraint, VBox provides a static method for setting it on the child.
 * <p>
 * <table border="1">
 * <tr><th>Constraint</th><th>Type</th><th>Description</th></tr>
 * <tr><td>vgrow</td><td>javafx.scene.layout.Priority</td><td>The vertical grow priority for the child.</td></tr>
 * <tr><td>margin</td><td>javafx.geometry.Insets</td><td>Margin space around the outside of the child.</td></tr>
 * </table>
 * <p>
 * For example, if a vbox needs the ListView to be allocated all extra space:
 * <pre><code>
 *     VBox vbox = new VBox();
 *     ListView list = new ListView();
 *     <b>VBox.setVgrow(list, Priority.ALWAYS);</b>
 *     vbox.getChildren().addAll(new Label("Names:"), list);
 * </code></pre>
 *
 * If more than one child has the same grow priority set, then the vbox will
 * allocate equal amounts of space to each.  VBox will only grow a child up to
 * its maximum height, so if the child has a max height other than Double.MAX_VALUE,
 * the application may need to override the max to allow it to grow.
 */
public class VBox extends Pane {

/********************************************************************
     *  BEGIN static methods
     ********************************************************************/
    private static final String MARGIN_CONSTRAINT = "vbox-margin";
    private static final String VGROW_CONSTRAINT = "vbox-vgrow";

    /**
     * Sets the vertical grow priority for the child when contained by an vbox.
     * If set, the vbox will use the priority to allocate additional space if the
     * vbox is resized larger than it's preferred height.
     * If multiple vbox children have the same vertical grow priority, then the
     * extra space will be split evenly between them.
     * If no vertical grow priority is set on a child, the vbox will never
     * allocate it additional vertical space if available.
     * Setting the value to null will remove the constraint.
     * @param child the child of a vbox
     * @param value the horizontal grow priority for the child
     */
    public static void setVgrow(Node child, Priority value) {
        setConstraint(child, VGROW_CONSTRAINT, value);
    }

    /**
     * Returns the child's vgrow property if set.
     * @param child the child node of a vbox
     * @return the vertical grow priority for the child or null if no priority was set
     */
    public static Priority getVgrow(Node child) {
        return (Priority)getConstraint(child, VGROW_CONSTRAINT);
    }

    /**
     * Sets the margin for the child when contained by a vbox.
     * If set, the vbox will layout the child so that it has the margin space around it.
     * Setting the value to null will remove the constraint.
     * @param child the child mode of a vbox
     * @param value the margin of space around the child
     */
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN_CONSTRAINT, value);
    }

    /**
     * Returns the child's margin property if set.
     * @param child the child node of a vbox
     * @return the margin for the child or null if no margin was set
     */
    public static Insets getMargin(Node child) {
        return (Insets)getConstraint(child, MARGIN_CONSTRAINT);
    }

    /**
     * Removes all vbox constraints from the child node.
     * @param child the child node
     */
    public static void clearConstraints(Node child) {
        setVgrow(child, null);
        setMargin(child, null);
    }

    /********************************************************************
     *  END static methods
     ********************************************************************/

    /**
     * Creates a VBox layout with spacing = 0 and alignment at TOP_LEFT.
     */
    public VBox() {
        super();
    }

    /**
     * Creates a VBox layout with the specified spacing between children.
     * @param spacing the amount of vertical space between each child
     */
    public VBox(double spacing) {
        this();
        setSpacing(spacing);
    }

    /**
     * Creates an VBox layout with spacing = 0.
     * @param children The initial set of children for this pane.
     */
    public VBox(Node... children) {
        super();
        getChildren().addAll(children);
    }

    /**
     * Creates an VBox layout with the specified spacing between children.
     * @param spacing the amount of horizontal space between each child
     * @param children The initial set of children for this pane.
     */
    public VBox(double spacing, Node... children) {
        this();
        setSpacing(spacing);
        getChildren().addAll(children);
    }

    /**
     * The amount of vertical space between each child in the vbox.
     */
    public final DoubleProperty spacingProperty() {
        if (spacing == null) {
            spacing = new StyleableDoubleProperty() {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return VBox.this;
                }

                @Override
                public String getName() {
                    return "spacing";
                }

                @Override
                public CssMetaData<VBox, Number> getCssMetaData() {
                    return StyleableProperties.SPACING;
                }
            };
        }
        return spacing;
    }

    private DoubleProperty spacing;
    public final void setSpacing(double value) { spacingProperty().set(value); }
    public final double getSpacing() { return spacing == null ? 0 : spacing.get(); }

    /**
     * The overall alignment of children within the vbox's width and height.
     */
    public final ObjectProperty<Pos> alignmentProperty() {
        if (alignment == null) {
            alignment = new StyleableObjectProperty<Pos>(Pos.TOP_LEFT) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return VBox.this;
                }

                @Override
                public String getName() {
                    return "alignment";
                }

                @Override
                public CssMetaData<VBox, Pos> getCssMetaData() {
                    return StyleableProperties.ALIGNMENT;
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
     * Whether or not resizable children will be resized to fill the full width of the vbox
     * or be kept to their preferred width and aligned according to the <code>alignment</code>
     * hpos value.
     */
    public final BooleanProperty fillWidthProperty() {
        if (fillWidth == null) {
            fillWidth = new StyleableBooleanProperty(true) {
                @Override
                public void invalidated() {
                    requestLayout();
                }

                @Override
                public Object getBean() {
                    return VBox.this;
                }

                @Override
                public String getName() {
                    return "fillWidth";
                }

                @Override
                public CssMetaData<VBox, Boolean> getCssMetaData() {
                    return StyleableProperties.FILL_WIDTH;
                }
            };
        }
        return fillWidth;
    }

    private BooleanProperty fillWidth;
    public final void setFillWidth(boolean value) { fillWidthProperty().set(value); }
    public final boolean isFillWidth() { return fillWidth == null ? true : fillWidth.get(); }

    /**
     *
     * @return null unless one of its children has a content bias.
     */
    @Override public Orientation getContentBias() {
        final List<Node> children = getChildren();
        for (int i=0, size=children.size(); i<size; i++) {
            Node child = children.get(i);
            if (child.isManaged() && child.getContentBias() != null) {
                return child.getContentBias();
            }
        }
        return null;
    }

    @Override protected double computeMinWidth(double height) {
        Insets insets = getInsets();
        List<Node>managed = getManagedChildren();
        double contentWidth = 0;
        if (getContentBias() == Orientation.VERTICAL) {
            double prefHeights[] = getAreaHeights(managed, -1, false);
            adjustAreaHeights(managed, prefHeights, height, -1);
            contentWidth = computeMaxMinAreaWidth(managed, getMargins(managed), prefHeights, getAlignmentInternal().getHpos());
        } else {
            contentWidth = computeMaxMinAreaWidth(managed, getMargins(managed), getAlignmentInternal().getHpos());
        }
        return snapSpace(insets.getLeft()) + contentWidth + snapSpace(insets.getRight());
    }

    @Override protected double computeMinHeight(double width) {
        Insets insets = getInsets();
        return snapSpace(insets.getTop()) +
               computeContentHeight(getAreaHeights(getManagedChildren(), width, true)) +
               snapSpace(insets.getBottom());
    }

    @Override protected double computePrefWidth(double height) {
        Insets insets = getInsets();
        List<Node>managed = getManagedChildren();
        double contentWidth = 0;
        if (getContentBias() == Orientation.VERTICAL) {
            double prefHeights[] = getAreaHeights(managed, -1, false);
            adjustAreaHeights(managed, prefHeights, height, -1);
            contentWidth = computeMaxPrefAreaWidth(managed, getMargins(managed), prefHeights, getAlignmentInternal().getHpos());
        } else {
            contentWidth = computeMaxPrefAreaWidth(managed, getMargins(managed), getAlignmentInternal().getHpos());
        }
        return snapSpace(insets.getLeft()) + contentWidth + snapSpace(insets.getRight());
    }

    @Override protected double computePrefHeight(double width) {
        Insets insets = getInsets();
        double d = snapSpace(insets.getTop()) +
               computeContentHeight(getAreaHeights(getManagedChildren(), width, false)) +
               snapSpace(insets.getBottom());
        return d;
    }

    private Insets[] getMargins(List<Node>managed) {
        Insets margins[] = new Insets[managed.size()];
        for(int i = 0; i < margins.length; i++) {
            margins[i] = getMargin(managed.get(i));
        }
        return margins;
    }

    private double[] getAreaHeights(List<Node>managed, double width, boolean minimum) {
        double[] prefAreaHeights = new double [managed.size()];
        final double insideWidth = width == -1? -1 : width -
                snapSpace(getInsets().getLeft()) - snapSpace(getInsets().getRight());
        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            Insets margin = getMargin(child);
            prefAreaHeights[i] = minimum?
                               computeChildMinAreaHeight(child, margin,
                                   isFillWidth()? insideWidth : child.minWidth(-1)) :
                                   computeChildPrefAreaHeight(child, margin,
                                       isFillWidth()? insideWidth : child.prefWidth(-1));
        }
        return prefAreaHeights;
    }

    private double adjustAreaHeights(List<Node>managed, double areaHeights[], double height, double width) {
        Insets insets = getInsets();
        double left = snapSpace(insets.getLeft());
        double right = snapSpace(insets.getRight());

        double contentHeight = computeContentHeight(areaHeights);
        double extraHeight = (height == -1 ? prefHeight(-1) : height) -
                snapSpace(insets.getTop()) - snapSpace(insets.getBottom()) - contentHeight;

        if (extraHeight != 0) {
            double remaining = growOrShrinkAreaHeights(managed, areaHeights,
                    Priority.ALWAYS, extraHeight, isFillWidth() && width != -1? width - left - right: -1);
            remaining = growOrShrinkAreaHeights(managed, areaHeights,
                    Priority.SOMETIMES, remaining, isFillWidth() && width != -1? width - left - right: -1);
            contentHeight += (extraHeight - remaining);
        }

        return contentHeight;
    }

    private double growOrShrinkAreaHeights(List<Node>managed, double areaHeights[], Priority priority, double extraHeight, double width) {
        final boolean shrinking = extraHeight < 0;
        List<Node> adjustList = new ArrayList<Node>();
        List<Node> adjusting = new ArrayList<Node>();

        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            if (shrinking || getVgrow(child) == priority) {
                adjustList.add(child);
                adjusting.add(child);
            }
        }

        double[] areaLimitHeights = new double[adjustList.size()];
        for (int i = 0, size = adjustList.size(); i < size; i++) {
            Node child = adjustList.get(i);
            Insets margin  = getMargin(child);
            areaLimitHeights[i] = shrinking?
                computeChildMinAreaHeight(child, margin, width) : computeChildMaxAreaHeight(child, margin, width);
        }

        double available = extraHeight; // will be negative in shrinking case
        while (Math.abs(available) > 1.0 && adjusting.size() > 0) {
            Node[] adjusted = new Node[adjustList.size()];
            final double portion = available / adjusting.size(); // negative in shrinking case
            for (int i = 0, size = adjusting.size(); i < size; i++) {
                final Node child = adjusting.get(i);
                final int childIndex = managed.indexOf(child);
                final double limit = areaLimitHeights[adjustList.indexOf(child)] - areaHeights[childIndex]; // negative in shrinking case
                final double change = Math.abs(limit) <= Math.abs(portion)? limit : portion;
                areaHeights[childIndex] += change;
                //if (node.id.startsWith("debug.")) println("{if (shrinking) "shrink" else "grow"}: {node.id} portion({portion})=available({available})/({sizeof adjusting}) change={change}");
                available -= change;
                if (Math.abs(change) < Math.abs(portion)) {
                    adjusted[i] = child;
                }
            }
            for (Node node : adjusted) {
                if (node != null) {
                    adjusting.remove(node);
                }
            }
        }
        return available; // might be negative in shrinking case
    }

    private double computeContentHeight(double[] heights) {
        double total = 0;
        for (double h : heights) {
            total += h;
        }
        return total + (heights.length-1)*snapSpace(getSpacing());
    }

    private double[] actualAreaHeights;

    @Override protected void layoutChildren() {
        List<Node> managed = getManagedChildren();
        Insets insets = getInsets();
        double width = getWidth();
        double height = getHeight();
        double top = snapSpace(insets.getTop());
        double left = snapSpace(insets.getLeft());
        double bottom = snapSpace(insets.getBottom());
        double right = snapSpace(insets.getRight());
        double space = snapSpace(getSpacing());
        HPos hpos = getAlignmentInternal().getHpos();
        VPos vpos = getAlignmentInternal().getVpos();

        actualAreaHeights = getAreaHeights(managed, width, false);
        double contentWidth = width - left - right;
        double contentHeight = adjustAreaHeights(managed, actualAreaHeights, height, width);

        double x = left;
        double y = top + computeYOffset(height - top - bottom, contentHeight, vpos);

        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            layoutInArea(child, x, y, contentWidth, actualAreaHeights[i],
                       /* baseline shouldn't matter */actualAreaHeights[i],
                       getMargin(child), isFillWidth(), true,
                       hpos, vpos);
            y += actualAreaHeights[i] + space;
        }
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
         private static final CssMetaData<VBox,Pos> ALIGNMENT =
             new CssMetaData<VBox,Pos>("-fx-alignment",
                 new EnumConverter<Pos>(Pos.class), Pos.TOP_LEFT){

            @Override
            public boolean isSettable(VBox node) {
                return node.alignment == null || !node.alignment.isBound();
            }

            @Override
            public StyleableProperty<Pos> getStyleableProperty(VBox node) {
                return (StyleableProperty<Pos>)node.alignmentProperty();
            }
        };

         private static final CssMetaData<VBox,Boolean> FILL_WIDTH =
             new CssMetaData<VBox,Boolean>("-fx-fill-width",
                 BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(VBox node) {
                return node.fillWidth == null || !node.fillWidth.isBound();
            }

            @Override
            public StyleableProperty<Boolean> getStyleableProperty(VBox node) {
                return (StyleableProperty<Boolean>)node.fillWidthProperty();
            }
        };

         private static final CssMetaData<VBox,Number> SPACING =
             new CssMetaData<VBox,Number>("-fx-spacing",
                 SizeConverter.getInstance(), 0d) {

            @Override
            public boolean isSettable(VBox node) {
                return node.spacing == null || !node.spacing.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(VBox node) {
                return (StyleableProperty<Number>)node.spacingProperty();
            }
        };

         private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
         static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Region.getClassCssMetaData());
            styleables.add(ALIGNMENT);
            styleables.add(FILL_WIDTH);
            styleables.add(SPACING);
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
