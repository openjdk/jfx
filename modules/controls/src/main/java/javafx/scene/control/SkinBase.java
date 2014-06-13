/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import java.util.Collections;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.css.Styleable;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
//import javafx.scene.accessibility.Action;
//import javafx.scene.accessibility.Attribute;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * Base implementation class for defining the visual representation of user
 * interface controls by defining a scene graph of nodes to represent the
 * {@link Skin skin}.
 * A user interface control is abstracted behind the {@link Skinnable} interface.
 *
 * @since JavaFX 8.0
 */
public abstract class SkinBase<C extends Control> implements Skin<C> {
    
    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    /**
     * The {@code Control} that is referencing this Skin. There is a
     * one-to-one relationship between a {@code Skin} and a {@code Control}.
     * When a {@code Skin} is set on a {@code Control}, this variable is
     * automatically updated.
     */
    private C control;
    
    /**
     * A local field that directly refers to the children list inside the Control.
     */
    private ObservableList<Node> children;
    
    
    
    /***************************************************************************
     *                                                                         *
     * Event Handlers / Listeners                                              *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Mouse handler used for consuming all mouse events (preventing them
     * from bubbling up to parent)
     */
    private static final EventHandler<MouseEvent> mouseEventConsumer = event -> {
        /*
        ** we used to consume mouse wheel rotations here,
        ** be we've switched to ScrollEvents, and only consume those which we use.
        ** See RT-13995 & RT-14480
        */
        event.consume();
    };
    
    
    
    /***************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Constructor for all SkinBase instances.
     * 
     * @param control The control for which this Skin should attach to.
     */
    protected SkinBase(final C control) {
        if (control == null) {
            throw new IllegalArgumentException("Cannot pass null for control");
        }

        // Update the control and behavior
        this.control = control;
        this.children = control.getControlChildren();
        
        // Default behavior for controls is to consume all mouse events
        consumeMouseEvents(true);
    }
    
    

    /***************************************************************************
     *                                                                         *
     * Public API (from Skin)                                                  *
     *                                                                         *
     **************************************************************************/    

    /** {@inheritDoc} */
    @Override public final C getSkinnable() {
        return control;
    }

    /** {@inheritDoc} */
    @Override public final Node getNode() {
        return control; 
    }

    /** {@inheritDoc} */
    @Override public void dispose() { 
//        control.removeEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, contextMenuHandler);

        this.control = null;
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/     
    
    /**
     * Returns the children of the skin.
     */
    public final ObservableList<Node> getChildren() {
        return children;
    }
    
    /**
     * Called during the layout pass of the scenegraph. 
     */
    protected void layoutChildren(final double contentX, final double contentY,
            final double contentWidth, final double contentHeight) {
        // By default simply sizes all managed children to fit within the space provided
        for (int i=0, max=children.size(); i<max; i++) {
            Node child = children.get(i);
            if (child.isManaged()) {
                layoutInArea(child, contentX, contentY, contentWidth, contentHeight, -1, HPos.CENTER, VPos.CENTER);
            }
        }
    }
    
    /**
     * Determines whether all mouse events should be automatically consumed.
     */
    protected final void consumeMouseEvents(boolean value) {
        if (value) {
            control.addEventHandler(MouseEvent.ANY, mouseEventConsumer);
        } else {
            control.removeEventHandler(MouseEvent.ANY, mouseEventConsumer);
        }
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public Layout-related API                                               *
     *                                                                         *
     **************************************************************************/
    
    /**
     * Computes the minimum allowable width of the Skin, based on the provided
     * height.
     *
     * @param height The height of the Skin, in case this value might dictate
     *      the minimum width.
     * @param topInset the pixel snapped top inset
     * @param rightInset the pixel snapped right inset
     * @param bottomInset the pixel snapped bottom inset
     * @param leftInset  the pixel snapped left inset
     * @return A double representing the minimum width of this Skin.
     */
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {

        double minX = 0;
        double maxX = 0;
        boolean firstManagedChild = true;
        for (int i = 0; i < children.size(); i++) {
            Node node = children.get(i);
            if (node.isManaged()) {
                final double x = node.getLayoutBounds().getMinX() + node.getLayoutX();
                if (!firstManagedChild) {  // branch prediction favors most often used condition
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x + node.minWidth(-1));
                } else {
                    minX = x;
                    maxX = x + node.minWidth(-1);
                    firstManagedChild = false;
                }
            }
        }
        double minWidth = maxX - minX;
        return leftInset + minWidth + rightInset;
    }

    /**
     * Computes the minimum allowable height of the Skin, based on the provided
     * width.
     *
     * @param width The width of the Skin, in case this value might dictate
     *      the minimum height.
     * @param topInset the pixel snapped top inset
     * @param rightInset the pixel snapped right inset
     * @param bottomInset the pixel snapped bottom inset
     * @param leftInset  the pixel snapped left inset
     * @return A double representing the minimum height of this Skin.
     */
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {

        double minY = 0;
        double maxY = 0;
        boolean firstManagedChild = true;
        for (int i = 0; i < children.size(); i++) {
            Node node = children.get(i);
            if (node.isManaged()) {
                final double y = node.getLayoutBounds().getMinY() + node.getLayoutY();
                if (!firstManagedChild) {  // branch prediction favors most often used condition
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y + node.minHeight(-1));
                } else {
                    minY = y;
                    maxY = y + node.minHeight(-1);
                    firstManagedChild = false;
                }
            }
        }
        double minHeight = maxY - minY;
        return topInset + minHeight + bottomInset;
    }

