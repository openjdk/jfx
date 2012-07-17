/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import java.util.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 *
 */
public abstract class SkinBase<C extends Control, B extends BehaviorBase<C>> implements Skin<C> {
    
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
     * The {@link BehaviorBase} that encapsulates the interaction with the
     * {@link Control} from this {@code Skin}. The {@code Skin} does not modify
     * the {@code Control} directly, but rather redirects events into the
     * {@code BehaviorBase} which then handles the events by modifying internal state
     * and public state in the {@code Control}. Generally, specific
     * {@code Skin} implementations will require specific {@code BehaviorBase}
     * implementations. For example, a ButtonSkin might require a ButtonBehavior.
     */
    private B behavior;
    
    /**
     * An ObservableList of the Nodes that make up the skin. This list differs
     * from the Controls children list, in that it will be a subset (not including
     * children such as the tooltip). When this children list changes, we 
     * manually update the children of the Control itself, so that the Nodes
     * are part of the scenegraph (as the Skin is not a member of the scenegraph).
     */
    private ObservableList<Node> children;
    
    /**
     * This is part of the workaround introduced during delomboking. We probably will
     * want to adjust the way listeners are added rather than continuing to use this
     * map (although it doesn't really do much harm).
     */
    private Map<ObservableValue,String> propertyReferenceMap =
            new HashMap<ObservableValue,String>();
    
    /***************************************************************************
     *                                                                         *
     * Event Handlers / Listeners                                              *
     *                                                                         *
     **************************************************************************/     
    
    private final ChangeListener controlPropertyChangedListener = new ChangeListener() {
        @Override public void changed(ObservableValue property, Object oldValue, Object newValue) {
            handleControlPropertyChanged(propertyReferenceMap.get(property));
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
     * @param behavior The behavior for which this Skin should defer to.
     */
    protected SkinBase(final C control, final B behavior) {
        if (control == null || behavior == null) {
            throw new IllegalArgumentException("Cannot pass null for control or behavior");
        }

        // Update the control and behavior
        this.control = control;
        this.behavior = behavior;
    }
    
    

    /***************************************************************************
     *                                                                         *
     * Public API (from Skin)                                                  *
     *                                                                         *
     **************************************************************************/    

    /** {@inheritDoc} */
    @Override public C getSkinnable() {
        return control;
    }

    /** {@inheritDoc} */
    @Override public Node getNode() {
        return control; 
    }
    
    /** {@inheritDoc} */
    public B getBehavior() {
        return behavior;
    }

    /** {@inheritDoc} */
    @Override public void dispose() { 
        // unhook listeners
        for (ObservableValue value : propertyReferenceMap.keySet()) {
            value.removeListener(controlPropertyChangedListener);
        }

//        this.removeEventHandler(MouseEvent.MOUSE_ENTERED, mouseHandler);
//        this.removeEventHandler(MouseEvent.MOUSE_EXITED, mouseHandler);
//        this.removeEventHandler(MouseEvent.MOUSE_PRESSED, mouseHandler);
//        this.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouseHandler);
//        this.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mouseHandler);
//        
//        control.removeEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, contextMenuHandler);

        this.control = null;
        this.behavior = null;
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/     
    
    /**
     * Returns the children of the skin.
     */
    protected ObservableList<Node> getChildren() {
        if (children == null) {
            instantiateChildren();
        }
        return children;
    }
    
    /**
     * Called during the layout pass of the scenegraph. 
     */
    protected void layoutChildren() {
        // no-op
    }
    
    /**
     * Subclasses can invoke this method to register that we want to listen to
     * property change events for the given property.
     *
     * @param property
     * @param reference
     */
    protected final void registerChangeListener(ObservableValue property, String reference) {
        if (!propertyReferenceMap.containsKey(property)) {
            propertyReferenceMap.put(property, reference);
            property.addListener(new WeakChangeListener(controlPropertyChangedListener));
        }
    }
    
    /**
     * Skin subclasses will override this method to handle changes in corresponding
     * control's properties.
     */
    protected void handleControlPropertyChanged(String propertyReference) {
        // no-op
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
        return computePrefWidth(height);
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
        return computePrefHeight(width);
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
        return computePrefWidth(height);
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
        return computePrefHeight(width);
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
        for (int i = 0; i < getChildren().size(); i++) {
            Node node = getChildren().get(i);
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
        for (int i = 0; i < getChildren().size(); i++) {
            Node node = getChildren().get(i);
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
        return getSkinnable().getLayoutBounds().getHeight();
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Convenience API                                                         *
     *                                                                         *
     **************************************************************************/      
    
    /**
     * Calls requestLayout() on the Skinnable
     */
    protected void requestLayout() {
        getSkinnable().requestLayout();
    }
    
    /**
     * Calls getInsets() on the Skinnable
     */
    protected Insets getInsets() {
        return getSkinnable().getInsets();
    }
    
    /**
     * Calls getPadding() on the Skinnable
     */
    protected Insets getPadding() {
        return getSkinnable().getPadding();
    }
    
    /**
     * Calls getWidth() on the Skinnable
     */
    protected double getWidth() {
        return getSkinnable().getWidth();
    }
    
    /**
     * Calls getHeight() on the Skinnable
     */
    protected double getHeight() {
        return getSkinnable().getHeight();
    }    
    
    
    
    /***************************************************************************
     *                                                                         *
     * (Mostly ugly) Skin -> Control forwarding API                            *
     *                                                                         *
     **************************************************************************/      
    
//    /**
//     * If this region's snapToPixel property is true, returns a value rounded
//     * to the nearest pixel, else returns the same value.
//     * @param value the space value to be snapped
//     * @return value rounded to nearest pixel
//     */
    protected double snapSpace(double value) {
        return getSkinnable()._snapSpace(value);
    }
    
//    /**
//     * If this region's snapToPixel property is true, returns a value ceiled
//     * to the nearest pixel, else returns the same value.
//     * @param value the size value to be snapped
//     * @return value ceiled to nearest pixel
//     */
    protected double snapSize(double value) {
        return getSkinnable()._snapSize(value);
    }

//    /**
//     * If this region's snapToPixel property is true, returns a value rounded
//     * to the nearest pixel, else returns the same value.
//     * @param value the position value to be snapped
//     * @return value rounded to nearest pixel
//     */
    protected double snapPosition(double value) {
        return getSkinnable()._snapPosition(value);
    }
    
    protected void positionInArea(Node child, double areaX, double areaY, double areaWidth, double areaHeight, double areaBaselineOffset, HPos halignment, VPos valignment) {
        getSkinnable()._positionInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, halignment, valignment);
    }
    
    protected void positionInArea(Node child, double areaX, double areaY, double areaWidth, double areaHeight, double areaBaselineOffset, Insets margin, HPos halignment, VPos valignment) {
        getSkinnable()._positionInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, margin, halignment, valignment);
    }
    
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               HPos halignment, VPos valignment) {
        getSkinnable()._layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, halignment, valignment);
    }
    
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               Insets margin,
                               HPos halignment, VPos valignment) {
        getSkinnable()._layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, margin, halignment, valignment);
    }
    
