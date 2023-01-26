/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.ToggleButtonBehavior;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.scene.control.Control;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.StackPane;

/**
 * Default skin implementation for the {@link RadioButton} control.
 *
 * @see RadioButton
 * @since 9
 */
public class RadioButtonSkin extends LabeledSkinBase<RadioButton> {

    /* *************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    /** The radio contains the "dot", which is usually a circle */
    private StackPane radio;
    private final BehaviorBase<RadioButton> behavior;



    /* *************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new RadioButtonSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public RadioButtonSkin(RadioButton control) {
        super(control);

        // install default input map for the RadioButton control
        behavior = new ToggleButtonBehavior<>(control);
//        control.setInputMap(behavior.getInputMap());

        radio = createRadio();
        updateChildren();
    }



    /* *************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override protected void updateChildren() {
        super.updateChildren();
        if (radio != null) {
            getChildren().add(radio);
        }
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computeMinWidth(height, topInset, rightInset, bottomInset, leftInset) + snapSizeX(radio.minWidth(-1));
    }

    /** {@inheritDoc} */
    @Override protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Math.max(snapSizeY(super.computeMinHeight(width - radio.minWidth(-1), topInset, rightInset, bottomInset, leftInset)),
                topInset + radio.minHeight(-1) + bottomInset);
    }

    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset) + snapSizeX(radio.prefWidth(-1));
    }

    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return Math.max(snapSizeY(super.computePrefHeight(width - radio.prefWidth(-1), topInset, rightInset, bottomInset, leftInset)),
                topInset + radio.prefHeight(-1) + bottomInset);
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren(final double x, final double y,
                                            final double w, final double h) {
        final RadioButton radioButton = getSkinnable();
        final double radioWidth = radio.prefWidth(-1);
        final double radioHeight = radio.prefHeight(-1);
        final double computeWidth = Math.max(radioButton.prefWidth(-1),radioButton.minWidth(-1));
        final double labelWidth = Math.min(computeWidth - radioWidth, w - snapSizeX(radioWidth));
        final double labelHeight = Math.min(radioButton.prefHeight(labelWidth), h);
        final double maxHeight = Math.max(radioHeight, labelHeight);
        final double xOffset = Utils.computeXOffset(w, labelWidth + radioWidth, radioButton.getAlignment().getHpos()) + x;
        final double yOffset = Utils.computeYOffset(h, maxHeight, radioButton.getAlignment().getVpos()) + y;

        layoutLabelInArea(xOffset + radioWidth, yOffset, labelWidth, maxHeight,  radioButton.getAlignment());
        radio.resize(snapSizeX(radioWidth), snapSizeY(radioHeight));
        positionInArea(radio, xOffset, yOffset, radioWidth, maxHeight, 0, radioButton.getAlignment().getHpos(), radioButton.getAlignment().getVpos());
    }



    /* *************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    private static StackPane createRadio() {
        StackPane radio = new StackPane();
        radio.getStyleClass().setAll("radio");
        radio.setSnapToPixel(false);
        StackPane region = new StackPane();
        region.getStyleClass().setAll("dot");
        radio.getChildren().clear();
        radio.getChildren().addAll(region);
        return radio;
    }
}