    /**
     * Computes the maximum allowable width of the Skin, based on the provided
     * height.
     *
     * @param height The height of the Skin, in case this value might dictate
     *      the maximum width.
     * @param topInset the pixel snapped top inset
     * @param rightInset the pixel snapped right inset
     * @param bottomInset the pixel snapped bottom inset
     * @param leftInset  the pixel snapped left inset
     * @return A double representing the maximum width of this Skin.
     */
    protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Double.MAX_VALUE;
    }
    
    /**
     * Computes the maximum allowable height of the Skin, based on the provided
     * width.
     *
     * @param width The width of the Skin, in case this value might dictate
     *      the maximum height.
     * @param topInset the pixel snapped top inset
     * @param rightInset the pixel snapped right inset
     * @param bottomInset the pixel snapped bottom inset
     * @param leftInset  the pixel snapped left inset
     * @return A double representing the maximum height of this Skin.
     */
    protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Double.MAX_VALUE;
    }
    
    // PENDING_DOC_REVIEW
    /**
     * Calculates the preferred width of this {@code SkinBase}. The default
     * implementation calculates this width as the width of the area occupied
     * by its managed children when they are positioned at their
     * current positions at their preferred widths.
     *
     * @param height the height that should be used if preferred width depends on it
     * @param topInset the pixel snapped top inset
     * @param rightInset the pixel snapped right inset
     * @param bottomInset the pixel snapped bottom inset
     * @param leftInset  the pixel snapped left inset
     * @return the calculated preferred width
     */
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {

        double minX = 0;
        double maxX = 0;
        boolean firstManagedChild = true;
        for (int i = 0; i < children.size(); i++) {
            Node node = children.get(i);
            if (node.isManaged()) {
                final double x = node.getLayoutBounds().getMinX() + node.getLayoutX();
                if (!firstManagedChild) {  // branch prediction favors most often used condition
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x + node.prefWidth(-1));
                } else {
                    minX = x;
                    maxX = x + node.prefWidth(-1);
                    firstManagedChild = false;
                }
            }
        }
        return maxX - minX;
    }
    
    // PENDING_DOC_REVIEW
    /**
     * Calculates the preferred height of this {@code SkinBase}. The default
     * implementation calculates this height as the height of the area occupied
     * by its managed children when they are positioned at their current
     * positions at their preferred heights.
     *
     * @param width the width that should be used if preferred height depends on it
     * @param topInset the pixel snapped top inset
     * @param rightInset the pixel snapped right inset
     * @param bottomInset the pixel snapped bottom inset
     * @param leftInset  the pixel snapped left inset
     * @return the calculated preferred height
     */
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {

        double minY = 0;
        double maxY = 0;
        boolean firstManagedChild = true;
        for (int i = 0; i < children.size(); i++) {
            Node node = children.get(i);
            if (node.isManaged()) {
                final double y = node.getLayoutBounds().getMinY() + node.getLayoutY();
                if (!firstManagedChild) {  // branch prediction favors most often used condition
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y + node.prefHeight(-1));
                } else {
                    minY = y;
                    maxY = y + node.prefHeight(-1);
                    firstManagedChild = false;
                }
            }
        }
        return maxY - minY;
    }
    
    /**
     * Calculates the baseline offset based on the first managed child. If there
     * is no such child, returns {@link Node#getBaselineOffset()}.
     *
     * @param topInset the pixel snapped top inset
     * @param rightInset the pixel snapped right inset
     * @param bottomInset the pixel snapped bottom inset
     * @param leftInset  the pixel snapped left inset
     * @return baseline offset
     */
    protected double computeBaselineOffset(double topInset, double rightInset, double bottomInset, double leftInset) {
        int size = children.size();
        for (int i = 0; i < size; ++i) {
            Node child = children.get(i);
            if (child.isManaged()) {
                double offset = child.getBaselineOffset();
                if (offset == Node.BASELINE_OFFSET_SAME_AS_HEIGHT) {
                    continue;
                }
                return child.getLayoutBounds().getMinY() + child.getLayoutY() + offset;
            }
        }
        return Node.BASELINE_OFFSET_SAME_AS_HEIGHT;
    }

    
    /***************************************************************************
     *                                                                         *
     * (Mostly ugly) Skin -> Control forwarding API                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Utility method to get the top inset which includes padding and border
     * inset. Then snapped to whole pixels if getSkinnable().isSnapToPixel() is true.
     *
     * @return Rounded up insets top
     */
    protected double snappedTopInset() {
        return control.snappedTopInset();
    }

    /**
     * Utility method to get the bottom inset which includes padding and border
     * inset. Then snapped to whole pixels if getSkinnable().isSnapToPixel() is true.
     *
     * @return Rounded up insets bottom
     */
    protected double snappedBottomInset() {
        return control.snappedBottomInset();
    }

    /**
     * Utility method to get the left inset which includes padding and border
     * inset. Then snapped to whole pixels if getSkinnable().isSnapToPixel() is true.
     *
     * @return Rounded up insets left
     */
    protected double snappedLeftInset() {
        return control.snappedLeftInset();
    }

    /**
     * Utility method to get the right inset which includes padding and border
     * inset. Then snapped to whole pixels if getSkinnable().isSnapToPixel() is true.
     *
     * @return Rounded up insets right
     */
    protected double snappedRightInset() {
        return control.snappedRightInset();
    }

    /**
     * If this region's snapToPixel property is true, returns a value rounded
     * to the nearest pixel, else returns the same value.
     * @param value the space value to be snapped
     * @return value rounded to nearest pixel
     */
    protected double snapSpace(double value) {
        return control.isSnapToPixel() ? Math.round(value) : value;
    }
    
    /**
     * If this region's snapToPixel property is true, returns a value ceiled
     * to the nearest pixel, else returns the same value.
     * @param value the size value to be snapped
     * @return value ceiled to nearest pixel
     */
    protected double snapSize(double value) {
        return control.isSnapToPixel() ? Math.ceil(value) : value;
    }

    /**
     * If this region's snapToPixel property is true, returns a value rounded
     * to the nearest pixel, else returns the same value.
     * @param value the position value to be snapped
     * @return value rounded to nearest pixel
     */
    protected double snapPosition(double value) {
        return control.isSnapToPixel() ? Math.round(value) : value;
    }

    /**
     * Utility method which positions the child within an area of this
     * skin defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * This function does <i>not</i> resize the node and uses the node's layout bounds
     * width and height to determine how it should be positioned within the area.
     * <p>
     * If the vertical alignment is {@code VPos.BASELINE} then it
     * will position the node so that its own baseline aligns with the passed in
     * {@code baselineOffset}, otherwise the baseline parameter is ignored.
     * <p>
     * If {@code snapToPixel} is {@code true} for this skin, then the x/y position
     * values will be rounded to their nearest pixel boundaries.
     *
     * @param child the child being positioned within this skin
     * @param areaX the horizontal offset of the layout area relative to this skin
     * @param areaY the vertical offset of the layout area relative to this skin
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     *
     */
    protected void positionInArea(Node child, double areaX, double areaY, 
            double areaWidth, double areaHeight, double areaBaselineOffset, 
            HPos halignment, VPos valignment) {
        positionInArea(child, areaX, areaY, areaWidth, areaHeight, 
                areaBaselineOffset, Insets.EMPTY, halignment, valignment);
    }

    /**
     * Utility method which positions the child within an area of this
     * skin defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * This function does <i>not</i> resize the node and uses the node's layout bounds
     * width and height to determine how it should be positioned within the area.
     * <p>
     * If the vertical alignment is {@code VPos.BASELINE} then it
     * will position the node so that its own baseline aligns with the passed in
     * {@code baselineOffset},  otherwise the baseline parameter is ignored.
     * <p>
     * If {@code snapToPixel} is {@code true} for this skin, then the x/y position
     * values will be rounded to their nearest pixel boundaries.
     * <p>
     * If {@code margin} is non-null, then that space will be allocated around the
     * child within the layout area.  margin may be null.
     *
     * @param child the child being positioned within this skin
     * @param areaX the horizontal offset of the layout area relative to this skin
     * @param areaY the vertical offset of the layout area relative to this skin
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param margin the margin of space to be allocated around the child
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     *
     * @since JavaFX 8.0
     */
    protected void positionInArea(Node child, double areaX, double areaY,
            double areaWidth, double areaHeight, double areaBaselineOffset, 
            Insets margin, HPos halignment, VPos valignment) {
        Region.positionInArea(child, areaX, areaY, areaWidth, areaHeight, 
                areaBaselineOffset, margin, halignment, valignment, 
                control.isSnapToPixel());
    }

    /**
     * Utility method which lays out the child within an area of this
     * skin defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * If the child is resizable, this method will resize it to fill the specified
     * area unless the node's maximum size prevents it.  If the node's maximum
     * size preference is less than the area size, the maximum size will be used.
     * If node's maximum is greater than the area size, then the node will be
     * resized to fit within the area, unless its minimum size prevents it.
     * <p>
     * If the child has a non-null contentBias, then this method will use it when
     * resizing the child.  If the contentBias is horizontal, it will set its width
     * first to the area's width (up to the child's max width limit) and then pass
     * that value to compute the child's height.  If child's contentBias is vertical,
     * then it will set its height to the area height (up to child's max height limit)
     * and pass that height to compute the child's width.  If the child's contentBias
     * is null, then it's width and height have no dependencies on each other.
     * <p>
     * If the child is not resizable (Shape, Group, etc) then it will only be
     * positioned and not resized.
     * <p>
     * If the child's resulting size differs from the area's size (either
     * because it was not resizable or it's sizing preferences prevented it), then
     * this function will align the node relative to the area using horizontal and
     * vertical alignment values.
     * If valignment is {@code VPos.BASELINE} then the node's baseline will be aligned
     * with the area baseline offset parameter, otherwise the baseline parameter
     * is ignored.
     * <p>
     * If {@code snapToPixel} is {@code true} for this skin, then the resulting x,y
     * values will be rounded to their nearest pixel boundaries and the
     * width/height values will be ceiled to the next pixel boundary.
     *
     * @param child the child being positioned within this skin
     * @param areaX the horizontal offset of the layout area relative to this skin
     * @param areaY the vertical offset of the layout area relative to this skin
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     *
     */
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, 
                Insets.EMPTY, true, true, halignment, valignment);
    }

    /**
     * Utility method which lays out the child within an area of this
     * skin defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * If the child is resizable, this method will resize it to fill the specified
     * area unless the node's maximum size prevents it.  If the node's maximum
     * size preference is less than the area size, the maximum size will be used.
     * If node's maximum is greater than the area size, then the node will be
     * resized to fit within the area, unless its minimum size prevents it.
     * <p>
     * If the child has a non-null contentBias, then this method will use it when
     * resizing the child.  If the contentBias is horizontal, it will set its width
     * first to the area's width (up to the child's max width limit) and then pass
     * that value to compute the child's height.  If child's contentBias is vertical,
     * then it will set its height to the area height (up to child's max height limit)
     * and pass that height to compute the child's width.  If the child's contentBias
     * is null, then it's width and height have no dependencies on each other.
     * <p>
     * If the child is not resizable (Shape, Group, etc) then it will only be
     * positioned and not resized.
     * <p>
     * If the child's resulting size differs from the area's size (either
     * because it was not resizable or it's sizing preferences prevented it), then
     * this function will align the node relative to the area using horizontal and
     * vertical alignment values.
     * If valignment is {@code VPos.BASELINE} then the node's baseline will be aligned
     * with the area baseline offset parameter, otherwise the baseline parameter
     * is ignored.
     * <p>
     * If {@code margin} is non-null, then that space will be allocated around the
     * child within the layout area.  margin may be null.
     * <p>
     * If {@code snapToPixel} is {@code true} for this skin, then the resulting x,y
     * values will be rounded to their nearest pixel boundaries and the
     * width/height values will be ceiled to the next pixel boundary.
     *
     * @param child the child being positioned within this skin
     * @param areaX the horizontal offset of the layout area relative to this skin
     * @param areaY the vertical offset of the layout area relative to this skin
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param margin the margin of space to be allocated around the child
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     */
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               Insets margin,
                               HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset,
                margin, true, true, halignment, valignment);
    }

    /**
     * Utility method which lays out the child within an area of this
     * skin defined by {@code areaX}, {@code areaY}, {@code areaWidth} x {@code areaHeight},
     * with a baseline offset relative to that area.
     * <p>
     * If the child is resizable, this method will use {@code fillWidth} and {@code fillHeight}
     * to determine whether to resize it to fill the area or keep the child at its
     * preferred dimension.  If fillWidth/fillHeight are true, then this method
     * will only resize the child up to its max size limits.  If the node's maximum
     * size preference is less than the area size, the maximum size will be used.
     * If node's maximum is greater than the area size, then the node will be
     * resized to fit within the area, unless its minimum size prevents it.
     * <p>
     * If the child has a non-null contentBias, then this method will use it when
     * resizing the child.  If the contentBias is horizontal, it will set its width
     * first and then pass that value to compute the child's height.  If child's
     * contentBias is vertical, then it will set its height first
     * and pass that value to compute the child's width.  If the child's contentBias
     * is null, then it's width and height have no dependencies on each other.
     * <p>
     * If the child is not resizable (Shape, Group, etc) then it will only be
     * positioned and not resized.
     * <p>
     * If the child's resulting size differs from the area's size (either
     * because it was not resizable or it's sizing preferences prevented it), then
     * this function will align the node relative to the area using horizontal and
     * vertical alignment values.
     * If valignment is {@code VPos.BASELINE} then the node's baseline will be aligned
     * with the area baseline offset parameter, otherwise the baseline parameter
     * is ignored.
     * <p>
     * If {@code margin} is non-null, then that space will be allocated around the
     * child within the layout area.  margin may be null.
     * <p>
     * If {@code snapToPixel} is {@code true} for this skin, then the resulting x,y
     * values will be rounded to their nearest pixel boundaries and the
     * width/height values will be ceiled to the next pixel boundary.
     *
     * @param child the child being positioned within this skin
     * @param areaX the horizontal offset of the layout area relative to this skin
     * @param areaY the vertical offset of the layout area relative to this skin
     * @param areaWidth  the width of the layout area
     * @param areaHeight the height of the layout area
     * @param areaBaselineOffset the baseline offset to be used if VPos is BASELINE
     * @param margin the margin of space to be allocated around the child
     * @param fillWidth whether or not the child should be resized to fill the area width or kept to its preferred width
     * @param fillHeight whether or not the child should e resized to fill the area height or kept to its preferred height
     * @param halignment the horizontal alignment for the child within the area
     * @param valignment the vertical alignment for the child within the area
     */
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               Insets margin, boolean fillWidth, boolean fillHeight,
                               HPos halignment, VPos valignment) {
        Region.layoutInArea(child, areaX, areaY, areaWidth, areaHeight, 
                areaBaselineOffset, margin, fillWidth, fillHeight, halignment, 
                valignment, control.isSnapToPixel());
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/     
    
    
    
     /**************************************************************************
      *                                                                        *
      * Specialization of CSS handling code                                    *
      *                                                                        *
     **************************************************************************/

    private static class StyleableProperties {

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            STYLEABLES = Collections.unmodifiableList(Control.getClassCssMetaData());
        }
    }

    /** 
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return SkinBase.StyleableProperties.STYLEABLES;
    }

    /**
     * This method should delegate to {@link Node#getClassCssMetaData()} so that
     * a Node's CssMetaData can be accessed without the need for reflection.
     * @return The CssMetaData associated with this node, which may include the
     * CssMetaData of its super classes.
     */
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }
    
    /** @see Node#pseudoClassStateChanged */
    public final void pseudoClassStateChanged(PseudoClass pseudoClass, boolean active) {
        Control ctl = getSkinnable();
        if (ctl != null) {
            ctl.pseudoClassStateChanged(pseudoClass, active);
        }
    }


    /***************************************************************************
     *                                                                         *
     * Accessibility handling                                                  *
     *                                                                         *
     **************************************************************************/

//    /** @treatAsPrivate */
//    protected Object accGetAttribute(Attribute attribute, Object... parameters) {
//        return null;
//    }
//
//    /** @treatAsPrivate */
//    protected void accExecuteAction(Action action, Object... parameters) {
//    }

    /***************************************************************************
     *                                                                         *
     * Testing-only API                                                        *
     *                                                                         *
     **************************************************************************/

}
