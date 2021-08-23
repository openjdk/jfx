/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.text.Text;

/**
 * Utility methods to access package-private api in TextInput-related skins.
 */
public class TextInputSkinShim {

//------------ TextField

    /**
     * Returns the promptNode from the textField's skin. The skin must be of type
     * TextFieldSkin.
     */
    public static Text getPromptNode(TextField textField) {
        TextFieldSkin skin = (TextFieldSkin) textField.getSkin();
        return skin.getPromptNode();
    }

    /**
     * Returns the textNode from the textField's skin. The skin must be of type
     * TextFieldSkin.
     */
    public static Text getTextNode(TextField textField) {
        TextFieldSkin skin = (TextFieldSkin) textField.getSkin();
        return skin.getTextNode();
    }

    /**
     * Returns the textTranslateX from the textField's skin. The skin must be of type
     * TextFieldSkin.
     */
    public static double getTextTranslateX(TextField textField) {
        TextFieldSkin skin = (TextFieldSkin) textField.getSkin();
        return skin.getTextTranslateX();
    }

//----------- TextArea

    /**
     * Returns the promptNode from the textField's skin. The skin must be of type
     * TextFieldSkin.
     */
    public static Text getPromptNode(TextArea textArea) {
        TextAreaSkin skin = (TextAreaSkin) textArea.getSkin();
        return skin.getPromptNode();
    }

    public static Text getTextNode(TextArea textArea) {
        TextAreaSkin skin = (TextAreaSkin) textArea.getSkin();
        return skin.getTextNode();
    }

    public static ScrollPane getScrollPane(TextArea textArea) {
        TextAreaSkin skin = (TextAreaSkin) textArea.getSkin();
        return skin.getScrollPane();
    }

    public static void setHandlePressed(TextArea textArea, boolean pressed) {
        TextAreaSkin skin = (TextAreaSkin) textArea.getSkin();
        skin.setHandlePressed(pressed);
    }

//---------- TextInputControl

    /**
     * Returns a boolean indicating whether or not the control's caret is blinking.
     * The control's skin must be of type TextInputControlSkin.
     */
    public static boolean isCaretBlinking(TextInputControl control) {
        TextInputControlSkin<?> skin = (TextInputControlSkin<?>) control.getSkin();
        return skin.isCaretBlinking();
    }

    private TextInputSkinShim() {}
}
