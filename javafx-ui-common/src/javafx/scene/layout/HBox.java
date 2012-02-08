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

import com.sun.javafx.css.*;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.WritableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;



/**
 * HBox lays out its children in a single horizontal row.
 * If the hbox has a border and/or padding set, then the contents will be layed
 * out within those insets.
 * <p>
 * HBox example:
 * <pre><code>
 *     HBox hbox = new HBox(8); // spacing = 8
 *     hbox.getChildren().addAll(new Label("Name:), new TextBox());
 * </code></pre>
 *
 * HBox will resize children (if resizable) to their preferred widths and uses its
 * fillHeight property to determine whether to resize their heights to
 * fill its own height or keep their heights to their preferred (fillHeight defaults to true).
 * The alignment of the content is controlled by the alignment property,
 * which defaulst to Pos.TOP_LEFT.
 * <p>
 * If an hbox is resized larger than its preferred width, by default it will keep
 * children to their preferred widths, leaving the extra space unused.  If an
 * application wishes to have one or more children be allocated that extra space
 * it may optionally set an hgrow constraint on the child.  See "Optional Layout
 * Constraints" for details.
 * <p>
 * HBox lays out each managed child regardless of the child's
 * visible property value; unmanaged children are ignored.</p>
 *
 * <h4>Resizable Range</h4>
 *
 * An hbox's parent will resize the hbox within the hbox's resizable range
 * during layout.   By default the hbox computes this range based on its content
 * as outlined in the table below.
 * <table border="1">
 * <tr><td></td><th>width</th><th>height</th></tr>
 * <tr><th>minimum</th>
 * <td>left/right insets plus the sum of each child's min width plus spacing between each child.</td>
 * <td>top/bottom insets plus the largest of the children's min heights.</td></tr>
 * <tr><th>preferred</th>
 * <td>left/right insets plus the sum of each child's pref width plus spacing between each child.</td>
 * <td>top/bottom insets plus the largest of the children's pref heights.</td></tr>
 * <tr><th>maximum</th>
 * <td>Double.MAX_VALUE</td><td>Double.MAX_VALUE</td></tr>
 * </table>
 * <p>
 * An hbox's unbounded maximum width and height are an indication to the parent that
 * it may be resized beyond its preferred size to fill whatever space is assigned
 * to it.
 * <p>
 * HBox provides properties for setting the size range directly.  These
 * properties default to the sentinel value USE_COMPUTED_SIZE, however the
 * application may set them to other values as needed:
 * <pre><code>
 *     <b>hbox.setPrefWidth(400);</b>
 * </code></pre>
 * Applications may restore the computed values by setting these properties back
 * to USE_COMPUTED_SIZE.
 * <p>
 * HBox does not clip its content by default, so it is possible that childrens'
 * bounds may extend outside its own bounds if a child's min size prevents it from
 * being fit within the hbox.</p>
 *
 * <h4>Optional Layout Constraints</h4>
 *
 * An application may set constraints on individual children to customize HBox's layout.
 * For each constraint, HBox provides a static method for setting it on the child.
 * <p>
 * <table border="1">
 * <tr><th>Constraint</th><th>Type</th><th>Description</th></tr>
 * <tr><td>hgrow</td><td>javafx.scene.layout.Priority</td><td>The horizontal grow priority for the child.</td></tr>
 * <tr><td>margin</td><td>javafx.geometry.Insets</td><td>Margin space around the outside of the child.</td></tr>
 * </table>
 * <p>
 * For example, if an hbox needs the TextField to be allocated all extra space:
 * <pre><code>
 *     HBox hbox = new HBox();
 *     TextField field = new TextField();
 *     <b>HBox.setHgrow(field, Priority.ALWAYS);</b>
 *     hbox.getChildren().addAll(new Label("Search:"), field, new Button("Go"));
 * </code></pre>
 *
 * If more than one child has the same grow priority set, then the hbox will
 * allocate equal amounts of space to each.  HBox will only grow a child up to
 * its maximum width, so if the child has a max width other than Double.MAX_VALUE,
 * the application may need to override the max to allow it to grow.
 * For example:
 * <pre><code>
 *     HBox hbox = new HBox();
 *     Button button1 = new Button("Add");
 *     Button button2 = new Button("Remove");
 *     <b>HBox.setHgrow(button1, Priority.ALWAYS);
 *     HBox.setHgrow(button2, Priority.ALWAYS);
 *     button1.setMaxWidth(Double.MAX_VALUE);
 *     button2.setMaxWidth(Double.MAX_VALUE);</b>
 *     hbox.getChildren().addAll(button1, button2);
 * </code></pre>
 */
public class HBox extends Pane {

