/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class TextFieldSkinAndroid extends TextFieldSkin {

    /**************************************************************************
     *
     * Private fields
     *
     **************************************************************************/

    private final EventHandler<MouseEvent> mouseEventListener = e -> {
        if (getSkinnable().isEditable() && getSkinnable().isFocused()) {
            showSoftwareKeyboard();
        }
    };

    private final ChangeListener<Boolean> focusChangeListener = (observable, wasFocused, isFocused) -> {
        if (wasFocused && !isFocused) {
            hideSoftwareKeyboard();
        }
    };
    private final WeakChangeListener<Boolean> weakFocusChangeListener = new WeakChangeListener<>(focusChangeListener);

    /**************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    public TextFieldSkinAndroid(final TextField textField) {
        super(textField);

        textField.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseEventListener);
        textField.focusedProperty().addListener(weakFocusChangeListener);
    }

    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        if (getSkinnable() == null) return;
        getSkinnable().removeEventHandler(MouseEvent.MOUSE_CLICKED, mouseEventListener);
        getSkinnable().focusedProperty().removeListener(weakFocusChangeListener);
        super.dispose();
    }

    native void showSoftwareKeyboard();
    native void hideSoftwareKeyboard();

}
