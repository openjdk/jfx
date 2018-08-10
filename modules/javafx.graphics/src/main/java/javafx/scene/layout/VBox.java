/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import javafx.css.Styleable;
import javafx.util.Callback;

/**
 * VBox lays out its children in a single vertical column.
 * If the vbox has a border and/or padding set, then the contents will be laid
 * out within those insets.
 * <p>
 * VBox example:
 * <pre>{@code
 *     VBox vbox = new VBox(8); // spacing = 8
 *     vbox.getChildren().addAll(new Button("Cut"), new Button("Copy"), new Button("Paste"));
 * }</pre>
 *
 * VBox will resize children (if resizable) to their preferred heights and uses its
 * {@link #fillWidthProperty() fillWidth} property to determine whether to resize their widths to
 * fill its own width or keep their widths to their preferred (fillWidth defaults to true).
 * The alignment of the content is controlled by the {@link #alignmentProperty() alignment} property,
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
 * <h3>Resizable Range</h3>
 *
 * <p>
 * A vbox's parent will resize the vbox within the vbox's resizable range
 * during layout.   By default the vbox computes this range based on its content
 * as outlined in the table below.
 * </p>
 * <table border="1">
 * <caption>VBox Resize Table</caption>
 * <tr><td></td><th scope="col">width</th><th scope="col">height</th></tr>
 * <tr><th scope="row">minimum</th>
 * <td>left/right insets plus the largest of the children's min widths.</td>
 * <td>top/bottom insets plus the sum of each child's min height plus spacing between each child.</td>
 * </tr>
 * <tr><th scope="row">preferred</th>
 * <td>left/right insets plus the largest of the children's pref widths.</td>
 * <td>top/bottom insets plus the sum of each child's pref height plus spacing between each child.</td>
 * </tr>
 * <tr><th scope="row">maximum</th>
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
 * VBox does not clip its content by default, so it is possible that children's
 * bounds may extend outside its own bounds if a child's min size prevents it from
 * being fit within the vbox.</p>
 *
 * <h3>Optional Layout Constraints</h3>
 *
 * <p>
 * An application may set constraints on individual children to customize VBox's layout.
 * For each constraint, VBox provides a static method for setting it on the child.
 * </p>
 *
 * <table border="1">
 * <caption>VBox Constraint Table</caption>
 * <tr><th scope="col">Constraint</th><th scope="col">Type</th><th scope="col">Description</th></tr>
 * <tr><th scope="row">vgrow</th><td>javafx.scene.layout.Priority</td><td>The vertical grow priority for the child.</td></tr>
 * <tr><th scope="row">margin</th><td>javafx.geometry.Insets</td><td>Margin space around the outside of the child.</td></tr>
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
 * @since JavaFX 2.0
 */
public class VBox extends Pane {

    private boolean biasDirty = true;
    private Orientation bias;
    private double[][] tempArray;

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

    private static final Callback<Node, Insets> marginAccessor = n -> getMargin(n);

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
     * @since JavaFX 8.0
     */
    public VBox(Node... children) {
        super();
        getChildren().addAll(children);
    }

    /**
     * Creates an VBox layout with the specified spacing between children.
     * @param spacing the amount of horizontal space between each child
     * @param children The initial set of children for this pane.
     * @since JavaFX 8.0
     */
    public VBox(double spacing, Node... children) {
        this();
        setSpacing(spacing);
        getChildren().addAll(children);
    }

