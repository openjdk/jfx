/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import com.sun.javafx.scene.ParentHelper;
import com.sun.javafx.scene.control.FakeFocusTextField;
import com.sun.javafx.scene.control.ListenerHelper;
import com.sun.javafx.scene.control.behavior.SpinnerBehavior;
import com.sun.javafx.scene.traversal.Algorithm;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.scene.traversal.ParentTraversalEngine;
import com.sun.javafx.scene.traversal.TraversalContext;

/**
 * Default skin implementation for the {@link Spinner} control.
 *
 * @see Spinner
 * @since 9
 */
public class SpinnerSkin<T> extends SkinBase<Spinner<T>> {

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private TextField textField;

    private Region incrementArrow;
    private StackPane incrementArrowButton;

    private Region decrementArrow;
    private StackPane decrementArrowButton;

    // rather than create an private enum, lets just use an int, here's the important details:
    private static final int ARROWS_ON_RIGHT_VERTICAL   = 0;
    private static final int ARROWS_ON_LEFT_VERTICAL    = 1;
    private static final int ARROWS_ON_RIGHT_HORIZONTAL = 2;
    private static final int ARROWS_ON_LEFT_HORIZONTAL  = 3;
    private static final int SPLIT_ARROWS_VERTICAL      = 4;
    private static final int SPLIT_ARROWS_HORIZONTAL    = 5;

    private int layoutMode = 0;
    /* Package-private for testing purposes */
    final SpinnerBehavior behavior;


    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new SpinnerSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public SpinnerSkin(Spinner<T> control) {
        super(control);

        // install default input map for the Button control
        behavior = new SpinnerBehavior<>(control);

        textField = control.getEditor();

        ListenerHelper lh = ListenerHelper.get(this);

        updateStyleClass();
        lh.addListChangeListener(control.getStyleClass(), (ch) -> {
            updateStyleClass();
        });

        // increment / decrement arrows
        incrementArrow = new Region();
        incrementArrow.setFocusTraversable(false);
        incrementArrow.getStyleClass().setAll("increment-arrow");
        incrementArrow.setMaxWidth(Region.USE_PREF_SIZE);
        incrementArrow.setMaxHeight(Region.USE_PREF_SIZE);
        incrementArrow.setMouseTransparent(true);

        incrementArrowButton = new StackPane() {
            @Override
            public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
                switch (action) {
                    case FIRE: getSkinnable().increment(); break;
                    default: super.executeAccessibleAction(action, parameters);
                }
            }
        };
        incrementArrowButton.setAccessibleRole(AccessibleRole.INCREMENT_BUTTON);
        incrementArrowButton.setFocusTraversable(false);
        incrementArrowButton.getStyleClass().setAll("increment-arrow-button");
        incrementArrowButton.getChildren().add(incrementArrow);
        incrementArrowButton.setOnMousePressed(e -> {
            getSkinnable().requestFocus();
            behavior.startSpinning(true);
        });
        incrementArrowButton.setOnMouseReleased(e -> behavior.stopSpinning());

        decrementArrow = new Region();
        decrementArrow.setFocusTraversable(false);
        decrementArrow.getStyleClass().setAll("decrement-arrow");
        decrementArrow.setMaxWidth(Region.USE_PREF_SIZE);
        decrementArrow.setMaxHeight(Region.USE_PREF_SIZE);
        decrementArrow.setMouseTransparent(true);

