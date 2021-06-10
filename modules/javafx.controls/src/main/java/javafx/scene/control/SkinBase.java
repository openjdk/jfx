/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.sun.javafx.scene.control.LambdaMultiplePropertyChangeListenerHandler;

import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
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

    /**
     * This is part of the workaround introduced during delomboking. We probably will
     * want to adjust the way listeners are added rather than continuing to use this
     * map (although it doesn't really do much harm).
     */
    private LambdaMultiplePropertyChangeListenerHandler lambdaChangeListenerHandler;



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

        // unhook listeners
        if (lambdaChangeListenerHandler != null) {
            lambdaChangeListenerHandler.dispose();
        }

        this.control = null;
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Returns the children of the skin.
     * @return the children of the skin
     */
    public final ObservableList<Node> getChildren() {
        return children;
    }

    /**
     * Called during the layout pass of the scenegraph.
     * @param contentX the x position
     * @param contentY the y position
     * @param contentWidth the width
     * @param contentHeight the height
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
     * @param value the consume mouse events flag
     */
    protected final void consumeMouseEvents(boolean value) {
        if (value) {
            control.addEventHandler(MouseEvent.ANY, mouseEventConsumer);
        } else {
            control.removeEventHandler(MouseEvent.ANY, mouseEventConsumer);
        }
    }


    /**
     * Registers an operation to perform when the given {@code observable} sends a change event.
     * Does nothing if either {@code observable} or {@code operation} are {@code null}.
     * If multiple operations are registered on the same observable, they will be performed in the
     * order in which they were registered.
     *
     * @param observable the observable to observe for change events, may be {@code null}
     * @param operation the operation to perform when the observable sends a change event,
     *  may be {@code null}
     * @since 9
     */
    protected final void registerChangeListener(ObservableValue<?> observable, Consumer<ObservableValue<?>> operation) {
        if (lambdaChangeListenerHandler == null) {
            lambdaChangeListenerHandler = new LambdaMultiplePropertyChangeListenerHandler();
        }
        lambdaChangeListenerHandler.registerChangeListener(observable, operation);
    }

    /**
     * Unregisters all operations that have been registered using
     * {@link #registerChangeListener(ObservableValue, Consumer)}
     * for the given {@code observable}. Does nothing if {@code observable} is {@code null}.
     *
     * @param observable the observable for which the registered operations should be removed,
     *  may be {@code null}
     * @return a composed consumer representing all previously registered operations, or
     *  {@code null} if none have been registered or the observable is {@code null}
     * @since 9
     */
    protected final Consumer<ObservableValue<?>> unregisterChangeListeners(ObservableValue<?> observable) {
        if (lambdaChangeListenerHandler == null) {
            return null;
        }
        return lambdaChangeListenerHandler.unregisterChangeListeners(observable);
    }

    /**
     * Registers an operation to perform when the given {@code observable} sends an invalidation event.
     * Does nothing if either {@code observable} or {@code operation} are {@code null}.
     * If multiple operations are registered on the same observable, they will be performed in the
     * order in which they were registered.
     *
     * @param observable the observable to observe for invalidation events, may be {@code null}
     * @param operation the operation to perform when the observable sends an invalidation event,
     *  may be {@code null}
     * @since 17
     */
    protected final void registerInvalidationListener(Observable observable, Consumer<Observable> operation) {
        if (lambdaChangeListenerHandler == null) {
            lambdaChangeListenerHandler = new LambdaMultiplePropertyChangeListenerHandler();
        }
        lambdaChangeListenerHandler.registerInvalidationListener(observable, operation);
    }

    /**
     * Unregisters all operations that have been registered using
     * {@link #registerInvalidationListener(Observable, Consumer)}
     * for the given {@code observable}. Does nothing if {@code observable} is {@code null}.
     *
     * @param observable the observable for which the registered operations should be removed,
     *  may be {@code null}
     * @return a composed consumer representing all previously registered operations, or
     *  {@code null} if none have been registered or the observable is {@code null}
     * @since 17
     */
    protected final Consumer<Observable> unregisterInvalidationListeners(Observable observable) {
        if (lambdaChangeListenerHandler == null) {
            return null;
        }
        return lambdaChangeListenerHandler.unregisterInvalidationListeners(observable);
    }


    /**
     * Registers an operation to perform when the given {@code observableList} sends a list change event.
     * Does nothing if either {@code observableList} or {@code operation} are {@code null}.
     * If multiple operations are registered on the same observable list, they will be performed in the
     * order in which they were registered.
     *
     * @param observableList the observableList to observe for list change events, may be {@code null}
     * @param operation the operation to perform when the observableList sends a list change event,
     *  may be {@code null}
     * @since 17
     */
    protected final void registerListChangeListener(ObservableList<?> observableList, Consumer<Change<?>> operation) {
        if (lambdaChangeListenerHandler == null) {
            lambdaChangeListenerHandler = new LambdaMultiplePropertyChangeListenerHandler();
        }
        lambdaChangeListenerHandler.registerListChangeListener(observableList, operation);
    }

    /**
     * Unregisters all operations that have been registered using
     * {@link #registerListChangeListener(ObservableList, Consumer)}
     * for the given {@code observableList}. Does nothing if {@code observableList} is {@code null}.
     *
     * @param observableList the observableList for which the registered operations should be removed,
     *  may be {@code null}
     * @return a composed consumer representing all previously registered operations, or
     *  {@code null} if none have been registered or the observableList is {@code null}
     * @since 17
     */
    protected final Consumer<Change<?>> unregisterListChangeListeners(ObservableList<?> observableList) {
        if (lambdaChangeListenerHandler == null) {
            return null;
        }
        return lambdaChangeListenerHandler.unregisterListChangeListeners(observableList);
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
     * If {@code getSkinnable().isSnapToPixel()} is false, this method
     * returns the same value, else it tries to return a value rounded to
     * the nearest pixel, but since there is no indication if the value is
     * a vertical or horizontal measurement then it may be snapped to the
     * wrong pixel size metric on screens with different horizontal and
     * vertical scales.
     * @param value the space value to be snapped
     * @return value rounded to nearest pixel
     * @deprecated replaced by {@code snapSpaceX()} and {@code snapSpaceY()}
     */
    @Deprecated(since="9")
    protected double snapSpace(double value) {
        return control.snapSpaceX(value);
    }

    /**
     * Convenience method for accessing the
     * {@link Region#snapSpaceX(double) snapSpaceX()}
     * method on the skinnable.
     * It is equivalent to calling
     * {@code getSkinnable().snapSpaceX(value)}.
     * @param value the space value to be snapped
     * @return value rounded to nearest pixel
     * @see Region#snapSpaceX(double)
     * @since 9
     */
    protected double snapSpaceX(double value) {
        return control.snapSpaceX(value);
    }

    /**
     * Convenience method for accessing the
     * {@link Region#snapSpaceY(double) snapSpaceY()}
     * method on the skinnable.
     * It is equivalent to calling
     * {@code getSkinnable().snapSpaceY(value)}.
     * @param value the space value to be snapped
     * @return value rounded to nearest pixel
     * @see Region#snapSpaceY(double)
     * @since 9
     */
    protected double snapSpaceY(double value) {
        return control.snapSpaceY(value);
    }

    /**
     * If {@code getSkinnable().isSnapToPixel()} is false, this method
     * returns the same value, else it tries to return a value ceiled to
     * the nearest pixel, but since there is no indication if the value is
     * a vertical or horizontal measurement then it may be snapped to the
     * wrong pixel size metric on screens with different horizontal and
     * vertical scales.
     * @param value the size value to be snapped
     * @return value ceiled to nearest pixel
     * @deprecated replaced by {@code snapSizeX()} and {@code snapSizeY()}
     */
    @Deprecated(since="9")
    protected double snapSize(double value) {
        return control.snapSizeX(value);
    }

    /**
     * Convenience method for accessing the
     * {@link Region#snapSizeX(double) snapSizeX()}
     * method on the skinnable.
     * It is equivalent to calling
     * {@code getSkinnable().snapSizeX(value)}.
     * @param value the size value to be snapped
     * @return value ceiled to nearest pixel
     * @see Region#snapSizeX(double)
     * @since 9
     */
    protected double snapSizeX(double value) {
        return control.snapSizeX(value);
    }

    /**
     * Convenience method for accessing the
     * {@link Region#snapSizeY(double) snapSizeY()}
     * method on the skinnable.
     * It is equivalent to calling
     * {@code getSkinnable().snapSizeY(value)}.
     * @param value the size value to be snapped
     * @return value ceiled to nearest pixel
     * @see Region#snapSizeY(double)
     * @since 9
     */
    protected double snapSizeY(double value) {
        return control.snapSizeY(value);
    }

    /**
     * If {@code getSkinnable().isSnapToPixel()} is false, this method
     * returns the same value, else it tries to return a value rounded to
     * the nearest pixel, but since there is no indication if the value is
     * a vertical or horizontal measurement then it may be snapped to the
     * wrong pixel size metric on screens with different horizontal and
     * vertical scales.
     * @param value the position value to be snapped
     * @return value rounded to nearest pixel
     * @deprecated replaced by {@code snapPositionX()} and {@code snapPositionY()}
     */
    @Deprecated(since="9")
    protected double snapPosition(double value) {
        return control.snapPositionX(value);
    }

    /**
     * Convenience method for accessing the
     * {@link Region#snapPositionX(double) snapPositionX()}
     * method on the skinnable.
     * It is equivalent to calling
     * {@code getSkinnable().snapPositionX(value)}.
     * @param value the position value to be snapped
     * @return value rounded to nearest pixel
     * @see Region#snapPositionX(double)
     * @since 9
     */
    protected double snapPositionX(double value) {
        return control.snapPositionX(value);
    }

    /**
     * Convenience method for accessing the
     * {@link Region#snapPositionY(double) snapPositionY()}
     * method on the skinnable.
     * It is equivalent to calling
     * {@code getSkinnable().snapPositionY(value)}.
     * @param value the position value to be snapped
     * @return value rounded to nearest pixel
     * @see Region#snapPositionY(double)
     * @since 9
     */
    protected double snapPositionY(double value) {
        return control.snapPositionY(value);
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
     * Returns the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @return the CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return SkinBase.StyleableProperties.STYLEABLES;
    }

    /**
     * This method should delegate to {@link Node#getClassCssMetaData()} so that
     * a Node's CssMetaData can be accessed without the need for reflection.
     * @return The CssMetaData associated with this node, which may include the
     * CssMetaData of its superclasses.
     */
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /**
     * Used to specify that a pseudo-class of this Node has changed. If the
     * pseudo-class is used in a CSS selector that matches this Node, CSS will
     * be reapplied. Typically, this method is called from the {@code invalidated}
     * method of a property that is used as a pseudo-class. For example:
     * <pre><code>
     *
     *     private static final PseudoClass MY_PSEUDO_CLASS_STATE = PseudoClass.getPseudoClass("my-state");
     *
     *     BooleanProperty myPseudoClassState = new BooleanPropertyBase(false) {
     *
     *           {@literal @}Override public void invalidated() {
     *                pseudoClassStateChanged(MY_PSEUDO_CLASS_STATE, get());
     *           }
     *
     *           {@literal @}Override public Object getBean() {
     *               return MyControl.this;
     *           }
     *
     *           {@literal @}Override public String getName() {
     *               return "myPseudoClassState";
     *           }
     *       };
     * </code></pre>
     *
     * @see Node#pseudoClassStateChanged
     * @param pseudoClass the pseudo-class that has changed state
     * @param active whether or not the state is active
     * @since JavaFX 8.0
     */
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

    /**
     * This method is called by the assistive technology to request
     * the value for an attribute.
     * <p>
     * This method is commonly overridden by subclasses to implement
     * attributes that are required for a specific role.<br>
     * If a particular attribute is not handled, the superclass implementation
     * must be called.
     * </p>
     *
     * @param attribute the requested attribute
     * @param parameters optional list of parameters
     * @return the value for the requested attribute
     *
     * @see AccessibleAttribute
     * @see Node#queryAccessibleAttribute
     *
     * @since JavaFX 8u40
     */
    protected Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        return null;
    }

    /**
     * This method is called by the assistive technology to request the action
     * indicated by the argument should be executed.
     * <p>
     * This method is commonly overridden by subclasses to implement
     * action that are required for a specific role.<br>
     * If a particular action is not handled, the superclass implementation
     * must be called.
     * </p>
     *
     * @param action the action to execute
     * @param parameters optional list of parameters
     *
     * @see AccessibleAction
     * @see Node#executeAccessibleAction
     *
     * @since JavaFX 8u40
     */
    protected void executeAccessibleAction(AccessibleAction action, Object... parameters) {
    }

    /***************************************************************************
     *                                                                         *
     * Testing-only API                                                        *
     *                                                                         *
     **************************************************************************/

}
