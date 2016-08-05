/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import com.sun.javafx.scene.control.IntegerField;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.scene.Node;

/**
 */
public class IntegerFieldSkin extends InputFieldSkin {
    private InvalidationListener integerFieldValueListener;

    /**
     * Create a new IntegerFieldSkin.
     * @param control The IntegerField
     */
    public IntegerFieldSkin(final IntegerField control) {
        super(control);

        // Whenever the value changes on the control, we need to update the text
        // in the TextField. The only time this is not the case is when the update
        // to the control happened as a result of an update in the text textField.
        control.valueProperty().addListener(integerFieldValueListener = observable -> {
            updateText();
        });
    }

    @Override public IntegerField getSkinnable() {
        return (IntegerField) control;
    }

    @Override public Node getNode() {
        return getTextField();
    }

    /**
     * Called by a Skinnable when the Skin is replaced on the Skinnable. This method
     * allows a Skin to implement any logic necessary to clean up itself after
     * the Skin is no longer needed. It may be used to release native resources.
     * The methods {@link #getSkinnable()} and {@link #getNode()}
     * should return null following a call to dispose. Calling dispose twice
     * has no effect.
     */
    @Override
    public void dispose() {
        ((IntegerField) control).valueProperty().removeListener(integerFieldValueListener);
        super.dispose();
    }

    @Override protected boolean accept(String text) {
        if (text.length() == 0) return true;
        if (text.matches("[0-9]*")) {
            try {
                Integer.parseInt(text);
                int value = Integer.parseInt(text);
                int maxValue = ((IntegerField) control).getMaxValue();
                return (maxValue != -1) ? (value <= maxValue )  : true;
            } catch (NumberFormatException ex) { }
        }
        return false;
    }

    @Override protected void updateText() {
        getTextField().setText("" + ((IntegerField) control).getValue());
    }

    @Override protected void updateValue() {
        int value = ((IntegerField) control).getValue();
        int newValue;
        String text = getTextField().getText() == null ? "" : getTextField().getText().trim();
        try {
            newValue = Integer.parseInt(text);
            if (newValue != value) {
                ((IntegerField) control).setValue(newValue);
            }
        } catch (NumberFormatException ex) {
            // Empty string most likely
            ((IntegerField) control).setValue(0);
            Platform.runLater(() -> {
                getTextField().positionCaret(1);
            });
        }
    }
}
