/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import java.util.concurrent.atomic.AtomicBoolean;
import javafx.geometry.Point2D;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.skin.TextFieldSkin;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TextInputControlSkinTest {
    @Test public void caretStopsAnimatingWhenTextFieldLosesFocus() {
        final AtomicBoolean caretAnimating = new AtomicBoolean(false);
        FocusableTextField textField = new FocusableTextField();
        TextFieldSkin skin = new TextFieldSkin(textField) {
            @Override public void setCaretAnimating(boolean value) {
                caretAnimating.set(value);
                super.setCaretAnimating(value);
            }
        };
        textField.setSkin(skin);

        textField.setFocus(true);
        assertTrue(caretAnimating.get());
        textField.setFocus(false);
        assertFalse(caretAnimating.get());
    }

    @Test public void caretStopsAnimatingWhenTextAreaLosesFocus() {
        final AtomicBoolean caretAnimating = new AtomicBoolean(false);
        FocusableTextArea textArea = new FocusableTextArea();
        TextAreaSkin skin = new TextAreaSkin(textArea) {
            @Override public void setCaretAnimating(boolean value) {
                caretAnimating.set(value);
                super.setCaretAnimating(value);
            }
        };
        textArea.setSkin(skin);

        textArea.setFocus(true);
        assertTrue(caretAnimating.get());
        textArea.setFocus(false);
        assertFalse(caretAnimating.get());
    }

    @Test public void skinsCanHandleNullValues_RT34178() {
        // RT-34178: NPE in TextFieldSkin of PasswordField

        // The skins should always use textProperty().getValueSafe()
        // instead of getText().

        TextArea textArea = new TextArea();
        textArea.setSkin(new TextAreaSkin(textArea));
        textArea.setText(null);

        TextField textField = new TextField();
        textField.setSkin(new TextFieldSkin(textField));
        textField.setText(null);

        PasswordField passwordField = new PasswordField();
        passwordField.setSkin(new TextFieldSkin(passwordField));
        passwordField.setText(null);
    }

    @Test public void noNullPointerIfTextInputNotInScene() {
        TextField textField = new TextField();
        TextFieldSkin skin = new TextFieldSkin(textField);
        textField.setSkin(skin);

        // Check that no NullPointerException is thrown if the TextField is not in scene
        // and that the default point is returned.
        Point2D point = textField.getInputMethodRequests().getTextLocation(0);
        assertEquals(new Point2D(0, 0), point);
    }

    public class FocusableTextField extends TextField {
        public void setFocus(boolean value) {
            super.setFocused(value);
        }
    }

    public class FocusableTextArea extends TextArea {
        public void setFocus(boolean value) {
            super.setFocused(value);
        }
    }
}
