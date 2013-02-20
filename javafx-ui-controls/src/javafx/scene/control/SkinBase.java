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
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.css.Styleable;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 *
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
    private static final EventHandler<MouseEvent> mouseEventConsumer = new EventHandler<MouseEvent>() {
        @Override public void handle(MouseEvent event) {
            /*
            ** we used to consume mouse wheel rotations here, 
            ** be we've switched to ScrollEvents, and only consume those which we use.
            ** See RT-13995 & RT-14480
            */
            event.consume();
        }
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
        
        // RT-28337: request layout on prefWidth / prefHeight changes
        InvalidationListener prefSizeListener = new InvalidationListener() {
            @Override public void invalidated(Observable o) {
                control.requestLayout();
            }
        };
        this.control.prefWidthProperty().addListener(prefSizeListener);
        this.control.prefHeightProperty().addListener(prefSizeListener);
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
        // By default simply sizes all children to fit within the space provided
        for (int i=0, max=children.size(); i<max; i++) {
            Node child = children.get(i);
            layoutInArea(child, contentX, contentY, contentWidth, contentHeight, -1, HPos.CENTER, VPos.CENTER);
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
     * @return A double representing the minimum width of this Skin.
     */
    protected double computeMinWidth(double height) {
        return control.prefWidth(height);
    }

    /**
     * Computes the minimum allowable height of the Skin, based on the provided
     * width.
     * 
     * @param width The width of the Skin, in case this value might dictate
     *      the minimum height.
     * @return A double representing the minimum height of this Skin.
     */
    protected double computeMinHeight(double width) {
        return control.prefHeight(width);
    }

    /**
     * Computes the maximum allowable width of the Skin, based on the provided
     * height.
     * 
     * @param height The height of the Skin, in case this value might dictate
     *      the maximum width.
     * @return A double representing the maximum width of this Skin.
     */
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }
    
    /**
     * Computes the maximum allowable height of the Skin, based on the provided
     * width.
     * 
     * @param width The width of the Skin, in case this value might dictate
     *      the maximum height.
     * @return A double representing the maximum height of this Skin.
     */
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }
    
    // PENDING_DOC_REVIEW
    /**
     * Calculates the preferred width of this {@code SkinBase}. The default
     * implementation calculates this width as the width of the area occupied
     * by its managed children when they are positioned at their
     * current positions at their preferred widths.
     *
     * @param height the height that should be used if preferred width depends
     *      on it
     * @return the calculated preferred width
     */
    protected double computePrefWidth(double height) {
        double minX = 0;
        double maxX = 0;
        for (int i = 0; i < children.size(); i++) {
            Node node = children.get(i);
            if (node.isManaged()) {
                final double x = node.getLayoutBounds().getMinX() + node.getLayoutX();
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x + node.prefWidth(-1));
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
     * @param width the width that should be used if preferred height depends
     *      on it
     * @return the calculated preferred height
     */
    protected double computePrefHeight(double width) {
        double minY = 0;
        double maxY = 0;
        for (int i = 0; i < children.size(); i++) {
            Node node = children.get(i);
            if (node.isManaged()) {
                final double y = node.getLayoutBounds().getMinY() + node.getLayoutY();
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y + node.prefHeight(-1));
            }
        }
        return maxY - minY;
    }
    
    /**
     * Calculates the baseline offset based on the first managed child. If there
     * is no such child, returns {@link Node#getBaselineOffset()}.
     *
     * @return baseline offset
     */
    public double getBaselineOffset() {
        int size = children.size();
        for (int i = 0; i < size; ++i) {
            Node child = children.get(i);
            if (child.isManaged()) {
                return child.getLayoutBounds().getMinY() + child.getLayoutY() + child.getBaselineOffset();
            }
        }
        return control.getLayoutBounds().getHeight();
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * (Mostly ugly) Skin -> Control forwarding API                            *
     *                                                                         *
     **************************************************************************/      
    
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
    
    protected void positionInArea(Node child, double areaX, double areaY, 
            double areaWidth, double areaHeight, double areaBaselineOffset, 
            HPos halignment, VPos valignment) {
        positionInArea(child, areaX, areaY, areaWidth, areaHeight, 
                areaBaselineOffset, Insets.EMPTY, halignment, valignment);
    }
    
    protected void positionInArea(Node child, double areaX, double areaY, 
            double areaWidth, double areaHeight, double areaBaselineOffset, 
            Insets margin, HPos halignment, VPos valignment) {
        Region.positionInArea(child, areaX, areaY, areaWidth, areaHeight, 
                areaBaselineOffset, margin, halignment, valignment, 
                control.isSnapToPixel());
    }
    
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, 
                Insets.EMPTY, true, true, halignment, valignment);
    }
    
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               Insets margin,
                               HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset,
                margin, true, true, halignment, valignment);
    }
    
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
     * Testing-only API                                                        *
     *                                                                         *
     **************************************************************************/      
    
}
