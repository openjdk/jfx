/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import javafx.event.EventHandler;
import javafx.scene.control.SkinBase;
import com.sun.javafx.scene.control.behavior.ComboBoxBaseBehavior;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * An abstract class intended to be used as the base skin for ComboBox-like
 * controls that are based on {@link ComboBoxBase}. Most users of this skin class
 * would be well-advised to also look at {@link ComboBoxPopupControl} for
 * additional useful API.
 *
 * @since 9
 * @param <T> The type of the ComboBox-like control.
 * @see ComboBoxBase
 * @see ComboBoxPopupControl
 */
public abstract class ComboBoxBaseSkin<T> extends SkinBase<ComboBoxBase<T>> {

    /* *************************************************************************
     *                                                                         *
     * Private Fields                                                          *
     *                                                                         *
     **************************************************************************/

    private Node displayNode; // this is normally either label or textField

    StackPane arrowButton;
    Region arrow;

    /** The mode in which this control will be represented. */
    private ComboBoxMode mode = ComboBoxMode.COMBOBOX;
    final ComboBoxMode getMode() { return mode; }
    final void setMode(ComboBoxMode value) { mode = value; }

    private final EventHandler<MouseEvent> mouseEnteredEventHandler  = e ->   getBehavior().mouseEntered(e);
    private final EventHandler<MouseEvent> mousePressedEventHandler  = e -> { getBehavior().mousePressed(e);  e.consume(); };
    private final EventHandler<MouseEvent> mouseReleasedEventHandler = e -> { getBehavior().mouseReleased(e); e.consume(); };
    private final EventHandler<MouseEvent> mouseExitedEventHandler   = e ->   getBehavior().mouseExited(e);



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new instance of ComboBoxBaseSkin, although note that this
     * instance does not handle any behavior / input mappings - this needs to be
     * handled appropriately by subclasses.
     *
     * @param control The control that this skin should be installed onto.
     */
    public ComboBoxBaseSkin(final ComboBoxBase<T> control) {
        // Call the super method with the ComboBox we were just given in the constructor
        super(control);

        getChildren().clear();

        // open button / arrow
        arrow = new Region();
        arrow.setFocusTraversable(false);
        arrow.getStyleClass().setAll("arrow");
        arrow.setId("arrow");
        arrow.setMaxWidth(Region.USE_PREF_SIZE);
        arrow.setMaxHeight(Region.USE_PREF_SIZE);
        arrow.setMouseTransparent(true);

        arrowButton = new StackPane();
        arrowButton.setFocusTraversable(false);
        arrowButton.setId("arrow-button");
        arrowButton.getStyleClass().setAll("arrow-button");
        arrowButton.getChildren().add(arrow);

        getChildren().add(arrowButton);

        // When ComboBoxBase focus shifts to another node, it should hide.
        getSkinnable().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                focusLost();
            }
        });

        // Register listeners
        updateArrowButtonListeners();
        registerChangeListener(control.editableProperty(), e -> {
            updateArrowButtonListeners();
            updateDisplayArea();
        });
        registerChangeListener(control.showingProperty(), e -> {
            if (getSkinnable().isShowing()) {
                show();
            } else {
                hide();
            }
        });
        registerChangeListener(control.valueProperty(), e -> updateDisplayArea());
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * This method should return a Node that will be positioned within the
     * ComboBox 'button' area.
     * @return the node that will be positioned within the ComboBox 'button' area
     */
    public abstract Node getDisplayNode();

    /**
     * This method will be called when the ComboBox popup should be displayed.
     * It is up to specific skin implementations to determine how this is handled.
     */
    public abstract void show();

    /**
     * This method will be called when the ComboBox popup should be hidden.
     * It is up to specific skin implementations to determine how this is handled.
     */
    public abstract void hide();

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
            final double w, final double h) {
        if (displayNode == null) {
            updateDisplayArea();
        }

        final double arrowWidth = snapSizeX(arrow.prefWidth(-1));
        final double arrowButtonWidth = (isButton()) ? 0 :
                arrowButton.snappedLeftInset() + arrowWidth +
                arrowButton.snappedRightInset();

        if (displayNode != null) {
            displayNode.resizeRelocate(x, y, w - arrowButtonWidth, h);
        }

        arrowButton.setVisible(! isButton());
        if (! isButton()) {
            arrowButton.resize(arrowButtonWidth, h);
            positionInArea(arrowButton, (x + w) - arrowButtonWidth, y,
                    arrowButtonWidth, h, 0, HPos.CENTER, VPos.CENTER);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (displayNode == null) {
            updateDisplayArea();
        }

        final double arrowWidth = snapSizeX(arrow.prefWidth(-1));
        final double arrowButtonWidth = isButton() ? 0 :
                                        arrowButton.snappedLeftInset() +
                                        arrowWidth +
                                        arrowButton.snappedRightInset();
        final double displayNodeWidth = displayNode == null ? 0 : displayNode.prefWidth(height);

        final double totalWidth = displayNodeWidth + arrowButtonWidth;
        return leftInset + totalWidth + rightInset;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        if (displayNode == null) {
            updateDisplayArea();
        }

        double ph;
        if (displayNode == null) {
            final int DEFAULT_HEIGHT = 21;
            double arrowHeight = (isButton()) ? 0 :
                    (arrowButton.snappedTopInset() + arrow.prefHeight(-1) + arrowButton.snappedBottomInset());
            ph = Math.max(DEFAULT_HEIGHT, arrowHeight);
        } else {
            ph = displayNode.prefHeight(width);
        }

        return topInset+ ph + bottomInset;
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefWidth(height);
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefHeight(width);
    }

    // Overridden so that we use the displayNode as the baseline, rather than the arrow.
    // See RT-30754 for more information.
    /** {@inheritDoc} */
    @Override protected double computeBaselineOffset(double topInset, double rightInset, double bottomInset, double leftInset) {
        if (displayNode == null) {
            updateDisplayArea();
        }

        if (displayNode != null) {
            return displayNode.getLayoutBounds().getMinY() + displayNode.getLayoutY() + displayNode.getBaselineOffset();
        }

        return super.computeBaselineOffset(topInset, rightInset, bottomInset, leftInset);
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    ComboBoxBaseBehavior getBehavior() {
        return null;
    }

    void focusLost() {
        getSkinnable().hide();
    }

    private boolean isButton() {
        return getMode() == ComboBoxMode.BUTTON;
    }

    private void updateArrowButtonListeners() {
        if (getSkinnable().isEditable()) {
            //
            // arrowButton behaves like a button.
            // This is strongly tied to the implementation in ComboBoxBaseBehavior.
            //
            arrowButton.addEventHandler(MouseEvent.MOUSE_ENTERED,  mouseEnteredEventHandler);
            arrowButton.addEventHandler(MouseEvent.MOUSE_PRESSED,  mousePressedEventHandler);
            arrowButton.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);
            arrowButton.addEventHandler(MouseEvent.MOUSE_EXITED,   mouseExitedEventHandler);
        } else {
            arrowButton.removeEventHandler(MouseEvent.MOUSE_ENTERED,  mouseEnteredEventHandler);
            arrowButton.removeEventHandler(MouseEvent.MOUSE_PRESSED,  mousePressedEventHandler);
            arrowButton.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);
            arrowButton.removeEventHandler(MouseEvent.MOUSE_EXITED,   mouseExitedEventHandler);
        }
    }

    void updateDisplayArea() {
        final List<Node> children = getChildren();
        final Node oldDisplayNode = displayNode;
        displayNode = getDisplayNode();

        // don't remove displayNode if it hasn't changed.
        if (oldDisplayNode != null && oldDisplayNode != displayNode) {
            children.remove(oldDisplayNode);
        }

        if (displayNode != null && !children.contains(displayNode)) {
            children.add(displayNode);
            displayNode.applyCss();
        }
    }
}
