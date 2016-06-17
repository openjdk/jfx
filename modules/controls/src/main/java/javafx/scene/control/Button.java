/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.event.ActionEvent;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;

import javafx.css.PseudoClass;
import javafx.scene.control.skin.ButtonSkin;

/**
 * <p>A simple button control.  The button control can contain
 * text and/or a graphic.  A button control has three different modes</p>
 * <ul>
 *   <li><strong>Normal:</strong> A normal push button. </li>
 *   <li><strong>Default:</strong> The default button is rendered differently to make it apparent to users that it should
 *   be the default choice should they be unclear as to what should be selected. The behavior of the default button differs
 *   depending on the platform in which it is presented:
 *      <ul>
 *          <li><strong>Windows / Linux:</strong>  A default Button receives {@link javafx.scene.input.KeyCode#ENTER ENTER}
 *          key presses when it has focus. When the default button does not have focus, and focus is on another Button
 *          control, the ENTER key press will be received by the other, non-default Button. When focus is elsewhere in
 *          the user interface, and not on any Button, the ENTER key press will be received by the default button, if
 *          one is specified, and if no other node in the scene consumes it first.</li>
 *          <li><strong>Mac OS X:</strong> A default Button is the only Button in the user interface that responds to the
 *          ENTER key press. If focus is on another non-default Button and ENTER is pressed, the event is only received
 *          by the default Button. On macOS, the only way to fire a non-default Button is through the
 *          {@link javafx.scene.input.KeyCode#SPACE SPACE} key.</li>
 *      </ul>
 *   </li>
 *   <li><strong>Cancel:</strong> A Cancel Button is the button that receives a keyboard VK_ESC press, if no other node in
 *      the scene consumes it.</li>
 * </ul>
 *
 * <p>When a button is pressed and released a {@link ActionEvent} is sent.
 * Your application can perform some action based on this event by implementing an
 * {@link javafx.event.EventHandler} to process the {@link ActionEvent}.  Buttons can also respond to
 * mouse events by implementing an {@link javafx.event.EventHandler} to process the {@link javafx.scene.input.MouseEvent}
 * </p>
 *
 * <p>
 * MnemonicParsing is enabled by default for Button.
 * </p>
 *
 * <p>Example:
 * <pre><code>Button button = new Button("Click Me");</code></pre>
 * @since JavaFX 2.0
 */
public class Button extends ButtonBase {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a button with an empty string for its label.
     */
    public Button() {
        initialize();
    }

    /**
     * Creates a button with the specified text as its label.
     *
     * @param text A text string for its label.
     */
    public Button(String text) {
        super(text);
        initialize();
    }

    /**
     * Creates a button with the specified text and icon for its label.
     *
     * @param text A text string for its label.
     * @param graphic the icon for its label.
     */
    public Button(String text, Node graphic) {
        super(text, graphic);
        initialize();
    }

    private void initialize() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setAccessibleRole(AccessibleRole.BUTTON);
        setMnemonicParsing(true);     // enable mnemonic auto-parsing by default
    }

    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * A default Button is the button that receives
     * a keyboard VK_ENTER press, if no other node in the scene consumes it.
     */
    private BooleanProperty defaultButton;
    public final void setDefaultButton(boolean value) {
        defaultButtonProperty().set(value);
    }
    public final boolean isDefaultButton() {
        return defaultButton == null ? false : defaultButton.get();
    }

    public final BooleanProperty defaultButtonProperty() {
        if (defaultButton == null) {
            defaultButton = new BooleanPropertyBase(false) {
                @Override protected void invalidated() {
                    pseudoClassStateChanged(PSEUDO_CLASS_DEFAULT, get());
                }

                @Override
                public Object getBean() {
                    return Button.this;
                }

                @Override
                public String getName() {
                    return "defaultButton";
                }
            };
        }
        return defaultButton;
    }


    /**
     * A Cancel Button is the button that receives
     * a keyboard VK_ESC press, if no other node in the scene consumes it.
     */
    private BooleanProperty cancelButton;
    public final void setCancelButton(boolean value) {
        cancelButtonProperty().set(value);
    }
    public final boolean isCancelButton() {
        return cancelButton == null ? false : cancelButton.get();
    }

    public final BooleanProperty cancelButtonProperty() {
        if (cancelButton == null) {
            cancelButton = new BooleanPropertyBase(false) {
                @Override protected void invalidated() {
                    pseudoClassStateChanged(PSEUDO_CLASS_CANCEL, get());
                }

                @Override
                public Object getBean() {
                    return Button.this;
                }

                @Override
                public String getName() {
                    return "cancelButton";
                }
            };
        }
        return cancelButton;
    }


    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void fire() {
        if (!isDisabled()) {
            fireEvent(new ActionEvent());
        }
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new ButtonSkin(this);
    }


    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    /**
     * Initialize the style class to 'button'.
     *
     * This is the selector class from which CSS can be used to style
     * this control.
     */
    private static final String DEFAULT_STYLE_CLASS = "button";

    private static final PseudoClass PSEUDO_CLASS_DEFAULT
            = PseudoClass.getPseudoClass("default");
    private static final PseudoClass PSEUDO_CLASS_CANCEL
            = PseudoClass.getPseudoClass("cancel");

}