    /********************************************************************
     *  BEGIN static methods
     ********************************************************************/
    private static final String MARGIN_CONSTRAINT = "hbox-margin";
    private static final String HGROW_CONSTRAINT = "hbox-hgrow";

    /**
     * Sets the horizontal grow priority for the child when contained by an hbox.
     * If set, the hbox will use the priority to allocate additional space if the
     * hbox is resized larger than it's preferred width.
     * If multiple hbox children have the same horizontal grow priority, then the
     * extra space will be split evening between them.
     * If no horizontal grow priority is set on a child, the hbox will never
     * allocate it additional horizontal space if available.
     * Setting the value to null will remove the constraint.
     * @param child the child of an hbox
     * @param value the horizontal grow priority for the child
     */
    public static void setHgrow(Node child, Priority value) {
        setConstraint(child, HGROW_CONSTRAINT, value);
    }

    /**
     * Returns the child's hgrow constraint if set.
     * @param child the child node of an hbox
     * @return the horizontal grow priority for the child or null if no priority was set
     */
    public static Priority getHgrow(Node child) {
        return (Priority)getConstraint(child, HGROW_CONSTRAINT);
    }

    /**
     * Sets the margin for the child when contained by an hbox.
     * If set, the hbox will layout the child with the margin space around it.
     * Setting the value to null will remove the constraint.
     * @param child the child mode of the hbox
     * @param value the margin of space around the child
     */
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN_CONSTRAINT, value);
    }

    /**
     * Returns the child's margin constraint if set.
     * @param child the child node of an hbox
     * @return the margin for the child or null if no margin was set
     */
    public static Insets getMargin(Node child) {
        return (Insets)getConstraint(child, MARGIN_CONSTRAINT);
    }

    /**
     * Removes all hbox constraints from the child node.
     * @param child the child node
     */
    public static void clearConstraints(Node child) {
        setHgrow(child, null);
        setMargin(child, null);
    }

    /********************************************************************
     *  END static methods
     ********************************************************************/

    /**
     * Creates an HBox layout with spacing = 0.
     */
    public HBox() {
        super();
    }

    /**
     * Creates an HBox layout with the specified spacing between children.
     * @param spacing the amount of horizontal space between each child
     */
    public HBox(double spacing) {
        this();
        setSpacing(spacing);
    }

    /**
     * The amount of horizontal space between each child in the hbox.
     */
    public final DoubleProperty spacingProperty() {
        if (spacing == null) {
            spacing = new StyleableDoubleProperty() {
                @Override
                public void invalidated() {
                    requestLayout();
                }
                
                @Override 
                public StyleableProperty getStyleableProperty () {
                    return StyleableProperties.SPACING;
                }

                @Override
                public Object getBean() {
                    return HBox.this;
                }

                @Override
                public String getName() {
                    return "spacing";
                }
            };
        }
        return spacing;
    }
    
    private DoubleProperty spacing;
    public final void setSpacing(double value) { spacingProperty().set(value); }
    public final double getSpacing() { return spacing == null ? 0 : spacing.get(); }

    /**
     * The overall alignment of children within the hbox's width and height.
     * If the vertical alignment value is BASELINE, then children will always be
     * resized to their preferred heights and the fillHeight property will be
     * ignored.
     */
    public final ObjectProperty<Pos> alignmentProperty() {
        if (alignment == null) {
            alignment = new StyleableObjectProperty<Pos>(Pos.TOP_LEFT) {
                @Override
                public void invalidated() {
                    requestLayout();
                }
                
                @Override 
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.ALIGNMENT;
                }

                @Override
                public Object getBean() {
                    return HBox.this;
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

    /**
     * Whether or not resizable children will be resized to fill the full height of the hbox
     * or be kept to their preferred height and aligned according to the <code>alignment</code>
     * vpos value.   Note that if the hbox vertical alignment is set to BASELINE, then this
     * property will be ignored and children will be resized to their preferred heights.
     */
    public final BooleanProperty fillHeightProperty() {
        if (fillHeight == null) {
            fillHeight = new StyleableBooleanProperty(true) {
                @Override
                public void invalidated() {
                    requestLayout();
                }
                                
                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.FILL_HEIGHT;
                }

                @Override
                public Object getBean() {
                    return HBox.this;
                }

                @Override
                public String getName() {
                    return "fillHeight";
                }
            };
        }
        return fillHeight;
    }
    
    private BooleanProperty fillHeight;
    public final void setFillHeight(boolean value) { fillHeightProperty().set(value); }
    public final boolean isFillHeight() { return fillHeight == null ? true : fillHeight.get(); }

    private boolean shouldFillHeight() {
        return isFillHeight() && getAlignment().getVpos() != VPos.BASELINE;
    }

    /**
     *
     * @return null unless one of its children has a content bias.
     */
    @Override public Orientation getContentBias() {
        for (int i = 0; i < getChildren().size(); i++) {
            Node child = getChildren().get(i);
            if (child.isManaged() && child.getContentBias() != null) {
                return child.getContentBias();
            }
        }
        return null;
    }

    @Override protected double computeMinWidth(double height) {
        Insets insets = getInsets();
        return snapSpace(insets.getLeft()) +
               computeContentWidth(getAreaWidths(getManagedChildren(), height, true)) +
               snapSpace(insets.getRight());
    }

    @Override protected double computeMinHeight(double width) {
        Insets insets = getInsets();
        List<Node>managed = getManagedChildren();
        double contentHeight = 0;
        if (getContentBias() == Orientation.HORIZONTAL) {
            // if width is different than preferred, then child widths may grow or shrink,
            // altering the height of any child with a horizontal contentBias.
            double minWidths[] = getAreaWidths(managed, -1, true);
            adjustAreaWidths(managed, minWidths, width, -1);
            contentHeight = computeMaxMinAreaHeight(managed, getMargins(managed), minWidths, getAlignment().getVpos());
        } else {
            contentHeight = computeMaxMinAreaHeight(managed, getMargins(managed), getAlignment().getVpos());
        }
        return snapSpace(insets.getTop()) +
               contentHeight +
               snapSpace(insets.getBottom());
    }

    @Override protected double computePrefWidth(double height) {
         Insets insets = getInsets();
         return snapSpace(insets.getLeft()) +
                computeContentWidth(getAreaWidths(getManagedChildren(), height, false)) +
                snapSpace(insets.getRight());
    }

    @Override protected double computePrefHeight(double width) {
        Insets insets = getInsets();
        List<Node>managed = getManagedChildren();
        double contentHeight = 0;
        if (getContentBias() == Orientation.HORIZONTAL) {
            // if width is different than preferred, then child widths may grow or shrink,
            // altering the height of any child with a horizontal contentBias.
            double prefWidths[] = getAreaWidths(managed, -1, false);
            adjustAreaWidths(managed, prefWidths, width, -1);
            contentHeight = computeMaxPrefAreaHeight(managed, getMargins(managed), prefWidths, getAlignment().getVpos());
        } else {
            contentHeight = computeMaxPrefAreaHeight(managed, getMargins(managed), getAlignment().getVpos());
        }
        return snapSpace(insets.getTop()) +
               contentHeight +
               snapSpace(insets.getBottom());
    }

    private Insets[] getMargins(List<Node>managed) {
        Insets margins[] = new Insets[managed.size()];
        for(int i = 0; i < margins.length; i++) {
            margins[i] = getMargin(managed.get(i));
        }
        return margins;
    }

    private double[] getAreaWidths(List<Node>managed, double height, boolean minimum) {
        // height could be -1
        double[] prefAreaWidths = new double [managed.size()];
        final double insideHeight = height == -1? -1 : height -
                                     snapSpace(getInsets().getTop()) - snapSpace(getInsets().getBottom());
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            Insets margin = getMargin(child);
            prefAreaWidths[i] = minimum?
                               computeChildMinAreaWidth(child, margin,
                                   shouldFillHeight()? insideHeight : child.minHeight(-1)) :
                                   computeChildPrefAreaWidth(child, margin,
                                       shouldFillHeight()? insideHeight : child.prefHeight(-1));
        }
        return prefAreaWidths;
    }

    private double adjustAreaWidths(List<Node>managed, double areaWidths[], double width, double height) {
        Insets insets = getInsets();
        double top = snapSpace(insets.getTop());
        double bottom = snapSpace(insets.getBottom());
        double space = snapSpace(getSpacing());

        double contentWidth = computeContentWidth(areaWidths);
        double extraWidth = (width == -1? prefWidth(-1) : width) -
                snapSpace(insets.getLeft()) - snapSpace(insets.getRight()) - contentWidth;

        if (extraWidth != 0) {            
            double remaining = growOrShrinkAreaWidths(managed, areaWidths, Priority.ALWAYS, extraWidth,
                    shouldFillHeight() && height != -1? height - top - bottom : -1);
            remaining = growOrShrinkAreaWidths(managed, areaWidths, Priority.SOMETIMES, remaining,
                    shouldFillHeight() && height != -1? height - top - bottom : -1);
            contentWidth += (extraWidth - remaining);
        }
        return contentWidth;
    }

    private double growOrShrinkAreaWidths(List<Node>managed, double areaWidths[], Priority priority, double extraWidth, double height) {
        final boolean shrinking = extraWidth < 0;
        List<Node> adjustList = new ArrayList<Node>();
        List<Node> adjusting = new ArrayList<Node>();

        for (int i = 0; i < managed.size(); i++) {
            final Node child = managed.get(i);
            if (shrinking || getHgrow(child) == priority) {
                adjustList.add(child);
                adjusting.add(child);
            }
        }

        double[] areaLimitWidths = new double[adjustList.size()];
        for (int i = 0; i < adjustList.size(); i++) {
            final Node child = adjustList.get(i);
            final Insets margin = getMargin(child);
            areaLimitWidths[i] = shrinking?
                computeChildMinAreaWidth(child, margin, height) : computeChildMaxAreaWidth(child, margin, height);
        }

        double available = extraWidth; // will be negative in shrinking case
        while (Math.abs(available) > 1.0 && adjusting.size() > 0) {
            Node[] adjusted = new Node[adjustList.size()];
            final double portion = available / adjusting.size(); // negative in shrinking case
            for (int i = 0; i < adjusting.size(); i++) {
                final Node child = adjusting.get(i);
                final int childIndex = managed.indexOf(child);
                final double limit = areaLimitWidths[adjustList.indexOf(child)] - areaWidths[childIndex]; // negative in shrinking case                
                final double change = Math.abs(limit) <= Math.abs(portion)? limit : portion;
                areaWidths[childIndex] += change;
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
        for (int i = 0; i < areaWidths.length; i++) {
            areaWidths[i] = snapSpace(areaWidths[i]);
        }
        return available; // might be negative in shrinking case
    }

    private double computeContentWidth(double[] widths) {
        double total = 0;
        for (double w : widths) {
            total += w;
        }
        return total + (widths.length-1)*snapSpace(getSpacing());
    }

    private double[] actualAreaWidths;

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

        actualAreaWidths = getAreaWidths(managed, height, false);
        double contentWidth = adjustAreaWidths(managed, actualAreaWidths, width, height);
        double contentHeight = height - top - bottom;

        double x = snapSpace(insets.getLeft()) + computeXOffset(width - left - right, contentWidth, getAlignment().getHpos());
        double y = snapSpace(insets.getTop());
        double baselineOffset = getAlignment().getVpos() == VPos.BASELINE ? getMaxBaselineOffset(managed)
                                    : height/2;

        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            Insets margin = getMargin(child);
            layoutInArea(child, x, y, actualAreaWidths[i], contentHeight,
                    baselineOffset, margin, true, shouldFillHeight(),
                    getAlignment().getHpos(), getAlignment().getVpos());
            x += actualAreaWidths[i] + space;
        }
    }


    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

     /**
      * Super-lazy instantiation pattern from Bill Pugh.
      * @treatasprivate implementation detail
      */
     private static class StyleableProperties {

         private static final StyleableProperty<HBox,Pos> ALIGNMENT = 
             new StyleableProperty<HBox,Pos>("-fx-alignment",
                 new EnumConverter<Pos>(Pos.class), 
                 Pos.TOP_LEFT) {

            @Override
            public boolean isSettable(HBox node) {
                return node.alignment == null || !node.alignment.isBound();
            }

            @Override
            public WritableValue<Pos> getWritableValue(HBox node) {
                return node.alignmentProperty();
            }
                     
         };
         
         private static final StyleableProperty<HBox,Boolean> FILL_HEIGHT = 
             new StyleableProperty<HBox,Boolean>("-fx-fill-height",
                 BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(HBox node) {
                return node.fillHeight == null ||
                        !node.fillHeight.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(HBox node) {
                return node.fillHeightProperty();
            }
                     
         };
         
         private static final StyleableProperty<HBox,Number> SPACING = 
             new StyleableProperty<HBox,Number>("-fx-spacing",
                 SizeConverter.getInstance(), 0.0){

            @Override
            public boolean isSettable(HBox node) {
                return node.spacing == null || !node.spacing.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(HBox node) {
                return node.spacingProperty();
            }
                     
         };

         private static final List<StyleableProperty> STYLEABLES;
         static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Pane.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                FILL_HEIGHT,
                ALIGNMENT,
                SPACING
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

     /**
      * Super-lazy instantiation pattern from Bill Pugh. StyleableProperties is referenced
      * no earlier (and therefore loaded no earlier by the class loader) than
      * the moment that  impl_CSS_STYLEABLES() is called.
      * @treatasprivate implementation detail
      * @deprecated This is an internal API that is not intended for use and will be removed in the next version
      */
     @Deprecated
     public static List<StyleableProperty> impl_CSS_STYLEABLES() {
         return HBox.StyleableProperties.STYLEABLES;
     }

}
