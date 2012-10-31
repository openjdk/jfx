/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Node;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.scene.control.accessible.AccessibleButton;
import com.sun.javafx.accessible.providers.AccessibleProvider;

/**
 * <p>A simple button control.  The button control can contain
 * text and/or a graphic.  A button control has three different modes</p>
 * <ul>
 * <li> Normal: A normal push button. </li>
 * <li> Default: A default Button is the button that receives a keyboard VK_ENTER press, if no other node in the scene consumes it.</li>
 * <li> Cancel: A Cancel Button is the button that receives a keyboard VK_ESC press, if no other node in the scene consumes it.</li>
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
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_DEFAULT);
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
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_CANCEL);
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
        fireEvent(new ActionEvent());
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
    private static final String PSEUDO_CLASS_DEFAULT = "default";
    private static final String PSEUDO_CLASS_CANCEL = "cancel";

    private static final long PSEUDO_CLASS_DEFAULT_MASK
            = StyleManager.getPseudoclassMask(PSEUDO_CLASS_DEFAULT);
    private static final long PSEUDO_CLASS_CANCEL_MASK
            = StyleManager.getPseudoclassMask(PSEUDO_CLASS_CANCEL);

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        if (isDefaultButton()) mask |= PSEUDO_CLASS_DEFAULT_MASK;
        if (isCancelButton()) mask |= PSEUDO_CLASS_CANCEL_MASK;
        return mask;
    }
    
    private AccessibleButton accButton ;
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public AccessibleProvider impl_getAccessible() {
        if( accButton == null)
            accButton = new AccessibleButton(this);
        return (AccessibleProvider)accButton ;
    }
    
}
