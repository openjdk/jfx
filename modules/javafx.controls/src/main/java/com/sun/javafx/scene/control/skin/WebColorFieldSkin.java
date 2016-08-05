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

import java.util.Locale;

import com.sun.javafx.scene.control.WebColorField;
import javafx.beans.InvalidationListener;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.skin.ColorPickerSkin;
import javafx.scene.paint.Color;

/**
 */
public class WebColorFieldSkin extends InputFieldSkin {
    private InvalidationListener integerFieldValueListener;
    private boolean noChangeInValue = false;

    /**
     * Create a new WebColorFieldSkin.
     * @param control The WebColorField
     */
    public WebColorFieldSkin(final WebColorField control) {
        super(control);

        // Whenever the value changes on the control, we need to update the text
        // in the TextField. The only time this is not the case is when the update
        // to the control happened as a result of an update in the text textField.
        control.valueProperty().addListener(integerFieldValueListener = observable -> {
            updateText();
        });

        // RT-37494: Force the major text direction to LTR, so that '#' is always
        // on the left side of the text. A special style is used in CSS to keep
        // the text right-aligned when in RTL mode.
        getTextField().setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
    }

    @Override public WebColorField getSkinnable() {
        return (WebColorField) control;
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
    @Override public void dispose() {
        ((WebColorField) control).valueProperty().removeListener(integerFieldValueListener);
        super.dispose();
    }

    //  "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
    protected boolean accept(String text) {
        if (text.length() == 0) return true;
        if (text.matches("#[a-fA-F0-9]{0,6}") || text.matches("[a-fA-F0-9]{0,6}")) {
            return true;
        }
        return false;
    }

    protected void updateText() {
        Color color = ((WebColorField) control).getValue();
        if (color == null) color = Color.BLACK;
        getTextField().setText(Utils.formatHexString(color));
    }

    protected void updateValue() {
        if (noChangeInValue) return;
        Color value = ((WebColorField) control).getValue();
        String text = getTextField().getText() == null ? "" : getTextField().getText().trim().toUpperCase(Locale.ROOT);
        if (text.matches("#[A-F0-9]{6}") || text.matches("[A-F0-9]{6}")) {
            try {
                Color newValue = (text.charAt(0) == '#')? Color.web(text) : Color.web("#"+text);
                if (!newValue.equals(value)) {
                    ((WebColorField) control).setValue(newValue);
                } else {
                    // calling setText results in updateValue - so we set this flag to true
                    // so that when this is true updateValue simply returns.
                    noChangeInValue = true;
                    getTextField().setText(Utils.formatHexString(newValue));
                    noChangeInValue = false;
                }
            } catch (java.lang.IllegalArgumentException ex) {
                System.out.println("Failed to parse ["+text+"]");
            }
        }
    }
}