    /**
     * The amount of vertical space between each child in the vbox.
     * @return the amount of vertical space between each child in the vbox
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
     * @return the overall alignment of children within the vbox's width and
     * height
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
     * or be resized to their preferred width and aligned according to the <code>alignment</code>
     * hpos value.
     * @return true if resizable children will be resized to fill the full width
     * of the vbox
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
        if (biasDirty) {
            bias = null;
            final List<Node> children = getManagedChildren();
            for (Node child : children) {
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

    @Override protected double computeMinWidth(double height) {
        Insets insets = getInsets();
        List<Node>managed = getManagedChildren();
        double contentWidth = 0;
        if (height != -1 && getContentBias() != null) {
            double prefHeights[][] = getAreaHeights(managed, -1, false);
            adjustAreaHeights(managed, prefHeights, height, -1);
            contentWidth = computeMaxMinAreaWidth(managed, marginAccessor, prefHeights[0], false);
        } else {
            contentWidth = computeMaxMinAreaWidth(managed, marginAccessor);
        }
        return snapSpaceX(insets.getLeft()) + contentWidth + snapSpaceX(insets.getRight());
    }

    @Override protected double computeMinHeight(double width) {
        Insets insets = getInsets();
        return snapSpaceY(insets.getTop()) +
               computeContentHeight(getManagedChildren(), width, true) +
               snapSpaceY(insets.getBottom());
    }

    @Override protected double computePrefWidth(double height) {
        Insets insets = getInsets();
        List<Node>managed = getManagedChildren();
        double contentWidth = 0;
        if (height != -1 && getContentBias() != null) {
            double prefHeights[][] = getAreaHeights(managed, -1, false);
            adjustAreaHeights(managed, prefHeights, height, -1);
            contentWidth = computeMaxPrefAreaWidth(managed, marginAccessor, prefHeights[0], false);
        } else {
            contentWidth = computeMaxPrefAreaWidth(managed, marginAccessor);
        }
        return snapSpaceX(insets.getLeft()) + contentWidth + snapSpaceX(insets.getRight());
    }

    @Override protected double computePrefHeight(double width) {
        Insets insets = getInsets();
        double d = snapSpaceY(insets.getTop()) +
               computeContentHeight(getManagedChildren(), width, false) +
               snapSpaceY(insets.getBottom());
        return d;
    }


    private double[][] getAreaHeights(List<Node>managed, double width, boolean minimum) {
        // width could be -1
        double[][] temp = getTempArray(managed.size());
        final double insideWidth = width == -1? -1 : width -
                                     snapSpaceX(getInsets().getLeft()) - snapSpaceX(getInsets().getRight());
        final boolean isFillWidth = isFillWidth();
        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            Insets margin = getMargin(child);
            if (minimum) {
                if (insideWidth != -1 && isFillWidth) {
                    temp[0][i] = computeChildMinAreaHeight(child, -1, margin, insideWidth);
                } else {
                    temp[0][i] = computeChildMinAreaHeight(child, -1, margin, -1);
                }
            } else {
                if (insideWidth != -1 && isFillWidth) {
                    temp[0][i] = computeChildPrefAreaHeight(child, -1, margin, insideWidth);
                } else {
                    temp[0][i] = computeChildPrefAreaHeight(child, -1, margin, -1);
                }
            }
        }
        return temp;
    }

    private double adjustAreaHeights(List<Node>managed, double areaHeights[][], double height, double width) {
        Insets insets = getInsets();
        double left = snapSpaceX(insets.getLeft());
        double right = snapSpaceX(insets.getRight());

        double contentHeight = sum(areaHeights[0], managed.size()) + (managed.size()-1)*snapSpaceY(getSpacing());
        double extraHeight = height -
                snapSpaceY(insets.getTop()) - snapSpaceY(insets.getBottom()) - contentHeight;

        if (extraHeight != 0) {
            final double refWidth = isFillWidth()&& width != -1? width - left - right : -1;
            double remaining = growOrShrinkAreaHeights(managed, areaHeights, Priority.ALWAYS, extraHeight, refWidth);
            remaining = growOrShrinkAreaHeights(managed, areaHeights, Priority.SOMETIMES, remaining, refWidth);
            contentHeight += (extraHeight - remaining);
        }

        return contentHeight;
    }

    private double growOrShrinkAreaHeights(List<Node>managed, double areaHeights[][], Priority priority, double extraHeight, double width) {
        final boolean shrinking = extraHeight < 0;
        int adjustingNumber = 0;

        double[] usedHeights = areaHeights[0];
        double[] temp = areaHeights[1];

        if (shrinking) {
            adjustingNumber = managed.size();
            for (int i = 0, size = managed.size(); i < size; i++) {
                final Node child = managed.get(i);
                temp[i] = computeChildMinAreaHeight(child, -1, getMargin(child), width);
            }
        } else {
            for (int i = 0, size = managed.size(); i < size; i++) {
            final Node child = managed.get(i);
            if (getVgrow(child) == priority) {
                temp[i] = computeChildMaxAreaHeight(child, -1, getMargin(child), width);
                adjustingNumber++;
            } else {
                temp[i] = -1;
            }
        }
        }

        double available = extraHeight; // will be negative in shrinking case
        outer: while (Math.abs(available) > 1 && adjustingNumber > 0) {
            final double portion = snapPortionY(available / adjustingNumber); // negative in shrinking case
            for (int i = 0, size = managed.size(); i < size; i++) {
                if (temp[i] == -1) {
                    continue;
                }
                final double limit = temp[i] - usedHeights[i]; // negative in shrinking case
                final double change = Math.abs(limit) <= Math.abs(portion)? limit : portion;
                usedHeights[i] += change;
                available -= change;
                if (Math.abs(available) < 1) {
                    break outer;
                }
                if (Math.abs(change) < Math.abs(portion)) {
                    temp[i] = -1;
                    adjustingNumber--;
                }
            }
        }

        return available; // might be negative in shrinking case
    }

    private double computeContentHeight(List<Node> managedChildren, double width, boolean minimum) {
        return sum(getAreaHeights(managedChildren, width, minimum)[0], managedChildren.size())
                + (managedChildren.size()-1)*snapSpaceY(getSpacing());
    }

    private static double sum(double[] array, int size) {
        int i = 0;
        double res = 0;
        while (i != size) {
            res += array[i++];
        }
        return res;
    }

    @Override public void requestLayout() {
        biasDirty = true;
        bias = null;
        super.requestLayout();
    }

    @Override protected void layoutChildren() {
        List<Node> managed = getManagedChildren();
        Insets insets = getInsets();
        double width = getWidth();
        double height = getHeight();
        double top = snapSpaceY(insets.getTop());
        double left = snapSpaceX(insets.getLeft());
        double bottom = snapSpaceY(insets.getBottom());
        double right = snapSpaceX(insets.getRight());
        double space = snapSpaceY(getSpacing());
        HPos hpos = getAlignmentInternal().getHpos();
        VPos vpos = getAlignmentInternal().getVpos();
        boolean isFillWidth = isFillWidth();

        double[][] actualAreaHeights = getAreaHeights(managed, width, false);
        double contentWidth = width - left - right;
        double contentHeight = adjustAreaHeights(managed, actualAreaHeights, height, width);

        double x = left;
        double y = top + computeYOffset(height - top - bottom, contentHeight, vpos);

        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            layoutInArea(child, x, y, contentWidth, actualAreaHeights[0][i],
                       /* baseline shouldn't matter */actualAreaHeights[0][i],
                       getMargin(child), isFillWidth, true,
                       hpos, vpos);
            y += actualAreaHeights[0][i] + space;
        }
    }

    private double[][] getTempArray(int size) {
        if (tempArray == null) {
            tempArray = new double[2][size]; // First array for the result, second for temporary computations
        } else if (tempArray[0].length < size) {
            tempArray = new double[2][Math.max(tempArray.length * 3, size)];
        }
        return tempArray;

    }

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

     /*
      * Super-lazy instantiation pattern from Bill Pugh.
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
     * CssMetaData of its superclasses.
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

}
