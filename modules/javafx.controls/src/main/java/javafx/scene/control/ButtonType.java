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
package javafx.scene.control;

import com.sun.javafx.scene.control.skin.resources.ControlResources;

import javafx.beans.NamedArg;
import javafx.scene.control.ButtonBar.ButtonData;

/**
 * The ButtonType class is used as part of the JavaFX {@link Dialog} API (more
 * specifically, the {@link DialogPane} API) to specify which buttons should be
 * shown to users in the dialogs. Refer to the {@link DialogPane} class javadoc
 * for more information on how to use this class.
 *
 * @see Alert
 * @see Dialog
 * @see DialogPane
 * @since JavaFX 8u40
 */
public final class ButtonType {

    /**
     * A pre-defined {@link ButtonType} that displays "Apply" and has a
     * {@link ButtonData} of {@link ButtonData#APPLY}.
     */
    public static final ButtonType APPLY = new ButtonType(
            "Dialog.apply.button", null, ButtonData.APPLY);

    /**
     * A pre-defined {@link ButtonType} that displays "OK" and has a
     * {@link ButtonData} of {@link ButtonData#OK_DONE}.
     */
    public static final ButtonType OK = new ButtonType(
            "Dialog.ok.button", null, ButtonData.OK_DONE);

    /**
     * A pre-defined {@link ButtonType} that displays "Cancel" and has a
     * {@link ButtonData} of {@link ButtonData#CANCEL_CLOSE}.
     */
    public static final ButtonType CANCEL = new ButtonType(
            "Dialog.cancel.button", null, ButtonData.CANCEL_CLOSE);

    /**
     * A pre-defined {@link ButtonType} that displays "Close" and has a
     * {@link ButtonData} of {@link ButtonData#CANCEL_CLOSE}.
     */
    public static final ButtonType CLOSE = new ButtonType(
            "Dialog.close.button", null, ButtonData.CANCEL_CLOSE);

    /**
     * A pre-defined {@link ButtonType} that displays "Yes" and has a
     * {@link ButtonData} of {@link ButtonData#YES}.
     */
    public static final ButtonType YES = new ButtonType(
            "Dialog.yes.button", null, ButtonData.YES);

    /**
     * A pre-defined {@link ButtonType} that displays "No" and has a
     * {@link ButtonData} of {@link ButtonData#NO}.
     */
    public static final ButtonType NO = new ButtonType(
            "Dialog.no.button", null, ButtonData.NO);

    /**
     * A pre-defined {@link ButtonType} that displays "Finish" and has a
     * {@link ButtonData} of {@link ButtonData#FINISH}.
     */
    public static final ButtonType FINISH = new ButtonType(
            "Dialog.finish.button", null, ButtonData.FINISH);

    /**
     * A pre-defined {@link ButtonType} that displays "Next" and has a
     * {@link ButtonData} of {@link ButtonData#NEXT_FORWARD}.
     */
    public static final ButtonType NEXT = new ButtonType(
            "Dialog.next.button", null, ButtonData.NEXT_FORWARD);

    /**
     * A pre-defined {@link ButtonType} that displays "Previous" and has a
     * {@link ButtonData} of {@link ButtonData#BACK_PREVIOUS}.
     */
    public static final ButtonType PREVIOUS = new ButtonType(
            "Dialog.previous.button", null, ButtonData.BACK_PREVIOUS);

    private final String key;
    private final String text;
    private final ButtonData buttonData;


    /**
     * Creates a ButtonType instance with the given text, and the ButtonData set
     * as {@link ButtonData#OTHER}.
     *
     * @param text The string to display in the text property of controls such
     *      as {@link Button#textProperty() Button}.
     */
    public ButtonType(@NamedArg("text") String text) {
        this(text, ButtonData.OTHER);
    }

    /**
     * Creates a ButtonType instance with the given text, and the ButtonData set
     * as specified.
     *
     * @param text The string to display in the text property of controls such
     *      as {@link Button#textProperty() Button}.
     * @param buttonData The type of button that should be created from this ButtonType.
     */
    public ButtonType(@NamedArg("text") String text,
                        @NamedArg("buttonData") ButtonData buttonData) {
        this(null, text, buttonData);
    }

    /**
     * Provide key or text. The other one should be null.
     */
    private ButtonType(String key, String text, ButtonData buttonData) {
        this.key = key;
        this.text = text;
        this.buttonData = buttonData;
    }

    /**
     * Returns the ButtonData specified for this ButtonType in the constructor.
     * @return the ButtonData specified for this ButtonType in the constructor
     */
    public final ButtonData getButtonData() { return this.buttonData; }

    /**
     * Returns the text specified for this ButtonType in the constructor.
     * @return the text specified for this ButtonType in the constructor
     */
    public final String getText() {
        if (text == null && key != null) {
            return ControlResources.getString(key);
        } else {
            return text;
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "ButtonType [text=" + getText() + ", buttonData=" + getButtonData() + "]";
    }
}
