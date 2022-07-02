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
 * <h2>Resizable Range</h2>
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
 * <h2>Optional Layout Constraints</h2>
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

/* ******************************************************************
     *  BEGIN static methods
     ********************************************************************/
    private static final Priority[] GROW_PRIORITY = new Priority[] {Priority.ALWAYS, Priority.SOMETIMES};
    private static final String MARGIN_CONSTRAINT = "vbox-margin";
    private static final String VGROW_CONSTRAINT = "vbox-vgrow";

    /**
     * Sets the vertical grow priority for the child when contained by a vbox.
     * If set, the vbox will use the priority value to allocate additional space if the
     * vbox is resized larger than its preferred height.
     * If multiple vbox children have the same vertical grow priority, then the
     * extra space will be split evenly between them.
     * If no vertical grow priority is set on a child, the vbox will never
     * allocate any additional vertical space for that child.
     * <p>
     * Setting the value to {@code null} will remove the constraint.
     * @param child the child of a vbox
     * @param value the vertical grow priority for the child
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

    /* ******************************************************************
     *  END static methods
     ********************************************************************/

    /**
     * Creates a {@code VBox} layout with {@code spacing = 0} and alignment at {@code TOP_LEFT}.
     */
    public VBox() {
        super();
    }

    /**
     * Creates a {@code VBox} layout with the specified spacing between children.
     * @param spacing the amount of vertical space between each child
     */
    public VBox(double spacing) {
        this();
        setSpacing(spacing);
    }

    /**
     * Creates a {@code VBox} layout with {@code spacing = 0}.
     * @param children the initial set of children for this pane
     * @since JavaFX 8.0
     */
    public VBox(Node... children) {
        super();
        getChildren().addAll(children);
    }

    /**
     * Creates a {@code VBox} layout with the specified spacing between children.
     * @param spacing the amount of vertical space between each child
     * @param children the initial set of children for this pane
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
            double[][] prefHeights = computeChildrenHeights(managed, -1, false);
            adjustChildrenHeights(managed, prefHeights, height, -1);
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
            double[][] prefHeights = computeChildrenHeights(managed, -1, false);
            adjustChildrenHeights(managed, prefHeights, height, -1);
            contentWidth = computeMaxPrefAreaWidth(managed, marginAccessor, prefHeights[0], false);
        } else {
            contentWidth = computeMaxPrefAreaWidth(managed, marginAccessor);
        }
        return snapSpaceX(insets.getLeft()) + contentWidth + snapSpaceX(insets.getRight());
    }

    @Override protected double computePrefHeight(double width) {
        Insets insets = getInsets();
        return snapSpaceY(insets.getTop()) +
               computeContentHeight(getManagedChildren(), width, false) +
               snapSpaceY(insets.getBottom());
    }

    /**
     * Calculates the preferred or minimum height for each child.
     * The returned heights are snapped to pixels in the vertical direction.
     */
    private double[][] computeChildrenHeights(List<Node> managed, double width, boolean minimum) {
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

    /**
     * Adjusts the children heights (within their min-max limits) to fit the provided space.
     * This is necessary when the VBox is constrained to be larger or smaller than the combined preferred
     * heights of its children. In this case, we grow or shrink the children until they fit the VBox exactly.
     *
     * @return the pixel-snapped content height, which is the combined height
     *         of all children as well as the spacing between them
     */
    private double adjustChildrenHeights(List<Node> managed, double[][] childrenHeights, double height, double width) {
        Insets insets = getInsets();
        double left = snapSpaceX(insets.getLeft());
        double right = snapSpaceX(insets.getRight());

        double refWidth = isFillWidth() && width != -1 ? width - left - right : -1;
        double totalSpacing = (managed.size() - 1) * snapSpaceY(getSpacing());
        double contentHeight = snappedSum(childrenHeights[0], managed.size()) + totalSpacing;
        double targetHeight = height - snapSpaceY(insets.getTop()) - snapSpaceY(insets.getBottom());

        if (contentHeight < targetHeight) {
            growChildrenHeights(managed, childrenHeights, targetHeight, refWidth);
        } else if (contentHeight > targetHeight) {
            shrinkChildrenHeights(managed, childrenHeights, targetHeight, refWidth);
        }

        return snappedSum(childrenHeights[0], managed.size()) + totalSpacing;
    }

    /**
     * Shrinks all children heights to fit the target height.
     * In contrast to growing, shrinking does not require two phases of processing.
     */
    private void shrinkChildrenHeights(List<Node> managed, double[][] childrenHeights, double targetHeight, double width) {
        double[] usedHeights = childrenHeights[0];
        double[] minHeights = childrenHeights[1];

        for (int i = 0, size = managed.size(); i < size; i++) {
            final Node child = managed.get(i);
            minHeights[i] = computeChildMinAreaHeight(child, -1, getMargin(child), width);
        }

        adjustHeightsWithinLimits(managed, usedHeights, minHeights, targetHeight, managed.size());
    }

    /**
     * Grows all children heights to fit the target height.
     * Growing is a two-phase process: first, only children with {@link Priority#ALWAYS} are eligible
     * for adjustment. If the first adjustment didn't suffice to fit the target height, children with
     * {@link Priority#SOMETIMES} are also eligible for adjustment.
     */
    private void growChildrenHeights(List<Node> managed, double[][] childrenHeights, double targetHeight, double width) {
        double[] currentHeights = childrenHeights[0];
        double[] maxHeights = childrenHeights[1];

        for (Priority priority : GROW_PRIORITY) {
            int adjustingNumber = 0;

            for (int i = 0, size = managed.size(); i < size; i++) {
                final Node child = managed.get(i);

                // If the child is eligible to grow (as indicated by its vertical grow priority),
                // we count it towards the 'adjustingNumber', which represents the number of children
                // that can grow in this phase.
                if (getVgrow(child) == priority) {
                    maxHeights[i] = computeChildMaxAreaHeight(child, -1, getMargin(child), width);
                    ++adjustingNumber;
                } else {
                    maxHeights[i] = -1;
                }
            }

            // Adjust the children that are eligible in this phase and return early if the children
            // fit the target height (so no second phase is required).
            if (adjustHeightsWithinLimits(managed, currentHeights, maxHeights, targetHeight, adjustingNumber)) {
                return;
            }
        }
    }

    /**
     * Resizes the children heights to fit the target height, while taking into account the resize limits
     * for each child (their minimum and maximum height). This method will be called once when shrinking,
     * and may be called twice when growing.
     *
     * @param managed the managed children
     * @param currentHeights the current children heights
     * @param limitHeights the max or min heights for each child, depending on whether we are growing or shrinking;
     *                     a value of -1 means the child cannot be resized
     * @param targetHeight sum of children heights and spacing
     * @param adjustingNumber a number that indicates how many children can be resized
     * @return {@code true} if the children heights were successfully resized to fit the target height;
     *         {@code false} otherwise
     */
    private boolean adjustHeightsWithinLimits(
            List<Node> managed, double[] currentHeights, double[] limitHeights, double targetHeight, int adjustingNumber) {
        double totalSpacing = (managed.size() - 1) * snapSpaceY(getSpacing());

        // Current total height and current delta are two important numbers that we continuously
        // update as this method converges towards a solution.
        double currentTotalHeight = snappedSum(currentHeights, managed.size()) + totalSpacing;
        double currentDelta = targetHeight - currentTotalHeight;

        // We repeatedly apply the following algorithm as long as we have space left to
        // distribute (currentDelta), as well as children that are eligible to grow or
        // shrink (adjustingNumber).
        while ((currentDelta > Double.MIN_VALUE || currentDelta < -Double.MIN_VALUE) && adjustingNumber > 0) {
            // The amount of space that, in the ideal case, we need to add to or subtract from
            // each eligible child in order to fit the children into the target height.
            double idealChange = snapPortionY(currentDelta / adjustingNumber);

            for (int i = managed.size() - 1; i >= 0; i--) {
                // If the child is not eligible for adjustment, skip it.
                if (limitHeights[i] == -1) {
                    continue;
                }

                // The actual amount of space that we add to or remove from the child is restricted
                // by its minimum and maximum height.
                double maxChange = limitHeights[i] - currentHeights[i];
                double actualChange = currentDelta > 0 ? Math.min(maxChange, idealChange) : Math.max(maxChange, idealChange);
                double oldHeight = currentHeights[i];

                // Update the child height and snap the updated height to pixels in the vertical direction.
                // Since snapping affects the total height, we need to recompute the current total height to
                // know how much space we have left to distribute.
                currentHeights[i] = snapSizeY(currentHeights[i] + actualChange);
                currentTotalHeight = snappedSum(currentHeights, managed.size()) + totalSpacing;

                // Update the amount of space we still need to grow or shrink (currentDelta) for the
                // remaining children. If we overshoot our target, we're done because we can't resize
                // any further.
                double newDelta = targetHeight - currentTotalHeight;
                if (Math.abs(newDelta) > Math.abs(currentDelta)) {
                    currentHeights[i] = oldHeight;
                    return true;
                }

                currentDelta = newDelta;

                // If the actual change for the current child was restricted (as evidenced by its smaller
                // magnitude when compared to the ideal change), we've reached the limit for this child and
                // need to exclude it from further consideration.
                if (Math.abs(actualChange) < Math.abs(idealChange)) {
                    limitHeights[i] = -1;
                    adjustingNumber--;
                }
            }
        }

        return false;
    }

    /**
     * Calculates the preferred or minimum content height.
     * The content height is the total preferred or minimum height of all children,
     * including spacing between the children.
     */
    private double computeContentHeight(List<Node> managedChildren, double width, boolean minimum) {
        return snappedSum(computeChildrenHeights(managedChildren, width, minimum)[0], managedChildren.size())
                + (managedChildren.size()-1)*snapSpaceY(getSpacing());
    }

    /**
     * Calculates the sum of the double values, and snaps the result
     * to the nearest pixel in the vertical direction.
     */
    private double snappedSum(double[] array, int size) {
        double res = 0;
        for (int i = 0; i < size; ++i) {
            res += array[i];
        }
        return snapSpaceY(res);
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

        double[][] actualChildrenHeights = computeChildrenHeights(managed, width, false);
        double contentWidth = width - left - right;
        double contentHeight = adjustChildrenHeights(managed, actualChildrenHeights, height, width);

        double x = left;
        double y = top + computeYOffset(height - top - bottom, contentHeight, vpos);

        for (int i = 0, size = managed.size(); i < size; i++) {
            Node child = managed.get(i);
            layoutInArea(child, x, y, contentWidth, actualChildrenHeights[0][i],
                       /* baseline shouldn't matter */actualChildrenHeights[0][i],
                       getMargin(child), isFillWidth, true,
                       hpos, vpos);
            y += actualChildrenHeights[0][i] + space;
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

    /* *************************************************************************
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

}