    protected void layoutInArea(Node child, double areaX, double areaY,
                               double areaWidth, double areaHeight,
                               double areaBaselineOffset,
                               Insets margin, boolean fillWidth, boolean fillHeight,
                               HPos halignment, VPos valignment) {
        getSkinnable()._layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, margin, fillWidth, fillHeight, halignment, valignment);
    }
    
    protected void consumeMouseEvents(boolean consume) {
        getSkinnable().consumeMouseEvents(consume);
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Private Implementation                                                  *
     *                                                                         *
     **************************************************************************/     
    
    private void instantiateChildren() {
        this.children = FXCollections.observableArrayList();
        
        // listen to the children in this skin so that we may add it to the 
        // scenegraph (which is the children of Control)
        this.children.addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> change) {
                handleSkinChildrenChanges(change);
            }
        });
    }

    // TODO this should be improved to retain the children z-order
    private void handleSkinChildrenChanges(Change<? extends Node> change) {
        while (change.next()) {
            if (change.wasRemoved()) {
                // remove nodes from Control children
                getSkinnable().getControlChildren().removeAll(change.getRemoved());
            }

            if (change.wasAdded()) {
                // add new nodes to Control children
                getSkinnable().getControlChildren().addAll(change.getAddedSubList());
            }
        }
    }
    
    
    
     /***************************************************************************
     * Specialization of CSS handling code                                     *
     **************************************************************************/

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated public long impl_getPseudoClassState() {
        return 0;
    }

    private static class StyleableProperties {

        private static final List<StyleableProperty> STYLEABLES;

        static {
            STYLEABLES = Collections.unmodifiableList(Control.impl_CSS_STYLEABLES());
        }
    }

     public static List<StyleableProperty> impl_CSS_STYLEABLES() {
         return SkinBase.StyleableProperties.STYLEABLES;
     }

    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }
}