        decrementArrowButton = new StackPane() {
            @Override
            public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
                switch (action) {
                    case FIRE: getSkinnable().decrement(); break;
                    default: super.executeAccessibleAction(action, parameters);
                }
            }
        };
        decrementArrowButton.setAccessibleRole(AccessibleRole.DECREMENT_BUTTON);
        decrementArrowButton.setFocusTraversable(false);
        decrementArrowButton.getStyleClass().setAll("decrement-arrow-button");
        decrementArrowButton.getChildren().add(decrementArrow);
        decrementArrowButton.setOnMousePressed(e -> {
            getSkinnable().requestFocus();
            behavior.startSpinning(false);
        });
        decrementArrowButton.setOnMouseReleased(e -> behavior.stopSpinning());

        getChildren().addAll(incrementArrowButton, decrementArrowButton);

        // Fixes in the same vein as ComboBoxListViewSkin

        // move fake focus in to the textfield if the spinner is editable
        lh.addChangeListener(control.focusedProperty(), (op) -> {
            // Fix for the regression noted in a comment in RT-29885.
            ((FakeFocusTextField)textField).setFakeFocus(control.isFocused());
        });

        lh.addEventFilter(control, KeyEvent.ANY, (ke) -> {
            if (control.isEditable()) {
                // This prevents a stack overflow from our rebroadcasting of the
                // event to the textfield that occurs in the final else statement
                // of the conditions below.
                if (ke.getTarget().equals(textField)) return;

                // Fix for RT-38527 which led to a stack overflow
                if (ke.getCode() == KeyCode.ESCAPE) return;

                // This and the additional check of isIncDecKeyEvent in
                // textField's event filter fix JDK-8185937.
                if (isIncDecKeyEvent(ke)) return;

                // Fix for the regression noted in a comment in RT-29885.
                // This forwards the event down into the TextField when
                // the key event is actually received by the Spinner.
                textField.fireEvent(ke.copyFor(textField, textField));

                if (ke.getCode() == KeyCode.ENTER) return;

                ke.consume();
            }
        });

        // This event filter is to enable keyboard events being delivered to the
        // spinner when the user has mouse clicked into the TextField area of the
        // Spinner control. Without this the up/down/left/right arrow keys don't
        // work when you click inside the TextField area (but they do in the case
        // of tabbing in).
        lh.addEventFilter(textField, KeyEvent.ANY, (ke) -> {
            if (! control.isEditable() || isIncDecKeyEvent(ke)) {
                control.fireEvent(ke.copyFor(control, control));
                ke.consume();
            }
        });

        lh.addChangeListener(textField.focusedProperty(), (op) -> {
            boolean hasFocus = textField.isFocused();
            // Fix for RT-29885
            control.getProperties().put("FOCUSED", hasFocus);
            // --- end of RT-29885

            // RT-21454 starts here
            if (! hasFocus) {
                pseudoClassStateChanged(CONTAINS_FOCUS_PSEUDOCLASS_STATE, false);
            } else {
                pseudoClassStateChanged(CONTAINS_FOCUS_PSEUDOCLASS_STATE, true);
            }
            // --- end of RT-21454
        });

        // end of comboBox-esque fixes

        textField.focusTraversableProperty().bind(control.editableProperty());

        // Following code borrowed from ComboBoxPopupControl, to resolve the
        // issue initially identified in RT-36902, but specifically (for Spinner)
        // identified in RT-40625
        ParentHelper.setTraversalEngine(control,
                new ParentTraversalEngine(control, new Algorithm() {

            @Override public Node select(Node owner, Direction dir, TraversalContext context) {
                return null;
            }

            @Override public Node selectFirst(TraversalContext context) {
                return null;
            }

            @Override public Node selectLast(TraversalContext context) {
                return null;
            }
        }));

        lh.addChangeListener(control.sceneProperty(), (op) -> {
            // Stop spinning when sceneProperty is modified
            behavior.stopSpinning();
        });
    }

    private boolean isIncDecKeyEvent(KeyEvent ke) {
        final KeyCode kc = ke.getCode();
        return (kc == KeyCode.UP || kc == KeyCode.DOWN) && behavior.arrowsAreVertical();
    }

    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    @Override
    public void install() {
        // when replacing the skin, the textField (which comes from the control), must first be uninstalled
        // by the old skin in its dispose(), followed by (re-)adding it here.
        getChildren().add(textField);
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        if (getSkinnable() == null) {
            return;
        }

        getChildren().removeAll(textField, incrementArrowButton, decrementArrowButton);

        if (behavior != null) {
            behavior.dispose();
        }

        super.dispose();
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {

        final double incrementArrowButtonWidth = incrementArrowButton.snappedLeftInset() +
                snapSizeX(incrementArrow.prefWidth(-1)) + incrementArrowButton.snappedRightInset();

        final double decrementArrowButtonWidth = decrementArrowButton.snappedLeftInset() +
                snapSizeX(decrementArrow.prefWidth(-1)) + decrementArrowButton.snappedRightInset();

        final double widestArrowButton = Math.max(incrementArrowButtonWidth, decrementArrowButtonWidth);

        // we need to decide on our layout approach, and this depends on
        // the presence of style classes in the Spinner styleClass list.
        // To be a bit more efficient, we observe the list for changes, so
        // here in layoutChildren we can just react to a few booleans.
        if (layoutMode == ARROWS_ON_RIGHT_VERTICAL || layoutMode == ARROWS_ON_LEFT_VERTICAL) {
            final double textFieldStartX = layoutMode == ARROWS_ON_RIGHT_VERTICAL ? x : x + widestArrowButton;
            final double buttonStartX = layoutMode == ARROWS_ON_RIGHT_VERTICAL ? x + w - widestArrowButton : x;
            final double halfHeight = Math.floor(h / 2.0);

            textField.resizeRelocate(textFieldStartX, y, w - widestArrowButton, h);

            incrementArrowButton.resize(widestArrowButton, halfHeight);
            positionInArea(incrementArrowButton, buttonStartX, y,
                    widestArrowButton, halfHeight, 0, HPos.CENTER, VPos.CENTER);

            decrementArrowButton.resize(widestArrowButton, halfHeight);
            positionInArea(decrementArrowButton, buttonStartX, y + halfHeight,
                    widestArrowButton, h - halfHeight, 0, HPos.CENTER, VPos.BOTTOM);
        } else if (layoutMode == ARROWS_ON_RIGHT_HORIZONTAL || layoutMode == ARROWS_ON_LEFT_HORIZONTAL) {
            final double totalButtonWidth = incrementArrowButtonWidth + decrementArrowButtonWidth;
            final double textFieldStartX = layoutMode == ARROWS_ON_RIGHT_HORIZONTAL ? x : x + totalButtonWidth;
            final double buttonStartX = layoutMode == ARROWS_ON_RIGHT_HORIZONTAL ? x + w - totalButtonWidth : x;

            textField.resizeRelocate(textFieldStartX, y, w - totalButtonWidth, h);

            // decrement is always on the left
            decrementArrowButton.resize(decrementArrowButtonWidth, h);
            positionInArea(decrementArrowButton, buttonStartX, y,
                    decrementArrowButtonWidth, h, 0, HPos.CENTER, VPos.CENTER);

            // ... and increment is always on the right
            incrementArrowButton.resize(incrementArrowButtonWidth, h);
            positionInArea(incrementArrowButton, buttonStartX + decrementArrowButtonWidth, y,
                    incrementArrowButtonWidth, h, 0, HPos.CENTER, VPos.CENTER);
        } else if (layoutMode == SPLIT_ARROWS_VERTICAL) {
            final double incrementArrowButtonHeight = incrementArrowButton.snappedTopInset() +
                    snapSizeY(incrementArrow.prefHeight(-1)) + incrementArrowButton.snappedBottomInset();

            final double decrementArrowButtonHeight = decrementArrowButton.snappedTopInset() +
                    snapSizeY(decrementArrow.prefHeight(-1)) + decrementArrowButton.snappedBottomInset();

            final double tallestArrowButton = Math.max(incrementArrowButtonHeight, decrementArrowButtonHeight);

            // increment is at the top
            incrementArrowButton.resize(w, tallestArrowButton);
            positionInArea(incrementArrowButton, x, y,
                    w, tallestArrowButton, 0, HPos.CENTER, VPos.CENTER);

            // textfield in the middle
            textField.resizeRelocate(x, y + tallestArrowButton, w, h - (2*tallestArrowButton));

            // decrement is at the bottom
            decrementArrowButton.resize(w, tallestArrowButton);
            positionInArea(decrementArrowButton, x, y + h - tallestArrowButton,
                    w, tallestArrowButton, 0, HPos.CENTER, VPos.CENTER);
        } else if (layoutMode == SPLIT_ARROWS_HORIZONTAL) {
            // decrement is on the left-hand side
            decrementArrowButton.resize(widestArrowButton, h);
            positionInArea(decrementArrowButton, x, y,
                    widestArrowButton, h, 0, HPos.CENTER, VPos.CENTER);

            // textfield in the middle
            textField.resizeRelocate(x + widestArrowButton, y, w - (2*widestArrowButton), h);

            // increment is on the right-hand side
            incrementArrowButton.resize(widestArrowButton, h);
            positionInArea(incrementArrowButton, x + w - widestArrowButton, y,
                    widestArrowButton, h, 0, HPos.CENTER, VPos.CENTER);
        }
    }

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return textField.minWidth(height);
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double textfieldWidth = textField.prefWidth(height);
        return leftInset + textfieldWidth + rightInset;
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        double ph;
        double textFieldHeight = textField.prefHeight(width);

        if (layoutMode == SPLIT_ARROWS_VERTICAL) {
            ph = topInset + incrementArrowButton.prefHeight(width) +
                    textFieldHeight + decrementArrowButton.prefHeight(width) + bottomInset;
        } else {
            ph = topInset + textFieldHeight + bottomInset;
        }

        return ph;
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefWidth(height);
    }

    /** {@inheritDoc} */
    @Override protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return getSkinnable().prefHeight(width);
    }

    // Overridden so that we use the textfield as the baseline, rather than the arrow.
    // See RT-30754 for more information.
    /** {@inheritDoc} */
    @Override protected double computeBaselineOffset(double topInset, double rightInset, double bottomInset, double leftInset) {
        return textField.getLayoutBounds().getMinY() + textField.getLayoutY() + textField.getBaselineOffset();
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private void updateStyleClass() {
        final List<String> styleClass = getSkinnable().getStyleClass();

        if (styleClass.contains(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL)) {
            layoutMode = ARROWS_ON_LEFT_VERTICAL;
        } else if (styleClass.contains(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL)) {
            layoutMode = ARROWS_ON_LEFT_HORIZONTAL;
        } else if (styleClass.contains(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL)) {
            layoutMode = ARROWS_ON_RIGHT_HORIZONTAL;
        } else if (styleClass.contains(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL)) {
            layoutMode = SPLIT_ARROWS_VERTICAL;
        } else if (styleClass.contains(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL)) {
            layoutMode = SPLIT_ARROWS_HORIZONTAL;
        } else {
            layoutMode = ARROWS_ON_RIGHT_VERTICAL;
        }
    }



    /* *************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static PseudoClass CONTAINS_FOCUS_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("contains-focus");
}
